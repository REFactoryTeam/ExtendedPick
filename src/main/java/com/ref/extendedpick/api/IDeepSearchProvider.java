package com.ref.extendedpick.api;

import com.ref.extendedpick.common.DeepSearchProviderRegistry;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface IDeepSearchProvider<T> {

  void forEachItem(@NotNull ItemStack container, @NotNull Consumer<IndexedStack<T>> action);

  @NotNull
  ItemStack extract(
      @NotNull ServerPlayer player, @NotNull ItemStack container, @NotNull T internalIndex);

  @SuppressWarnings("unchecked")
  @NotNull
  default ItemStack invokeExtract(
      @NotNull ServerPlayer player, @NotNull ItemStack container, @NotNull Object internalIndex) {
    return this.extract(player, container, (T) internalIndex);
  }

  record IndexedStack<T>(@NotNull ItemStack stack, @NotNull T index) {}

  void register();

  default void register(@NotNull Item item) {
    DeepSearchProviderRegistry.getInstance().register(item, this);
  }
}
