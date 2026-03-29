package com.ref.extendedpick.api;

import com.ref.extendedpick.ExtendedPick;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosCapability;

public interface IPlayerInventoryAccess {

  /** 定义槽位上下文，用于标记物品来源 */
  record SlotContext(int index, boolean isCurios) {}

  /**
   * 遍历玩家的所有物品槽位（包括背包和饰品栏）
   *
   * @param processor 处理逻辑：接收当前栈和它的槽位上下文
   */
  static void findStacks(Player player, BiConsumer<ItemStack, SlotContext> processor) {
    Inventory inventory = player.getInventory();
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack stack = inventory.getItem(i);
      if (!stack.isEmpty()) {
        processor.accept(stack, new SlotContext(i, false));
      }
    }
    if (ExtendedPick.CURIOS_LOADED) {
      player
          .getCapability(CuriosCapability.INVENTORY)
          .ifPresent(
              handler -> {
                IItemHandlerModifiable curiosItems = handler.getEquippedCurios();
                for (int i = 0; i < curiosItems.getSlots(); i++) {
                  ItemStack stack = curiosItems.getStackInSlot(i);
                  if (!stack.isEmpty()) {
                    processor.accept(stack, new SlotContext(i, true));
                  }
                }
              });
    }
  }

  /**
   * 根据保存的上下文安全地找回容器 ItemStack
   *
   * @param player 玩家实例
   * @param slotIndex 槽位索引
   * @param isCurios 是否是饰品栏
   * @param expectedType 预期的物品类型（用于验证内容是否发生变化）
   * @return 找到的 ItemStack，如果类型不匹配或不存在则返回 EMPTY
   */
  static ItemStack getStackFromContext(
      Player player, int slotIndex, boolean isCurios, Item expectedType) {
    ItemStack result = ItemStack.EMPTY;

    if (isCurios && ExtendedPick.CURIOS_LOADED) {
      var capability = player.getCapability(CuriosCapability.INVENTORY).resolve();
      if (capability.isPresent()) {
        IItemHandlerModifiable curios = capability.get().getEquippedCurios();
        if (slotIndex >= 0 && slotIndex < curios.getSlots()) {
          result = curios.getStackInSlot(slotIndex);
        }
      }
    } else {
      if (slotIndex >= 0 && slotIndex < player.getInventory().getContainerSize()) {
        result = player.getInventory().getItem(slotIndex);
      }
    }

    return (result.getItem() == expectedType) ? result : ItemStack.EMPTY;
  }
}
