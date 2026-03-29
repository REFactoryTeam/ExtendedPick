package com.ref.extendedpick.integration.ae;

import static com.ref.extendedpick.ExtendedPick.LOGGER;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.StorageCells;
import appeng.api.storage.StorageHelper;
import appeng.items.tools.powered.AbstractPortableCell;
import appeng.items.tools.powered.PortableCellItem;
import appeng.me.helpers.PlayerSource;
import com.ref.extendedpick.api.IDeepSearchProvider;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public enum PortableCellDeepSearchProvider implements IDeepSearchProvider<AEItemKey> {
  INSTANCE;

  @Override
  public void forEachItem(
      @NotNull ItemStack container, @NotNull Predicate<IndexedStack<AEItemKey>> action) {
    var cellInventory = StorageCells.getCellInventory(container, null);
    if (cellInventory == null) {
      return;
    }

    for (var keyStack : cellInventory.getAvailableStacks()) {
      AEKey key = keyStack.getKey();
      long amount = keyStack.getLongValue();
      if (key instanceof AEItemKey itemKey) {
        int stackCount = (int) Math.min(Integer.MAX_VALUE, amount);
        if (!action.test(new IndexedStack<>(itemKey.toStack(stackCount), itemKey))) {
          break;
        }
      }
    }
  }

  @Override
  public @NotNull ItemStack extract(
      @NotNull ServerPlayer player,
      @NotNull ItemStack container,
      @NotNull AEItemKey internalIndex,
      int amount,
      boolean simulate) {

    if (container.isEmpty()
        || !(container.getItem() instanceof AbstractPortableCell portableCell)) {
      return ItemStack.EMPTY;
    }

    var cellInventory = StorageCells.getCellInventory(container, null);
    if (cellInventory == null) {
      return ItemStack.EMPTY;
    }

    var menuHost = portableCell.getMenuHost(player, -1, container, null);
    if (menuHost == null) {
      return ItemStack.EMPTY;
    }

    int extractAmount =
        amount == IDeepSearchProvider.MAX_STACK_SIZE ? internalIndex.getMaxStackSize() : amount;
    long extractedAmount =
        StorageHelper.poweredExtraction(
            menuHost,
            cellInventory,
            internalIndex,
            extractAmount,
            new PlayerSource(player),
            Actionable.ofSimulate(simulate));

    return extractedAmount > 0 ? internalIndex.toStack((int) extractedAmount) : ItemStack.EMPTY;
  }

  @Override
  public void register() {
    if (!ModList.get().isLoaded("ae2")) {
      return;
    }

    long aeCount = 0L;
    for (Item item : ForgeRegistries.ITEMS.getValues()) {
      if (item instanceof PortableCellItem pcItem && pcItem.getKeyType() == AEKeyType.items()) {
        register(item);
        aeCount++;
      }
    }

    LOGGER.debug(
        "Dynamically registered AE2 Portable Cell Deep Search Provider for {} items.", aeCount);
  }
}
