package com.holybuckets.orecluster.event;

import com.holybuckets.foundation.HolyBucketsUtility;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.database.DatabaseManager;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.model.ManagedChunk;
import com.holybuckets.orecluster.modelinterface.IMangedChunk;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.core.Direction;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.*;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class FoundationsForgeEventHandler {

    //create class_id
    public static final String CLASS_ID = "002";


    @SubscribeEvent
    public static void onLoadWorld(LevelEvent.Load event)
    {
        LevelAccessor world = event.getLevel();
        if( world.isClientSide() )
        {

        }
        else
        {
            //start the database
            LoggerBase.logInfo("002000", "Starting SQLite Database");
            HolyBucketsUtility.databaseManager = DatabaseManager.getInstance();
            try {
                //world.getLevelData().
                HolyBucketsUtility.databaseManager.startDatabase( "world" );
            } catch (Exception e) {
                LoggerBase.logError("002000", "Error starting database, attempting to cancel world load");
                event.setCanceled(true);
            }
            LoggerBase.logInfo("002001", "Database started successfully");
        }

    }

}
//END CLASS
