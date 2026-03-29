package com.ref.extendedpick.server;

import com.ref.extendedpick.ExtendedPick;
import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.IPlayerInventoryAccess;
import com.ref.extendedpick.api.ISearchHelper;
import com.ref.extendedpick.common.SearchHelperRegistry;
import de.mari_023.ae2wtlib.AE2wtlibEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ServerPickHandler {

  private record SearchCandidate(
      int slotIndex, boolean isCurios, ItemStack snapshot, IDeepSearchProvider<?> provider) {}

  private record SearchResult(
      int score,
      int containerSlot,
      boolean isCurios,
      Object internalIndex,
      Item containerItemType,
      IDeepSearchProvider<?> provider) {}

  private static class MatchContext {
    int bestScore = ISearchHelper.Failed;
    Object bestIndex = null;
  }

  /**
   * Handles the deep search request from a client. Initiates an asynchronous search through
   * inventory containers.
   */
  public static void handleDeepSearchRequest(ServerPlayer player, ItemStack targetStack) {
    if (!player.getMainHandItem().isEmpty()) {
      return;
    }

    List<SearchCandidate> candidates = new ArrayList<>();
    IPlayerInventoryAccess.findStacks(
        player,
        (stack, context) -> {
          IDeepSearchProvider<?> provider = IDeepSearchProvider.getProvider(stack);
          if (provider != null) {
            candidates.add(
                new SearchCandidate(context.index(), context.isCurios(), stack.copy(), provider));
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

  private static SearchResult performSearch(
      List<SearchCandidate> candidates, ItemStack target, ISearchHelper helper) {
    int globalBestScore = ISearchHelper.Failed;
    SearchResult bestResult = null;

    for (SearchCandidate candidate : candidates) {
      MatchContext context = new MatchContext();

      candidate.provider.forEachItem(
          candidate.snapshot,
          indexedStack -> {
            int score = helper.getMatchScore(target, indexedStack.stack());
            if (score > context.bestScore) {
              context.bestScore = score;
              context.bestIndex = indexedStack.index();
            }
            return context.bestScore != ISearchHelper.Success;
          });

      if (context.bestScore > globalBestScore) {
        globalBestScore = context.bestScore;
        bestResult =
            new SearchResult(
                globalBestScore,
                candidate.slotIndex,
                candidate.isCurios,
                context.bestIndex,
                candidate.snapshot.getItem(),
                candidate.provider);
      }

      if (globalBestScore == ISearchHelper.Success) {
        break;
      }
    }
    return bestResult;
  }

  private static void applyResult(ServerPlayer player, SearchResult result, ItemStack targetStack) {
    if (player.hasDisconnected()) {
      return;
    }

    if (result == null) {
      handleFallback(player, targetStack);
      return;
    }

    Inventory inventory = player.getInventory();
    if (!inventory.getItem(inventory.selected).isEmpty()) {
      return;
    }

    ItemStack sourceContainer =
        IPlayerInventoryAccess.getStackFromContext(
            player, result.containerSlot, result.isCurios, result.containerItemType);

    if (sourceContainer.isEmpty()) {
      return;
    }

    ItemStack extracted =
        result.provider.invokeExtract(player, sourceContainer, result.internalIndex, false);
    if (!extracted.isEmpty()) {
      inventory.setItem(inventory.selected, extracted);
    }
  }

  private static void handleFallback(ServerPlayer player, ItemStack targetStack) {
    if (ExtendedPick.AE_WIRELESS_TERMINAL_LOADED) {
      AE2wtlibEvents.pickBlock(player, targetStack);
    }
  }
}
