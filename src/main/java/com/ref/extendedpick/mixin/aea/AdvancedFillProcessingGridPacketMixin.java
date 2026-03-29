package com.ref.extendedpick.mixin.aea;

import appeng.api.stacks.AEItemKey;
import com.ref.aea.integration.aea.advancedterminal.AdvancedFillProcessingGridPacket;
import com.ref.aea.integration.aea.advancedterminal.AdvancedTerminalMenu;
import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.IPlayerInventoryAccess;
import com.ref.extendedpick.config.ExtendedPickCommonConfig;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AdvancedFillProcessingGridPacket.class, remap = false)
public class AdvancedFillProcessingGridPacketMixin {

  @Inject(method = "takeItemsFromPlayer", at = @At("RETURN"), cancellable = true)
  private static void onTakeItemsFromPlayer(
      ServerPlayer player,
      AdvancedTerminalMenu menu,
      AEItemKey itemKey,
      long needed,
      CallbackInfoReturnable<Long> cir) {
    if (!ExtendedPickCommonConfig.deepSearch) return;

    long alreadyTaken = cir.getReturnValue();
    long remainingNeeded = needed - alreadyTaken;

    if (remainingNeeded <= 0) return;

    AtomicLong deepExtracted = new AtomicLong(0);

    IPlayerInventoryAccess.findStacks(
        player,
        (container, context) -> {
          if (deepExtracted.get() >= remainingNeeded || container.isEmpty()) return;
          if (!context.isCurios() && menu.isPlayerInventorySlotLocked(context.index())) return;

          IDeepSearchProvider<?> provider = IDeepSearchProvider.getProvider(container);
          if (provider != null) {
            provider.forEachItem(
                container,
                indexed -> {
                  long currentNeeded = remainingNeeded - deepExtracted.get();
                  if (currentNeeded > 0 && itemKey.matches(indexed.stack())) {
                    int amountToExtract = (int) Math.min(currentNeeded, indexed.stack().getCount());
                    var extracted =
                        provider.invokeExtract(
                            player, container, indexed.index(), amountToExtract, false);
                    if (!extracted.isEmpty()) {
                      deepExtracted.addAndGet(extracted.getCount());
                    }
                  }
                  return deepExtracted.get() < remainingNeeded;
                });
          }
        });

    if (deepExtracted.get() > 0) {
      cir.setReturnValue(alreadyTaken + deepExtracted.get());
    }
  }
}
