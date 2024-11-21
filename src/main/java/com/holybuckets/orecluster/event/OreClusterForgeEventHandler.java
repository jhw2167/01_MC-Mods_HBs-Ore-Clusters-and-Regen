package com.holybuckets.orecluster.event;

import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.core.OreClusterManager;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class OreClusterForgeEventHandler {

    //create class_id
    public static final String CLASS_ID = "006";


    //Subscribe to chunk load event
    @SubscribeEvent
    //public static void onChunkLoad(final ChunkEvent.Load event)
    public static void onChunkLoad(final ChunkEvent.Load event)
    {

        LevelAccessor level = event.getLevel();
        if( level !=null && level.isClientSide() )
        {
            //Client side
        }
        else
        {
            if( OreClustersAndRegenMain.oreClusterManagers != null ) {
                OreClusterManager manager = OreClustersAndRegenMain.oreClusterManagers.get( level );
                if( manager != null ) {
                    manager.onChunkLoad( event );
                }
            }
        }

    }

    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event)
    {
        LevelAccessor level = event.getLevel();
        if( level !=null && level.isClientSide() )
        {
            //Client side
        }
        else
        {
            if( OreClustersAndRegenMain.oreClusterManagers != null ) {
                OreClusterManager manager = OreClustersAndRegenMain.oreClusterManagers.get( level );
                if( manager != null ) {
                    manager.onChunkUnload( event );
                }
            }
        }
    }



}
