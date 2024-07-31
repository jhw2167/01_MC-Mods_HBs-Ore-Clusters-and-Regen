package com.holybuckets.orecluster;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.config.AllConfigs;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = OreClustersAndRegenMain.MODID)
public class OreClusterEventHandler {


    //Subscribe to chunk load event
    //@SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event) {
        OreClusterManager.onChunkLoad( event.getChunk() );
    }



    //@SubscribeEvent
    public void onWorldLoad(final WorldLoadEvent event)
    {
        // Capture the world seed
        Minecraft mc = AllConfigs.mc;
        AllConfigs.WORLD_SEED = mc.level.getServer().overworld().getSeed();
        AllConfigs.WORLD_SPAWN = mc.level.getServer().overworld().getSharedSpawnPos();
        LoggerBase.logInfo("**** WORLD LOAD EVENT ****");
        LoggerBase.logInfo("World Seed: " + AllConfigs.WORLD_SEED);
        LoggerBase.logInfo("World Spawn: " + AllConfigs.WORLD_SPAWN);
    }


}
