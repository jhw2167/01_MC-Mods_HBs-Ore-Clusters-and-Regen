package com.holybuckets.orecluster.event;

import com.holybuckets.foundation.HolyBucketsUtility;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.orecluster.LoggerProject;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.core.OreClusterManager;
import net.minecraft.world.level.LevelAccessor;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


@EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, modid = OreClustersAndRegenMain.MODID)
public class OreClusterForgeEventHandler {

    //create class_id
    public static final String CLASS_ID = "006";


}
