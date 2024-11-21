package com.holybuckets.foundation.modelInterface;

import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IMangedChunkData extends INBTSerializable<CompoundTag> {

    /**
     * Initialize the ManagedChunk and underlying data from memory or
     * perform any necessary configuration

     */
    public void init(String id) throws InvalidId;
    public boolean isInit(String subclass);
    public ManagedOreClusterChunk getInstance(String id) throws InvalidId;
    //public void saveNBTData(CompoundTag nbt);
    //public void loadNBTData(CompoundTag nbt);
}
