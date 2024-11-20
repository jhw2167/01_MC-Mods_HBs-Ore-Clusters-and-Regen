package com.holybuckets.orecluster.event;

import com.holybuckets.foundation.HolyBucketsUtility;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.database.DatabaseManager;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraft.world.level.LevelAccessor;

import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;


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
