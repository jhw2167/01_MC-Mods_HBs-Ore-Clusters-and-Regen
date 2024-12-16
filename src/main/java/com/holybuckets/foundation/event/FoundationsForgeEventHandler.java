package com.holybuckets.foundation.event;

import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
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


    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event)
    {
        LoggerBase.logDebug( null, "002003", "**** WORLD LOAD EVENT ****");
        config.onLoadLevel( event );

    }

    @SubscribeEvent
    public static void onUnloadWorld(LevelEvent.Unload event) {
        config.onUnLoadLevel( event );
        LoggerBase.logDebug( null,"002004", "**** WORLD UNLOAD EVENT ****");
    }





    /** PLAYER EVENTS **/

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        config.initPlayerConfigs( event );
        LoggerBase.logDebug( null,"002005", "Player Logged In");
    }

    /** ################## **/
    /** END PLAYER EVENTS **/
    /** ################## **/



    /** CHUNK EVENTS **/

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event)
    {
        config.onChunkLoad( event );
    }


    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event)
    {
        config.onChunkUnload( event );
    }

    /** ################## **/
    /** END CHUNK EVENTS **/
    /** ################## **/

}
//END CLASS
