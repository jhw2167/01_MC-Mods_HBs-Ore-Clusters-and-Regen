package com.holybuckets.foundation.event;

import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.foundation.HolyBucketsUtility;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.database.DatabaseManager;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class FoundationsForgeEventHandler {

    //create class_id
    public static final String CLASS_ID = "002";

    private static final GeneralRealTimeConfig config = GeneralRealTimeConfig.getInstance();

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<LevelChunk> event)
    {
        LevelChunk chunk = event.getObject();

        if( chunk instanceof LevelChunk ) {
            //LoggerBase.logDebug("002001", "Attaching Capabilities to MOD EVENT:  ");
            if( !event.getObject().getCapability(ManagedChunkCapabilityProvider.MANAGED_CHUNK).isPresent() ) {
                event.addCapability(new ResourceLocation(HolyBucketsUtility.RESOURCE_NAMESPACE, "chunk"),
                new ManagedChunkCapabilityProvider( chunk ) );
            }
        }
    }


    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event)
    {
        LoggerBase.logDebug( null, "002003", "**** WORLD LOAD EVENT ****");

        LevelAccessor world = event.getLevel();
        if( world.isClientSide() )
        {

        }
        else
        {
            config.onLoadLevel( event );
            //start the database
            LoggerBase.logInfo( null,"002000", "Starting SQLite Database");
            HolyBucketsUtility.databaseManager = DatabaseManager.getInstance();
            try {
                //world.getLevelData().
                //HolyBucketsUtility.databaseManager.startDatabase( "world" );
            } catch (Exception e) {
                LoggerBase.logError( null,"002001", "Error starting database, attempting to cancel world load");
                event.setCanceled(true);
            }
            LoggerBase.logInfo( null,"002002", "Database started successfully");
        }

    }

    @SubscribeEvent
    public static void onUnloadWorld(LevelEvent.Unload event) {
        config.onUnLoadLevel( event );
        LoggerBase.logDebug( null,"002004", "**** WORLD UNLOAD EVENT ****");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        config.initPlayerConfigs( event );
        LoggerBase.logDebug( null,"002005", "Player Logged In");
    }

}
//END CLASS
