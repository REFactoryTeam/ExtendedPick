package com.ref.extendedpick.network;

import static com.ref.extendedpick.ExtendedPick.MOD_ID;

import com.ref.extendedpick.integration.jei.DeepRecipeTransferPacket;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
  private static final String PROTOCOL_VERSION = "1";
  public static final SimpleChannel CHANNEL =
      NetworkRegistry.newSimpleChannel(
          ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"),
          () -> PROTOCOL_VERSION,
          PROTOCOL_VERSION::equals,
          PROTOCOL_VERSION::equals);

  private static int packetId = 0;

  private static int nextId() {
    return packetId++;
  }

  public static void register() {
    CHANNEL.registerMessage(
        nextId(),
        DeepSearchC2SPacket.class,
        DeepSearchC2SPacket::encode,
        DeepSearchC2SPacket::new,
        DeepSearchC2SPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_SERVER));
    CHANNEL.registerMessage(
        nextId(),
        DeepRecipeTransferPacket.class,
        DeepRecipeTransferPacket::encode,
        DeepRecipeTransferPacket::decode,
        DeepRecipeTransferPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_SERVER));
  }

  public static <MSG> void sendToServer(MSG message) {
    CHANNEL.send(PacketDistributor.SERVER.noArg(), message);
  }
}
