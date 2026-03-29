package com.ref.extendedpick.client;

import static com.ref.extendedpick.ExtendedPick.LOGGER;

import com.ref.extendedpick.ExtendedPick;
import com.ref.extendedpick.api.ISearchHelper;
import com.ref.extendedpick.common.SearchHelperRegistry;
import com.ref.extendedpick.config.ExtendedPickClientConfig;
import com.ref.extendedpick.network.DeepSearchC2SPacket;
import com.ref.extendedpick.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
    modid = ExtendedPick.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class ExtendedPickClientForgeEvents {

  @SubscribeEvent
  public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
    ExtendedPick.isServerModLoaded = PacketHandler.CHANNEL.isRemotePresent(event.getConnection());
  }

  @SubscribeEvent
  public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
    ExtendedPick.isServerModLoaded = false;
  }

  @SubscribeEvent
  public static void onPickBlock(InputEvent.InteractionKeyMappingTriggered event) {
    Minecraft mc = Minecraft.getInstance();
    if (event.isCanceled() || event.getKeyMapping() != mc.options.keyPickItem) return;
    LocalPlayer player = mc.player;
    HitResult hitResult = mc.hitResult;
    if (player == null
        || mc.level == null
        || hitResult == null
        || hitResult.getType() == HitResult.Type.MISS) return;
    ItemStack targetStack = ItemStack.EMPTY;
    if (hitResult.getType() == HitResult.Type.BLOCK) {
      BlockHitResult blockHit = (BlockHitResult) hitResult;
      BlockState blockState = player.level().getBlockState(blockHit.getBlockPos());
      if (ExtendedPickClientConfig.pickInfoLog) {
        LOGGER.debug(
            "Picking Block: {} ({}) ({})",
            ForgeRegistries.BLOCKS.getKey(blockState.getBlock()),
            blockState,
            blockState.getBlock().getClass());
        BlockEntity be = player.level().getBlockEntity(blockHit.getBlockPos());
        if (be != null) {
          LOGGER.debug(
              " -> Block has Block Entity: {} ({}) with full NBT: {}",
              ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType()),
              be.getClass(),
              be.saveWithFullMetadata());
        }
      }
      targetStack =
          blockState.getCloneItemStack(hitResult, player.level(), blockHit.getBlockPos(), player);
    } else if (hitResult.getType() == HitResult.Type.ENTITY) {
      Entity entity = ((EntityHitResult) hitResult).getEntity();
      targetStack = entity.getPickedResult(hitResult);
      if (ExtendedPickClientConfig.pickInfoLog) {
        ExtendedPick.LOGGER.debug(
            "Picking Entity: {} ({}) with NBT: {}",
            ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()),
            entity.getClass(),
            entity.saveWithoutId(new CompoundTag()));
      }
      if (ExtendedPickClientConfig.creativePickNBTEntity
          && player.getAbilities().instabuild
          && Screen.hasControlDown()
          && targetStack != null) {
        ItemStack creativeStack = targetStack.copy();
        CompoundTag entityNbt = entity.saveWithoutId(new CompoundTag());
        entityNbt.remove("Pos");
        entityNbt.remove("Motion");
        entityNbt.remove("Rotation");
        entityNbt.remove("UUID");
        creativeStack.getOrCreateTag().put("EntityTag", entityNbt);
        if (entity.hasCustomName()) {
          creativeStack.setHoverName(entity.getCustomName());
        }
        event.setCanceled(true);
        player.getInventory().setPickedItem(creativeStack);
        if (mc.gameMode != null) {
          mc.gameMode.handleCreativeModeItemAdd(
              player.getItemInHand(InteractionHand.MAIN_HAND), 36 + player.getInventory().selected);
        }
        return;
      }
    }
    if (!ExtendedPickClientConfig.exPick) return;
    if (targetStack == null || targetStack.isEmpty()) return;
    if (player.getAbilities().instabuild || !player.getInventory().getSelected().isEmpty()) return;
    boolean playerHasExactItem = player.getInventory().findSlotMatchingItem(targetStack) != -1;
    boolean isSurvivalEntityPick =
        hitResult.getType() == HitResult.Type.ENTITY && !player.getAbilities().instabuild;
    if (playerHasExactItem && !isSurvivalEntityPick) return;

    event.setCanceled(true);
    if (ExtendedPickClientConfig.pickInfoLog) {
      LOGGER.debug(
          "Extended Pick an {}({}) with NBT: {}",
          ForgeRegistries.ITEMS.getKey(targetStack.getItem()),
          targetStack.getItem().getClass(),
          targetStack.getTag());
    }
    exPickBlock(targetStack, player, mc);
  }

  private static void exPickBlock(ItemStack targetStack, LocalPlayer player, Minecraft mc) {
    ISearchHelper searchHelper =
        SearchHelperRegistry.getInstance().getHelper(targetStack.getItem());
    Inventory inventory = player.getInventory();

    int bestSlot = -1;
    int maxScore = ISearchHelper.Failed;

    for (int i = 0; i < inventory.items.size(); i++) {
      ItemStack candidate = inventory.getItem(i);
      int score = searchHelper.getMatchScore(targetStack, candidate);
      if (score > maxScore) {
        maxScore = score;
        bestSlot = i;
      }
      if (score == ISearchHelper.Success) {
        break;
      }
    }
    if (bestSlot != -1) {
      if (Inventory.isHotbarSlot(bestSlot)) {
        inventory.selected = bestSlot;
      } else {
        if (mc.gameMode != null) {
          mc.gameMode.handlePickItem(bestSlot);
        }
      }
      return;
    }
    if (!ExtendedPickClientConfig.exPickPacket) return;
    PacketHandler.sendToServer(new DeepSearchC2SPacket(targetStack));
  }
}
