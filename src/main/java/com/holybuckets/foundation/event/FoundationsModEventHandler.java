package com.holybuckets.foundation.event;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent;
import net.minecraftforge.registries.RegisterEvent;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = OreClustersAndRegenMain.MODID)
public class FoundationsModEventHandler {

    //create class_id
    public static final String CLASS_ID = "009";

    private static final EventRegistrar config = EventRegistrar.getInstance();



    @SubscribeEvent
    public static void onModLifecycleEvent(ModLifecycleEvent event) {
        LoggerBase.logInit( null, "009000", "ModLifecycleEvent fired: " + event.getClass().getSimpleName());
    }

    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        LoggerBase.logInit( null, "009001", "RegisterEvent fired: " + event.getClass().getSimpleName());
    }

    @SubscribeEvent
    public static void onModConfigEvent(ModConfigEvent event) {
        //event.getConfig().getFullPath();
        //need to match config.filename == hbs_utility-server.toml
        DataStore.getInstance().initWorldOnConfigLoad(event);
        LoggerBase.logDebug( null,"002007", "Mod Config Event");
    }




}
//END CLASS
