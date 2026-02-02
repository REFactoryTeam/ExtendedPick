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
import java.util.function.Consumer;
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
      @NotNull ItemStack container, @NotNull Consumer<IndexedStack<AEItemKey>> action) {
    var cellInventory = StorageCells.getCellInventory(container, null);
    if (cellInventory == null) {
      return;
    }
    var storedStacks = cellInventory.getAvailableStacks();
    for (var keyStack : storedStacks) {
      AEKey key = keyStack.getKey();
      if (key instanceof AEItemKey itemKey) {
        var representativeStack = itemKey.toStack();
        action.accept(new IndexedStack<>(representativeStack, itemKey));
      }
    }
  }

  @Override
  public @NotNull ItemStack extract(
      @NotNull ServerPlayer player,
      @NotNull ItemStack container,
      @NotNull AEItemKey internalIndex) {
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
    long extractedAmount =
        StorageHelper.poweredExtraction(
            menuHost,
            cellInventory,
            internalIndex,
            internalIndex.getMaxStackSize(),
            new PlayerSource(player),
            Actionable.MODULATE);
    if (extractedAmount > 0) {
      return internalIndex.toStack((int) extractedAmount);
    } else {
      return ItemStack.EMPTY;
    }
  }

  @Override
  public void register() {
    if (!ModList.get().isLoaded("ae2")) {
      return;
    }
    int aecount = 0;
    for (Item item : ForgeRegistries.ITEMS.getValues()) {
      if (item instanceof PortableCellItem pcItem && pcItem.getKeyType() == AEKeyType.items()) {
        this.register(item);
        aecount++;
      }
    }
    LOGGER.debug(
        "Dynamically registered AE2 Portable Cell Deep Search Provider for {} items.", aecount);
  }
}
