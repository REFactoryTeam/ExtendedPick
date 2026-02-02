package com.ref.extendedpick.common;

import com.ref.extendedpick.ExtendedPick;
import com.ref.extendedpick.integration.ae.PortableCellDeepSearchProvider;
import com.ref.extendedpick.integration.mek.MekanismEnergyCubeSearchHelper;
import com.ref.extendedpick.network.PacketHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ExtendedPick.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExtendedPickCommonModEvents {

  @SubscribeEvent
  public static void commonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(
        () -> {
          PacketHandler.register();
          registerISearchHelper();
          registerIDeepSearchProvider();
        });
  }

  private static void registerISearchHelper() {
    SearchHelperRegistry.DefaultSearchHelper.INSTANCE.register();
    MekanismEnergyCubeSearchHelper.INSTANCE.register();
  }

  private static void registerIDeepSearchProvider() {
    DeepSearchProviderRegistry.DefaultDeepSearchProvider.INSTANCE.register();
    PortableCellDeepSearchProvider.INSTANCE.register();
  }
}
