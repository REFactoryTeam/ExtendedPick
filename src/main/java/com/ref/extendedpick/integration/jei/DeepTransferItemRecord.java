package com.ref.extendedpick.integration.jei;

import net.minecraft.world.item.ItemStack;

public class DeepTransferItemRecord {
  private final ItemStack stack;
  private int remaining;

  public DeepTransferItemRecord(ItemStack stack) {
    this.stack = stack;
    this.remaining = stack.getCount();
  }

  public ItemStack getStack() {
    return stack;
  }

  public int getRemaining() {
    return remaining;
  }

  public void consume() {
    if (this.remaining > 0) {
      this.remaining--;
    }
  }
}
