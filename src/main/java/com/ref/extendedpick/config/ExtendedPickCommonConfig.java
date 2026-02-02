package com.ref.extendedpick.config;

import com.ref.extendedpick.ExtendedPick;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ExtendedPick.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExtendedPickCommonConfig {
  private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

  private static final ForgeConfigSpec.ConfigValue<Boolean> DeepSearch =
      BUILDER.comment("deep search").define("deepSearch", true);

  public static final ForgeConfigSpec SPEC = BUILDER.build();

  public static boolean deepSearch;

  @SubscribeEvent
  static void onLoad(final ModConfigEvent event) {
    if (event.getConfig().getSpec() != SPEC) return;
    deepSearch = DeepSearch.get();
  }
}
