package com.holybuckets.foundation.model;

import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.orecluster.core.OreClusterManager;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    public static final String NBT_KEY_HEADER = "managedChunk";
    public static final GeneralRealTimeConfig GENERAL_CONFIG = GeneralRealTimeConfig.getInstance();

    private String id;
    private LevelAccessor level;
    private ChunkAccess chunk;
    private ManagedOreClusterChunk managedOreClusterChunk;

    /** CONSTRUCTORS **/
    private ManagedChunk() {
        super();
    }

    public ManagedChunk( CompoundTag tag ) {
        super();
        this.deserializeNBT(tag);
    }

    public ManagedChunk(LevelAccessor level, String id)
    {
        this();
        this.id = id;
        try {
            init(level, id);
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
    public void init(LevelAccessor level, String id) throws InvalidId
    {
        this.id = id;
        this.level = level;

        LoggerBase.logInfo("003000", "Initializing ManagedChunk with id: " + id);
        int errorCount = 0;
        try {
            this.managedOreClusterChunk = this.managedOreClusterChunk.getInstance(id);
        } catch (InvalidId e) {
            errorCount++;
            LoggerBase.logDebug("003001", "No managedOreClusterChunk found with id: " + id);
        }

    }

    /**
    * Check if all subclasses are not null and initialized successfully
    * @return boolean
    */
    @Override
    public boolean isInit(String subClass) {
        boolean isInit = false;
        IMangedChunkData sub = this.managedOreClusterChunk;
        boolean subClassInit =  (sub != null && sub.isInit(subClass));

        if(subClass.equals("ManagedOreClusterChunk")) {
            isInit = subClassInit;
        }

        return isInit;
    }

    @Override
    public ManagedOreClusterChunk getInstance(String id) throws InvalidId {
        return null;
    }

    @Override
    public CompoundTag serializeNBT()
    {
        if( this.id != null && this.id.contains("6") )
        {
            LoggerBase.logDebug("003003", "Serializing ManagedChunk " + this.id);
        }


        CompoundTag tag = new CompoundTag();
        tag.putString("id", this.id);
        tag.putString("level", this.level.hashCode() + "");
        tag.put("managedOreClusterChunk", this.managedOreClusterChunk.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if(tag == null)
            return;

        //print tag as string, info
        LoggerBase.logInfo("003005", "Deserializing ManagedChunk: " + tag.toString());
        this.id = tag.getString("id");
        this.level = GENERAL_CONFIG.getLEVELS().get( tag.get("level") );
        this.managedOreClusterChunk.deserializeNBT(tag.getCompound("managedOreClusterChunk"));
    }
}
