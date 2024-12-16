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
import net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent;
import net.minecraftforge.registries.RegisterEvent;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = OreClustersAndRegenMain.MODID)
public class FoundationsModEventHandler {

    //create class_id
    public static final String CLASS_ID = "009";

    private static final GeneralRealTimeConfig config = GeneralRealTimeConfig.getInstance();


    @SubscribeEvent
    public static void onModLifecycleEvent(ModLifecycleEvent event) {
        LoggerBase.logInit( null, "009000", "ModLifecycleEvent fired: " + event.getClass().getSimpleName());
    }

    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        LoggerBase.logInit( null, "009001", "RegisterEvent fired: " + event.getClass().getSimpleName());
    }


}
//END CLASS
