package com.holybuckets.orecluster;

import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = OreClustersAndRegenMain.MODID)
public class OreClusterEventHandler {


    //Subscribe to chunk load event
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        OreClusterManager.onChunkLoad( event.getChunk() );
    }

    //Subscribe to chunk unload event
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        OreClusterManager.onChunkUnload( event.getChunk() );
    }
}
