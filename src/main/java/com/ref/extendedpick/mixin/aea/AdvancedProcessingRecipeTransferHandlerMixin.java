package com.ref.extendedpick.mixin.aea;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.ref.aea.integration.aea.advancedterminal.AdvancedProcessingRecipeTransferHandler;
import com.ref.extendedpick.ExtendedPick;
import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.IPlayerInventoryAccess;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(value = AdvancedProcessingRecipeTransferHandler.class, remap = false)
public class AdvancedProcessingRecipeTransferHandlerMixin {

  @Inject(method = "countPlayerInventory", at = @At("TAIL"))
  private void onCountPlayerInventory(
      Player player,
      Map<AEKey, Long> availableCounts,
      Map<Item, Set<AEItemKey>> availableItemsMap,
      CallbackInfo ci) {
    if (!ExtendedPick.isServerModLoaded) return;

    IPlayerInventoryAccess.findStacks(
        player,
        (container, context) -> {
          if (container.isEmpty()) return;

          IDeepSearchProvider<?> provider = IDeepSearchProvider.getProvider(container);
          if (provider != null) {
            provider.forEachItem(
                container,
                indexedStack -> {
                  ItemStack innerStack = indexedStack.stack();
                  if (!innerStack.isEmpty()) {
                    AEItemKey key = AEItemKey.of(innerStack);
                    if (key != null) {
                      availableCounts.merge(key, (long) innerStack.getCount(), Long::sum);
                      availableItemsMap
                          .computeIfAbsent(key.getItem(), k -> new HashSet<>())
                          .add(key);
                    }
                  }
                  return true;
                });
          }
        });
  }
}
