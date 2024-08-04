package com.holybuckets.orecluster.event;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.core.OreClusterManager;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.RealTimeConfig;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class OreClusterEventHandler {


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // Handle the event
        RealTimeConfig.PLAYER_LOADED = true;
        LoggerBase.logDebug("Player Logged In");
    }


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
