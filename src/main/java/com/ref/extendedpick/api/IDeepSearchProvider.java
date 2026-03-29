package com.ref.extendedpick.api;

import com.ref.extendedpick.common.DeepSearchProviderRegistry;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;

/**
 * Interface representing a provider capable of performing deep searches and extractions within
 * container items.
 *
 * @param <T> The type of the internal index used to locate items.
 */
public interface IDeepSearchProvider<T> {

  /** Constant denoting a request to extract the maximum possible stack size. */
  int MAX_STACK_SIZE = 0;

  void forEachItem(@NotNull ItemStack container, @NotNull Predicate<IndexedStack<T>> action);

  @NotNull
  ItemStack extract(
      @NotNull ServerPlayer player,
      @NotNull ItemStack container,
      @NotNull T internalIndex,
      int amount,
      boolean simulate);

  @SuppressWarnings("unchecked")
  @NotNull
  default ItemStack invokeExtract(
      @NotNull ServerPlayer player,
      @NotNull ItemStack container,
      @NotNull Object internalIndex,
      int amount,
      boolean simulate) {
    return this.extract(player, container, (T) internalIndex, amount, simulate);
  }

  @SuppressWarnings("unchecked")
  @NotNull
  default ItemStack invokeExtract(
      @NotNull ServerPlayer player,
      @NotNull ItemStack container,
      @NotNull Object internalIndex,
      boolean simulate) {
    return this.extract(player, container, (T) internalIndex, MAX_STACK_SIZE, simulate);
  }

  record IndexedStack<T>(@NotNull ItemStack stack, @NotNull T index) {}

  void register();

  default void register(@NotNull Item item) {
    DeepSearchProviderRegistry.getInstance().register(item, this);
  }

  static IDeepSearchProvider<?> getProvider(ItemStack stack) {
    IDeepSearchProvider<?> provider =
        DeepSearchProviderRegistry.getInstance().getProvider(stack.getItem());
    if (provider == null && stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
      return DeepSearchProviderRegistry.getInstance().getDefaultHelper();
    }
    return provider;
  }
}
