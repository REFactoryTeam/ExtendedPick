package com.ref.extendedpick.mixin.jei;

import com.ref.extendedpick.ExtendedPick;
import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.IPlayerInventoryAccess;
import com.ref.extendedpick.config.ExtendedPickClientConfig;
import com.ref.extendedpick.integration.jei.DeepRecipeTransferPacket;
import com.ref.extendedpick.integration.jei.DeepTransferData;
import com.ref.extendedpick.integration.jei.DeepTransferItemRecord;
import com.ref.extendedpick.network.PacketHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.common.network.IConnectionToServer;
import mezz.jei.common.network.packets.PacketJei;
import mezz.jei.common.network.packets.PacketRecipeTransfer;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.library.transfer.BasicRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(value = BasicRecipeTransferHandler.class, remap = false)
public class BasicRecipeTransferHandlerMixin {

  @Unique
  private static final ThreadLocal<List<DeepTransferData>> DEEP_TRANSFERS =
      ThreadLocal.withInitial(ArrayList::new);

  @Inject(method = "transferRecipe", at = @At("HEAD"))
  private void onTransferRecipeStart(CallbackInfoReturnable<?> cir) {
    DEEP_TRANSFERS.remove();
  }

  @Inject(method = "transferRecipe", at = @At("RETURN"))
  private void onTransferRecipeEnd(CallbackInfoReturnable<?> cir) {
    DEEP_TRANSFERS.remove();
  }

  @Redirect(
      method = "transferRecipe",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lmezz/jei/common/transfer/RecipeTransferUtil;getRecipeTransferOperations(Lmezz/jei/api/helpers/IStackHelper;Ljava/util/Map;Ljava/util/List;Ljava/util/List;)Lmezz/jei/common/transfer/RecipeTransferOperationsResult;"))
  private RecipeTransferOperationsResult onGetRecipeTransferOperations(
      IStackHelper stackHelper,
      Map<Slot, ItemStack> availableItemStacks,
      List<IRecipeSlotView> requiredItemStacks,
      List<Slot> craftingSlots) {

    RecipeTransferOperationsResult result =
        RecipeTransferUtil.getRecipeTransferOperations(
            stackHelper, availableItemStacks, requiredItemStacks, craftingSlots);

    if (!(ExtendedPickClientConfig.exPick && ExtendedPickClientConfig.exPickPacket)) {
      return result;
    }

    if (!ExtendedPick.isServerModLoaded || result.missingItems.isEmpty()) {
      return result;
    }

    Player player = Minecraft.getInstance().player;
    if (player == null) {
      return result;
    }

    List<IRecipeSlotView> stillMissing = new ArrayList<>();
    List<DeepTransferData> deepTransfers = new ArrayList<>();

    List<DeepTransferItemRecord> availableDeepItems = extendedPick$getAvailableDeepItems(player);

    for (IRecipeSlotView missing : result.missingItems) {
      boolean claimed = false;

      int reqIndex = -1;
      for (int i = 0; i < requiredItemStacks.size(); i++) {
        if (requiredItemStacks.get(i) == missing) {
          reqIndex = i;
          break;
        }
      }

      if (reqIndex != -1) {
        int craftingSlotIndex = craftingSlots.get(reqIndex).index;

        missingStacksLoop:
        for (ItemStack validStack : missing.getItemStacks().toList()) {
          if (validStack == null || validStack.isEmpty()) continue;

          for (DeepTransferItemRecord record : availableDeepItems) {
            if (record.getRemaining() > 0
                && stackHelper.isEquivalent(validStack, record.getStack(), UidContext.Ingredient)) {

              record.consume();
              claimed = true;

              ItemStack transferItem = record.getStack().copy();
              transferItem.setCount(1);
              deepTransfers.add(new DeepTransferData(craftingSlotIndex, List.of(transferItem)));

              break missingStacksLoop;
            }
          }
        }
      }

      if (!claimed) {
        stillMissing.add(missing);
      }
    }

    if (!deepTransfers.isEmpty()) {
      DEEP_TRANSFERS.set(deepTransfers);
      result.missingItems.clear();
      result.missingItems.addAll(stillMissing);
    }

    return result;
  }

  @Redirect(
      method = "transferRecipe",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lmezz/jei/common/network/IConnectionToServer;sendPacketToServer(Lmezz/jei/common/network/packets/PacketJei;)V"))
  private void onSendPacketToServer(IConnectionToServer connection, PacketJei packet) {
    if (!ExtendedPick.isServerModLoaded || !(packet instanceof PacketRecipeTransfer prt)) {
      connection.sendPacketToServer(packet);
      return;
    }

    List<DeepTransferData> deepData = DEEP_TRANSFERS.get();

    if (!deepData.isEmpty()) {
      PacketRecipeTransferAccessor accessor = (PacketRecipeTransferAccessor) prt;

      DeepRecipeTransferPacket customPacket =
          new DeepRecipeTransferPacket(
              prt.transferOperations,
              prt.craftingSlots.stream().map(s -> s.index).toList(),
              prt.inventorySlots.stream().map(s -> s.index).toList(),
              accessor.isMaxTransfer(),
              accessor.isRequireCompleteSets(),
              new ArrayList<>(deepData));

      PacketHandler.sendToServer(customPacket);
    } else {
      connection.sendPacketToServer(packet);
    }

    DEEP_TRANSFERS.remove();
  }

  @Unique
  private List<DeepTransferItemRecord> extendedPick$getAvailableDeepItems(Player player) {
    List<DeepTransferItemRecord> availableDeepItems = new ArrayList<>();
    IPlayerInventoryAccess.findStacks(
        player,
        (container, context) -> {
          if (container.isEmpty()) return;
          IDeepSearchProvider<?> provider = IDeepSearchProvider.getProvider(container);
          if (provider == null) return;

          provider.forEachItem(
              container,
              indexedStack -> {
                ItemStack stackInSlot = indexedStack.stack();
                if (!stackInSlot.isEmpty()) {
                  availableDeepItems.add(new DeepTransferItemRecord(stackInSlot));
                }
                return true;
              });
        });
    return availableDeepItems;
  }
}
