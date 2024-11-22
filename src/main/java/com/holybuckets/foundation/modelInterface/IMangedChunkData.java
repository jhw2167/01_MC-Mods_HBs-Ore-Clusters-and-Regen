package com.holybuckets.foundation.modelInterface;

import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.core.OreClusterManager;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public interface IMangedChunkData extends INBTSerializable<CompoundTag> {

    /**
     * Initialize the ManagedChunk and underlying data from memory or
     * perform any necessary configuration
     */
    boolean isInit(String subclass);

    /**
     * Create a dummy constructor to call this method and return a reference to an
     * existing instance of the class sitting in RAM. I am aware this is a hack and
     * not an elegant implementation
     * @return IMangedChunkData
     */
    public IMangedChunkData getStaticInstance(LevelAccessor level, String id);
}
