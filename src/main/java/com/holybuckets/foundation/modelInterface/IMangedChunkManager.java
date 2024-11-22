package com.holybuckets.foundation.modelInterface;

import com.holybuckets.foundation.exception.InvalidId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces a method to return ConcurrentHashMap of ManagedChunkData objects
 */
public interface IMangedChunkManager {

    public ConcurrentHashMap<String, IMangedChunkData> getManagedChunks();

    public Map<String, IMangedChunkManager> getManager(LevelAccessor level);
}
