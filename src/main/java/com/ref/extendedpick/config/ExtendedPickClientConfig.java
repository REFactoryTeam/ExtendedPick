package com.ref.extendedpick.config;

import com.ref.extendedpick.ExtendedPick;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ExtendedPick.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExtendedPickClientConfig {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

  private static final ForgeConfigSpec.ConfigValue<Boolean> PickInfoLog =
      BUILDER
          .comment("Enable or disable logging information when using the pick function.")
          .define("PickInfoLog", false);

  private static final ForgeConfigSpec.ConfigValue<Boolean> CreativePickNBTEntity =
      BUILDER
          .comment(
              "Allow picking entities with NBT data when in creative mode and holding control.")
          .define("CreativePickNBTEntity", true);

  private static final ForgeConfigSpec.ConfigValue<Boolean> ExPick =
      BUILDER.comment("Enable or disable the Extended Pick.").define("ExPick", true);

  private static final ForgeConfigSpec.ConfigValue<Boolean> ExPickPacket =
      BUILDER
          .comment("Enable or disable Extended Pick packet sending.")
          .define("ExPickPacket", true);

  public static final ForgeConfigSpec SPEC = BUILDER.build();

  public static boolean pickInfoLog;
  public static boolean creativePickNBTEntity;
  public static boolean exPick;
  public static boolean exPickPacket;

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    if (event.getConfig().getSpec() != SPEC) return;
    pickInfoLog = PickInfoLog.get();
    creativePickNBTEntity = CreativePickNBTEntity.get();
    exPick = ExPick.get();
    exPickPacket = ExPickPacket.get();
  }
}
