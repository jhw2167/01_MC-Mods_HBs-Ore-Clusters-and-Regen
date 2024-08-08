package com.holybuckets.orecluster.event;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.config.AllConfigs;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.RealTimeConfig;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class OreClusterForgeEventHandler {


    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event)
    {
        LevelAccessor world = event.getLevel();
        if( world.isClientSide() )
        {

        }
        else
        {
            OreClustersAndRegenMain.onLoadWorld( world );
        }

    }

    @SubscribeEvent
    public static void onUnloadWorld(LevelEvent.Unload event) {
        LevelAccessor world = event.getLevel();
        OreClustersAndRegenMain.onUnloadWorld( world );
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        OreClustersAndRegenMain.realTimeConfig.PLAYER_LOADED = true;
        LoggerBase.logDebug("Player Logged In");
    }


    //Subscribe to chunk load event
    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event)
    {
        LevelAccessor world = OreClustersAndRegenMain.realTimeConfig.LEVEL;
        if( world !=null && world.isClientSide() )
        {
            //Client side
        }
        else
        {
            //Server side

            if( OreClustersAndRegenMain.oreClusterManager != null )
            {
                OreClustersAndRegenMain.oreClusterManager.onChunkLoad( event );
            }

        }

    }

    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event)
    {
        LevelAccessor world = OreClustersAndRegenMain.realTimeConfig.LEVEL;
        if( world !=null && world.isClientSide() )
        {
            //Client side
        }
        else
        {
            //Server side

            if( OreClustersAndRegenMain.oreClusterManager != null )
            {
                OreClustersAndRegenMain.oreClusterManager.onChunkUnload( event );
            }

        }
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
