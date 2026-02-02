package com.ref.extendedpick.common;

import com.ref.extendedpick.api.IDeepSearchProvider;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeepSearchProviderRegistry {
  private static final DeepSearchProviderRegistry INSTANCE = new DeepSearchProviderRegistry();

  private final IdentityHashMap<Item, IDeepSearchProvider<?>> registry = new IdentityHashMap<>();

  private IDeepSearchProvider<?> defaultDeepSearchProvider;

  private DeepSearchProviderRegistry() {}

  public static DeepSearchProviderRegistry getInstance() {
    return INSTANCE;
  }

  public void register(@NotNull Item item, @NotNull IDeepSearchProvider<?> provider) {
    if (registry.containsKey(item)) {
      throw new IllegalStateException("Item already registered!");
    }
    registry.put(item, provider);
  }

  public void unregister(@NotNull Item item) {
    registry.remove(item);
  }

  @Nullable
  public IDeepSearchProvider<?> getProvider(@NotNull Item item) {
    return registry.get(item);
  }

  public boolean isRegistered(@NotNull Item item) {
    return registry.containsKey(item);
  }

  public Map<Item, IDeepSearchProvider<?>> getRegistryView() {
    return Collections.unmodifiableMap(registry);
  }

  public void clear() {
    registry.clear();
  }

  public void setDefaultHelper(@NotNull IDeepSearchProvider<?> newDefaultHelper) {
    defaultDeepSearchProvider = newDefaultHelper;
  }

  public IDeepSearchProvider<?> getDefaultHelper() {
    return defaultDeepSearchProvider;
  }

  public enum DefaultDeepSearchProvider implements IDeepSearchProvider<Integer> {
    INSTANCE;

    @Override
    public void forEachItem(
        @NotNull ItemStack container, @NotNull Consumer<IndexedStack<Integer>> action) {
      if (container.isEmpty()) return;
      container
          .getCapability(ForgeCapabilities.ITEM_HANDLER)
          .ifPresent(
              handler -> {
                for (int i = 0; i < handler.getSlots(); i++) {
                  ItemStack stackInSlot = handler.getStackInSlot(i);
                  action.accept(new IndexedStack<>(stackInSlot, i));
                }
              });
    }

    @Override
    public @NotNull ItemStack extract(
        @NotNull ServerPlayer player,
        @NotNull ItemStack container,
        @NotNull Integer internalIndex) {
      if (container.isEmpty()) return ItemStack.EMPTY;
      return container
          .getCapability(ForgeCapabilities.ITEM_HANDLER)
          .map(
              handler -> {
                if (internalIndex < 0 || internalIndex >= handler.getSlots()) {
                  return ItemStack.EMPTY;
                }

                ItemStack stackInSlot = handler.getStackInSlot(internalIndex);
                if (stackInSlot.isEmpty()) {
                  return ItemStack.EMPTY;
                }

                return handler.extractItem(internalIndex, stackInSlot.getMaxStackSize(), false);
              })
          .orElse(ItemStack.EMPTY);
    }

    @Override
    public void register() {
      DeepSearchProviderRegistry.getInstance().setDefaultHelper(this);
    }
  }
}
