package com.ref.extendedpick.api;

import com.ref.extendedpick.common.DeepSearchProviderRegistry;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IDeepSearchProvider {

  void forEachItem(@NotNull ItemStack container, @NotNull Consumer<IndexedStack> action);

  @NotNull
  ItemStack extract(@NotNull ServerPlayer player, @NotNull ItemStack container, int internalIndex);

  record IndexedStack(ItemStack stack, int index) {}

  void register();

  default void register(@NotNull Item item) {
    DeepSearchProviderRegistry.getInstance().register(item, this);
  }
}
