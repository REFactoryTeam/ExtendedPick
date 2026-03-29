package com.ref.extendedpick;

import com.mojang.logging.LogUtils;
import com.ref.extendedpick.config.ExtendedPickClientConfig;
import com.ref.extendedpick.config.ExtendedPickCommonConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ExtendedPick.MOD_ID)
public class ExtendedPick {

  public static final String MOD_ID = "extendedpick";

  public static final Logger LOGGER = LogUtils.getLogger();

  public static boolean isServerModLoaded = false;

  public static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");
  public static final boolean AE_WIRELESS_TERMINAL_LOADED = ModList.get().isLoaded("ae2wtlib");

  public ExtendedPick(FMLJavaModLoadingContext context) {
    IEventBus modEventBus = context.getModEventBus();

    MinecraftForge.EVENT_BUS.register(this);

    context.registerConfig(ModConfig.Type.CLIENT, ExtendedPickClientConfig.SPEC);
    context.registerConfig(ModConfig.Type.COMMON, ExtendedPickCommonConfig.SPEC);
  }
}
