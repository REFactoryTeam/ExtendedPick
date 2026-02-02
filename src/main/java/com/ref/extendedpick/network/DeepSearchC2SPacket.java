package com.ref.extendedpick.network;

import com.ref.extendedpick.config.ExtendedPickCommonConfig;
import com.ref.extendedpick.server.ServerPickHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class DeepSearchC2SPacket {
  private final ItemStack targetStack;

  public DeepSearchC2SPacket(ItemStack targetStack) {
    this.targetStack = targetStack;
  }

  public DeepSearchC2SPacket(FriendlyByteBuf buf) {
    this.targetStack = buf.readItem();
  }

  public void encode(FriendlyByteBuf buf) {
    buf.writeItem(this.targetStack);
  }

  public static void handle(DeepSearchC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
    NetworkEvent.Context context = ctx.get();
    context.enqueueWork(
        () -> {
          ServerPlayer player = context.getSender();
          if (ExtendedPickCommonConfig.deepSearch && player != null && !msg.targetStack.isEmpty()) {
            ServerPickHandler.handleDeepSearchRequest(player, msg.targetStack);
          }
        });
    context.setPacketHandled(true);
  }
}
