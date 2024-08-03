package com.holybuckets.orecluster;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.config.AllConfigs;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.logging.Level;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class OreClusterEventHandler {


    //Subscribe to chunk load event
    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event) {
        if( RealTimeConfig.WORLD_SEED == null )
            RealTimeConfig.initWorldConfigs( event.getLevel() );

        OreClusterManager.onChunkLoad( event.getChunk() );
    }


    /*
    One Day...

    @SubscribeEvent
    public void onLevelLoad(final LevelEvent.Load level )
    {
        // Capture the world seed
        LoggerBase.logInfo("**** WORLD LOAD EVENT ****");
        Minecraft mc = AllConfigs.mc;
        AllConfigs.WORLD_SEED = mc.level.getServer().overworld().getSeed();
        AllConfigs.WORLD_SPAWN = mc.level.getServer().overworld().getSharedSpawnPos();
        LoggerBase.logInfo("World Seed: " + AllConfigs.WORLD_SEED);
        LoggerBase.logInfo("World Spawn: " + AllConfigs.WORLD_SPAWN);
    }
    */



}
