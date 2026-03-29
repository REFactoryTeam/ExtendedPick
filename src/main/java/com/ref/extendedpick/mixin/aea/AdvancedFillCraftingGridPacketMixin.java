package com.ref.extendedpick.mixin.aea;

import appeng.helpers.IMenuCraftingPacket;
import com.ref.aea.integration.aea.advancedterminal.AdvancedFillCraftingGridPacket;
import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.IPlayerInventoryAccess;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AdvancedFillCraftingGridPacket.class, remap = false)
public class AdvancedFillCraftingGridPacketMixin {

  @Inject(method = "takeIngredientFromPlayer", at = @At("RETURN"), cancellable = true)
  private static void onTakeIngredientFromPlayer(
      IMenuCraftingPacket cct,
      ServerPlayer player,
      Ingredient ingredient,
      CallbackInfoReturnable<ItemStack> cir) {
    if (!cir.getReturnValue().isEmpty()) return;

    AtomicReference<ItemStack> extractedResult = new AtomicReference<>(ItemStack.EMPTY);

    IPlayerInventoryAccess.findStacks(
        player,
        (container, context) -> {
          if (!extractedResult.get().isEmpty() || container.isEmpty()) return;
          if (!context.isCurios() && cct.isPlayerInventorySlotLocked(context.index())) return;

          IDeepSearchProvider<?> provider = IDeepSearchProvider.getProvider(container);
          if (provider != null) {
            provider.forEachItem(
                container,
                indexed -> {
                  if (extractedResult.get().isEmpty() && ingredient.test(indexed.stack())) {
                    ItemStack extracted =
                        provider.invokeExtract(player, container, indexed.index(), 1, false);
                    if (!extracted.isEmpty()) {
                      extractedResult.set(extracted);
                      return false;
                    }
                  }
                  return true;
                });
          }
        });

    if (!extractedResult.get().isEmpty()) {
      cir.setReturnValue(extractedResult.get());
    }
  }
}
