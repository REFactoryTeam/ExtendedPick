package com.ref.extendedpick.mixin.jei;

import mezz.jei.common.network.packets.PacketRecipeTransfer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@OnlyIn(Dist.CLIENT)
@Mixin(value = PacketRecipeTransfer.class, remap = false)
public interface PacketRecipeTransferAccessor {

  @Accessor("maxTransfer")
  boolean isMaxTransfer();

  @Accessor("requireCompleteSets")
  boolean isRequireCompleteSets();
}
