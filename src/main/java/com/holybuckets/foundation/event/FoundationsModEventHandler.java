package com.holybuckets.foundation.event;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent;
import net.minecraftforge.registries.RegisterEvent;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = OreClustersAndRegenMain.MODID)
public class FoundationsModEventHandler {

    //create class_id
    public static final String CLASS_ID = "009";

    private static final EventRegistrar EVENT_REGISTRAR = EventRegistrar.getInstance();



    @SubscribeEvent
    public static void onModLifecycleEvent(ModLifecycleEvent event) {
        LoggerBase.logDebug( null, "009000", "ModLifecycleEvent fired: " + event.getClass().getSimpleName());
        EVENT_REGISTRAR.onModLifecycle(event);
    }

    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        LoggerBase.logDebug( null, "009001", "RegisterEvent fired: " + event.getClass().getSimpleName());
        EVENT_REGISTRAR.onRegister(event);
    }

    @SubscribeEvent
    public static void onModConfigEvent(ModConfigEvent event) {
        LoggerBase.logDebug( null,"009007", "Mod Config Event");
        EVENT_REGISTRAR.onModConfig(event);
    }


}
//END CLASS
