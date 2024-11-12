package com.holybuckets.orecluster.event;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.RealTimeConfig;
import com.holybuckets.orecluster.config.AllConfigs;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = OreClustersAndRegenMain.MODID)
public class OreClusterModEventHandler {


    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event)
    {

        AllConfigs.onLoad( event );
        OreClustersAndRegenMain.onLoad( event );

        LoggerBase.logInit( "006002","Handler-onLoad" );
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event)
    {
        AllConfigs.onReload( event );
        OreClustersAndRegenMain.onReload( event );

        LoggerBase.logInit( "006003", "Handler-onReLoad" );
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
