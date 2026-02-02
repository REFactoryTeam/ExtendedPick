package com.ref.extendedpick.server;

import com.ref.extendedpick.ExtendedPick;
import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.ISearchHelper;
import com.ref.extendedpick.common.DeepSearchProviderRegistry;
import com.ref.extendedpick.common.SearchHelperRegistry;
import de.mari_023.ae2wtlib.AE2wtlibEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosCapability;

public class ServerPickHandler {

  private record SearchCandidate(
      int slotIndex, boolean isCurios, ItemStack snapshot, IDeepSearchProvider provider) {}

  private record SearchResult(
      int score,
      int containerSlot,
      boolean isCurios,
      int internalIndex,
      Item containerItemType,
      IDeepSearchProvider provider) {}

  /**
   * Handles the deep search request from a client. Initiates an asynchronous search through
   * inventory containers.
   */
  public static void handleDeepSearchRequest(ServerPlayer player, ItemStack targetStack) {
    if (!player.getMainHandItem().isEmpty()) return;

    Inventory inventory = player.getInventory();
    List<SearchCandidate> candidates = new ArrayList<>();

    for (int i = 0; i < inventory.getContainerSize(); i++) {
      collectCandidate(candidates, inventory.getItem(i), i, false);
    }

    player
        .getCapability(CuriosCapability.INVENTORY)
        .ifPresent(
            handler -> {
              IItemHandlerModifiable curiosItems = handler.getEquippedCurios();
              for (int i = 0; i < curiosItems.getSlots(); i++) {
                collectCandidate(candidates, curiosItems.getStackInSlot(i), i, true);
              }
            });

    if (candidates.isEmpty()) {
      handleFallback(player, targetStack);
      return;
    }

    ISearchHelper searchHelper =
        SearchHelperRegistry.getInstance().getHelper(targetStack.getItem());

    CompletableFuture.supplyAsync(() -> performSearch(candidates, targetStack, searchHelper))
        .thenAcceptAsync(result -> applyResult(player, result, targetStack), player.getServer());
  }

  private static void collectCandidate(
      List<SearchCandidate> candidates, ItemStack stack, int index, boolean isCurios) {
    if (stack.isEmpty()) return;

    IDeepSearchProvider provider =
        DeepSearchProviderRegistry.getInstance().getProvider(stack.getItem());

    if (provider == null && stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
      provider = DeepSearchProviderRegistry.getInstance().getDefaultHelper();
    }

    if (provider != null) {
      candidates.add(new SearchCandidate(index, isCurios, stack.copy(), provider));
    }
  }

  private static SearchResult performSearch(
      List<SearchCandidate> candidates, ItemStack target, ISearchHelper helper) {
    int globalBestScore = ISearchHelper.Failed;
    SearchResult bestResult = null;

    for (SearchCandidate candidate : candidates) {
      final int[] localBest = {ISearchHelper.Failed, -1};

      candidate.provider.forEachItem(
          candidate.snapshot,
          (indexedStack) -> {
            int score = helper.getMatchScore(target, indexedStack.stack());
            if (score > localBest[0]) {
              localBest[0] = score;
              localBest[1] = indexedStack.index();
            }
          });

      if (localBest[0] > globalBestScore) {
        globalBestScore = localBest[0];
        bestResult =
            new SearchResult(
                globalBestScore,
                candidate.slotIndex,
                candidate.isCurios,
                localBest[1],
                candidate.snapshot.getItem(),
                candidate.provider);
      }

      if (globalBestScore == ISearchHelper.Success) break;
    }
    return bestResult;
  }

  private static void applyResult(ServerPlayer player, SearchResult result, ItemStack targetStack) {
    if (player.hasDisconnected()) return;

    if (result == null) {
      handleFallback(player, targetStack);
      return;
    }

    Inventory inventory = player.getInventory();
    int handSlot = inventory.selected;

    if (!inventory.getItem(handSlot).isEmpty()) return;

    ItemStack sourceContainer = findSourceContainer(player, result);
    if (sourceContainer.isEmpty()) return;

    ItemStack extracted = result.provider.extract(player, sourceContainer, result.internalIndex);
    if (!extracted.isEmpty()) {
      inventory.setItem(handSlot, extracted);
    }
  }

  private static ItemStack findSourceContainer(ServerPlayer player, SearchResult result) {
    ItemStack candidate = ItemStack.EMPTY;

    if (!result.isCurios) {
      candidate = player.getInventory().getItem(result.containerSlot);
    } else {
      var capability = player.getCapability(CuriosCapability.INVENTORY).resolve();
      if (capability.isPresent()) {
        IItemHandlerModifiable curios = capability.get().getEquippedCurios();
        if (result.containerSlot >= 0 && result.containerSlot < curios.getSlots()) {
          candidate = curios.getStackInSlot(result.containerSlot);
        }
      }
    }

    return (candidate.getItem() == result.containerItemType) ? candidate : ItemStack.EMPTY;
  }

  private static void handleFallback(ServerPlayer player, ItemStack targetStack) {
    if (ExtendedPick.AE_WIRELESS_TERMINAL_LOADED) {
      AE2wtlibEvents.pickBlock(player, targetStack);
    }
  }
}
