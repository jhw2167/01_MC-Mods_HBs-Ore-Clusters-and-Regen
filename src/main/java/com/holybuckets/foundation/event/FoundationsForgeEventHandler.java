package com.holybuckets.foundation.event;

import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerLifecycleEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class FoundationsForgeEventHandler {

    //create class_id
    public static final String CLASS_ID = "002";

    private static final EventRegistrar EVENT_REGISTRAR = EventRegistrar.getInstance();

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<LevelChunk> event)
    {

        if( event.getObject() instanceof LevelChunk )
        {
            LevelChunk chunk = event.getObject();
            if(chunk.getLevel().isClientSide())
                return;
            //LoggerBase.logDebug("002001", "Attaching Capabilities to MOD EVENT:  ");
            if( !event.getObject().getCapability(ManagedChunkCapabilityProvider.MANAGED_CHUNK).isPresent() ) {
                event.addCapability(new ResourceLocation(HBUtil.RESOURCE_NAMESPACE, "chunk"),
                new ManagedChunkCapabilityProvider( chunk ) );
            }
        }
    }

    /**
     * This is a serverl level, not a dimension level, this holds the name of the save file
     *  ServerLevel.getLevel().getLevelData().getLevelName()
     * This also has dimension type data:
     *  event.level.dimensionTypeId().location.toString()
     * @param event
     */
    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event)
    {
        LoggerBase.logDebug( null, "002003", "**** WORLD LOAD EVENT ****");
        EVENT_REGISTRAR.onLoadLevel( event );

    }

    @SubscribeEvent
    public static void onUnloadWorld(LevelEvent.Unload event) {
        EVENT_REGISTRAR.onUnLoadLevel( event );
        LoggerBase.logDebug( null,"002004", "**** WORLD UNLOAD EVENT ****");
    }

    /** Server Events **/

    @SubscribeEvent
    public static void serverLifecycleEvent(ServerLifecycleEvent event)
    {
        //Minecraft.getInstance().gameDirectory
        if(event instanceof ServerStartedEvent) {
            LoggerBase.logInit( null, "009002", "ServerLifecycleEvent fired: Started");
            EVENT_REGISTRAR.onServerStart( event );
        }

        if(event instanceof ServerStoppedEvent) {
            LoggerBase.logInit( null, "009003", "ServerLifecycleEvent fired: Stopped");
            EVENT_REGISTRAR.onServerStop( event );
        }
    }




    /** PLAYER EVENTS **/

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        EVENT_REGISTRAR.initPlayerConfigs( event );
        LoggerBase.logDebug( null,"002005", "Player Logged In");
    }

    /** ################## **/
    /** END PLAYER EVENTS **/
    /** ################## **/



    /** CHUNK EVENTS **/

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event)
    {
        EVENT_REGISTRAR.onChunkLoad( event );
    }


    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event)
    {
        EVENT_REGISTRAR.onChunkUnload( event );
    }

    /** ################## **/
    /** END CHUNK EVENTS **/
    /** ################## **/

}
//END CLASS
