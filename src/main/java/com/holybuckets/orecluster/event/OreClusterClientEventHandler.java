package com.holybuckets.orecluster.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

//Project Imports
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.command.ClientCommands;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID, value = Dist.CLIENT)
public class OreClusterClientEventHandler {

    public OreClusterClientEventHandler() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        // Client setup code here
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommands.register(event.getDispatcher());
    }
}
