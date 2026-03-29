package com.ref.extendedpick.integration.jei;

import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.IPlayerInventoryAccess;
import com.ref.extendedpick.config.ExtendedPickCommonConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import mezz.jei.common.transfer.BasicRecipeTransferHandlerServer;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class DeepRecipeTransferPacket {

  private final Collection<TransferOperation> normalOperations;
  private final Collection<Integer> craftingSlotIndices;
  private final Collection<Integer> inventorySlotIndices;
  private final boolean maxTransfer;
  private final boolean requireCompleteSets;
  private final Collection<DeepTransferData> deepTransfers;

  public DeepRecipeTransferPacket(
      Collection<TransferOperation> normalOperations,
      Collection<Integer> craftingSlotIndices,
      Collection<Integer> inventorySlotIndices,
      boolean maxTransfer,
      boolean requireCompleteSets,
      Collection<DeepTransferData> deepTransfers) {
    this.normalOperations = normalOperations;
    this.craftingSlotIndices = craftingSlotIndices;
    this.inventorySlotIndices = inventorySlotIndices;
    this.maxTransfer = maxTransfer;
    this.requireCompleteSets = requireCompleteSets;
    this.deepTransfers = deepTransfers;
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeVarInt(normalOperations.size());
    for (TransferOperation op : normalOperations) {
      op.writePacketData(buf);
    }

    buf.writeVarInt(craftingSlotIndices.size());
    craftingSlotIndices.forEach(buf::writeVarInt);

    buf.writeVarInt(inventorySlotIndices.size());
    inventorySlotIndices.forEach(buf::writeVarInt);

    buf.writeBoolean(maxTransfer);
    buf.writeBoolean(requireCompleteSets);

    buf.writeVarInt(deepTransfers.size());
    for (DeepTransferData dt : deepTransfers) {
      dt.write(buf);
    }
  }

  public static DeepRecipeTransferPacket decode(FriendlyByteBuf buf) {
    int opSize = buf.readVarInt();
    List<TransferOperation> ops = new ArrayList<>();
    for (int i = 0; i < opSize; i++) {
      ops.add(new TransferOperation(buf.readVarInt(), buf.readVarInt()));
    }

    int craftSize = buf.readVarInt();
    List<Integer> crafts = new ArrayList<>();
    for (int i = 0; i < craftSize; i++) {
      crafts.add(buf.readVarInt());
    }

    int invSize = buf.readVarInt();
    List<Integer> invs = new ArrayList<>();
    for (int i = 0; i < invSize; i++) {
      invs.add(buf.readVarInt());
    }

    boolean maxTransfer = buf.readBoolean();
    boolean requireCompleteSets = buf.readBoolean();

    int dtSize = buf.readVarInt();
    List<DeepTransferData> dts = new ArrayList<>();
    for (int i = 0; i < dtSize; i++) {
      dts.add(DeepTransferData.read(buf));
    }

    return new DeepRecipeTransferPacket(ops, crafts, invs, maxTransfer, requireCompleteSets, dts);
  }

  public static void handle(DeepRecipeTransferPacket msg, Supplier<NetworkEvent.Context> ctx) {
    ctx.get()
        .enqueueWork(
            () -> {
              ServerPlayer player = ctx.get().getSender();
              if (!ExtendedPickCommonConfig.deepSearch) return;
              if (player == null) return;
              transfer(msg, player);
            });
    ctx.get().setPacketHandled(true);
  }

  private static void transfer(DeepRecipeTransferPacket msg, ServerPlayer player) {
    AbstractContainerMenu container = player.containerMenu;
    List<Slot> craftingSlots = msg.craftingSlotIndices.stream().map(container::getSlot).toList();
    List<Slot> inventorySlots = msg.inventorySlotIndices.stream().map(container::getSlot).toList();

    int jeiTransferredSets;
    boolean hasNormalOps = !msg.normalOperations.isEmpty();

    if (hasNormalOps) {
      BasicRecipeTransferHandlerServer.setItems(
          player,
          msg.normalOperations.stream().toList(),
          craftingSlots,
          inventorySlots,
          msg.maxTransfer,
          msg.requireCompleteSets);

      jeiTransferredSets = Integer.MAX_VALUE;
      for (TransferOperation op : msg.normalOperations) {
        Slot craftSlot = op.craftingSlot(container);
        ItemStack stack = craftSlot.getItem();
        if (stack.isEmpty()) {
          jeiTransferredSets = 0;
          break;
        }
        jeiTransferredSets = Math.min(jeiTransferredSets, stack.getCount());
      }

      if (jeiTransferredSets == 0 && msg.requireCompleteSets) {
        return;
      }
    } else {
      clearCraftingGridToInventory(player, craftingSlots);
      jeiTransferredSets = msg.maxTransfer ? 64 : 1;
    }

    if (msg.deepTransfers.isEmpty()) {
      return;
    }
    int successfulDeepSets = 0;
    ItemStack[] accumulatedDeepItems = new ItemStack[msg.deepTransfers.size()];
    Arrays.fill(accumulatedDeepItems, ItemStack.EMPTY);

    int targetLoops = Math.min(jeiTransferredSets, msg.maxTransfer ? 64 : 1);

    for (int i = 0; i < targetLoops; i++) {
      boolean setComplete = true;
      ItemStack[] currentSet = new ItemStack[msg.deepTransfers.size()];
      int opIndex = 0;

      for (DeepTransferData deepOp : msg.deepTransfers) {
        ItemStack pulled = extractExactOne(player, deepOp);
        if (pulled.isEmpty()) {
          setComplete = false;
          break;
        }
        currentSet[opIndex++] = pulled;
      }

      if (setComplete) {
        boolean maxStackReached = false;
        for (int j = 0; j < msg.deepTransfers.size(); j++) {
          ItemStack acc = accumulatedDeepItems[j];
          if (!acc.isEmpty() && acc.getCount() >= acc.getMaxStackSize()) {
            maxStackReached = true;
            break;
          }
        }

        if (maxStackReached) {
          for (int j = 0; j < msg.deepTransfers.size(); j++) {
            player.getInventory().placeItemBackInInventory(currentSet[j]);
          }
          break;
        }

        successfulDeepSets++;
        for (int j = 0; j < msg.deepTransfers.size(); j++) {
          if (accumulatedDeepItems[j].isEmpty()) {
            accumulatedDeepItems[j] = currentSet[j];
          } else {
            accumulatedDeepItems[j].grow(1);
          }
        }
      } else {

        for (int j = 0; j < opIndex; j++) {
          player.getInventory().placeItemBackInInventory(currentSet[j]);
        }
        break;
      }
    }

    if (successfulDeepSets == 0 && msg.requireCompleteSets) {
      if (hasNormalOps) {
        clearCraftingGridToInventory(player, craftingSlots);
      }
      return;
    }

    if (hasNormalOps && successfulDeepSets < jeiTransferredSets) {
      for (TransferOperation op : msg.normalOperations) {
        Slot craftSlot = op.craftingSlot(container);
        ItemStack stack = craftSlot.getItem();
        if (stack.getCount() > successfulDeepSets) {
          int excess = stack.getCount() - successfulDeepSets;
          ItemStack remainder = craftSlot.safeTake(excess, excess, player);
          player.getInventory().placeItemBackInInventory(remainder);
        }
      }
    }

    int opIndex = 0;
    for (DeepTransferData deepOp : msg.deepTransfers) {
      Slot craftSlot = container.getSlot(deepOp.craftingSlotIndex());
      ItemStack toInsert = accumulatedDeepItems[opIndex++];
      if (!toInsert.isEmpty()) {
        if (craftSlot.getItem().isEmpty()) {
          craftSlot.set(toInsert);
        } else if (ItemStack.isSameItemSameTags(craftSlot.getItem(), toInsert)) {
          craftSlot.getItem().grow(toInsert.getCount());
        }
      }
    }

    container.broadcastChanges();
  }

  private static void clearCraftingGridToInventory(ServerPlayer player, List<Slot> craftingSlots) {
    for (Slot craftingSlot : craftingSlots) {
      ItemStack item = craftingSlot.getItem();
      if (item.isEmpty()) {
        continue;
      }
      int count = item.getCount();
      ItemStack remainder = craftingSlot.safeTake(count, count, player);
      player.getInventory().placeItemBackInInventory(remainder);
    }
  }

  public static ItemStack extractExactOne(ServerPlayer player, DeepTransferData deepOp) {
    ItemStack[] extractedRef = new ItemStack[] {ItemStack.EMPTY};

    IPlayerInventoryAccess.findStacks(
        player,
        (containerItem, context) -> {
          if (!extractedRef[0].isEmpty() || containerItem.isEmpty()) {
            return;
          }

          IDeepSearchProvider<?> provider = IDeepSearchProvider.getProvider(containerItem);
          if (provider != null) {
            provider.forEachItem(
                containerItem,
                indexed -> {
                  for (ItemStack validTarget : deepOp.validItems()) {
                    if (ItemStack.isSameItemSameTags(validTarget, indexed.stack())) {
                      ItemStack extracted =
                          provider.invokeExtract(player, containerItem, indexed.index(), 1, false);
                      if (!extracted.isEmpty()) {
                        extractedRef[0] = extracted;
                        return false;
                      }
                    }
                  }
                  return true;
                });
          }
        });
    return extractedRef[0];
  }
}
