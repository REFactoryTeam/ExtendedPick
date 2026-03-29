package com.ref.extendedpick.integration.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record DeepTransferData(int craftingSlotIndex, List<ItemStack> validItems) {

  public void write(FriendlyByteBuf buf) {
    buf.writeVarInt(craftingSlotIndex);
    buf.writeVarInt(validItems.size());
    for (ItemStack stack : validItems) {
      buf.writeItem(stack);
    }
  }

  public static DeepTransferData read(FriendlyByteBuf buf) {
    int craftingSlotIndex = buf.readVarInt();
    int size = buf.readVarInt();
    List<ItemStack> validItems = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      validItems.add(buf.readItem());
    }
    return new DeepTransferData(craftingSlotIndex, validItems);
  }
}
