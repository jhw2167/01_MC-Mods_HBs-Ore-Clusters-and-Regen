package com.holybuckets.foundation.model;

import com.holybuckets.foundation.Exception.InvalidId;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ManagedChunk implements IMangedChunkData {

    private String id;
    private ChunkAccess chunk;
    private ManagedOreClusterChunk managedOreClusterChunk;

    /** CONSTRUCTORS **/
    public ManagedChunk() {
        this.managedOreClusterChunk = new ManagedOreClusterChunk();
    }

    public ManagedChunk(String id) {
        this.id = id;
        try {
            init(id);
        } catch (InvalidId e) {
            LoggerBase.logError("003002", "Error initializing ManagedChunk with id: " + id);
        }

    }

    /** GETTERS & SETTERS **/
    public ManagedOreClusterChunk getManagedOreClusterChunk() {
        return managedOreClusterChunk;
    }

    public void setManagedOreClusterChunk(ManagedOreClusterChunk c) {
        this.managedOreClusterChunk = c;
    }


    /** OVERRIDES **/
    @Override
    public void init(String id) throws InvalidId
    {
        LoggerBase.logInfo("003000", "Initializing ManagedChunk with id: " + id);
        int errorCount = 0;
        try {
            this.managedOreClusterChunk = this.managedOreClusterChunk.getInstance(id);
        } catch (InvalidId e) {
            errorCount++;
            LoggerBase.logDebug("003001", "No managedOreClusterChunk found with id: " + id);
        }

    }

    @Override
    public ManagedOreClusterChunk getInstance(String id) throws InvalidId {
        return null;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("managedOreClusterChunk", this.managedOreClusterChunk.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        this.managedOreClusterChunk.deserializeNBT(compoundTag.getCompound("managedOreClusterChunk"));
    }
}
