package com.holybuckets.foundation.model;

import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.foundation.modelInterface.IMangedChunkManager;
import com.holybuckets.orecluster.core.OreClusterManager;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashMap;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    public static final String NBT_KEY_HEADER = "managedChunk";
    public static final GeneralRealTimeConfig GENERAL_CONFIG = GeneralRealTimeConfig.getInstance();
    public static final HashMap<Integer, ManagedChunk> MANAGED_CHUNKS = new HashMap<>();

    private String id;
    private LevelAccessor level;
    private ChunkAccess chunk;
    private int tickLastLoaded;
    private HashMap<Integer, IMangedChunkData> managedChunkData = new HashMap<>();


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
        this.level = level;
        //Dont init here, working static maps will never be ready at this time
    }

    /** GETTERS & SETTERS **/
    public IMangedChunkData getSubclass(Class<? extends IMangedChunkData> classObject) {
        return managedChunkData.get(classObject.hashCode());
    }

    /**
     * Set a managed chunk data subclass
     * @param classObject The class of the managed chunk data
     * @param data The managed chunk data instance
     * @return true if set successfully
     */
    public Boolean setSubclass(Class<? extends IMangedChunkData> classObject, IMangedChunkData data) {
        if (classObject == null || data == null) {
            return false;
        }
        managedChunkData.put(classObject.hashCode(), data);
        return true;
    }


    /** OVERRIDES **/
    @Override
    public void init(LevelAccessor level, String id) throws InvalidId
    {
        this.id = id;
        this.level = level;

        //LoggerBase.logInfo("003000", "Initializing ManagedChunk with id: " + id);
        HashMap<String, String> errors = new HashMap<>();

        IMangedChunkData oreClusterChunk = new ManagedOreClusterChunk();
        IMangedChunkData instance = oreClusterChunk.getStaticInstance(level, id);
        if(instance == null) {
            errors.put(oreClusterChunk.getClass().getName(), "returned null");
        } else {
            setSubclass(ManagedOreClusterChunk.class, instance);
        }

        if(errors.size() > 0)
        {
        //Add all errors in list to error message
            StringBuilder error = new StringBuilder();
            for (String key : errors.keySet()) {
                error.append(key).append(": ").append(errors.get(key)).append("\n");
            }
            throw new InvalidId(error.toString());
        }

    }

    /**
    * Check if all subclasses are not null and initialized successfully
    * @return boolean
    */
    @Override
    public boolean isInit(String subClass) {
        if(subClass.equals("ManagedOreClusterChunk")) {
            IMangedChunkData data = getSubclass(ManagedOreClusterChunk.class);
            return data != null && data.isInit(subClass);
        }
        return false;
    }

    @Override
    public IMangedChunkData getStaticInstance(LevelAccessor level, String id) {
        return this;
    }


    @Override
    public CompoundTag serializeNBT()
    {

        CompoundTag details = new CompoundTag();
        CompoundTag wrapper = new CompoundTag();

        details.putString("id", this.id);
        details.putInt("level", this.level.hashCode());
        for(IMangedChunkData data : managedChunkData.values()) {
            details.put(data.getClass().getName(), data.serializeNBT());
        }

        if( this.id != null)
            wrapper.put(NBT_KEY_HEADER, details);

        if( this.id != null && this.id.contains("16") )
        {
            LoggerBase.logDebug("003003", "Serializing ManagedChunk " + this.id);
            LoggerBase.logDebug("003004", "This String: " + this.toString());
            LoggerBase.logDebug("003005", "Wrapper String: " + wrapper.toString());
            IMangedChunkData oreClusterData = getSubclass(ManagedOreClusterChunk.class);
            if(oreClusterData != null)
                LoggerBase.logDebug("003006", "OreCluster String: " + oreClusterData.toString());

        }

        return wrapper;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if(tag == null)
            return;

        CompoundTag details = tag.getCompound(NBT_KEY_HEADER);

        //print tag as string, info
        this.id = details.getString("id");
        this.level = GENERAL_CONFIG.getLEVELS().get( tag.get("level") );
        this.tickLastLoaded = GENERAL_CONFIG.getSERVER().getTickCount();

        // Initialize ManagedOreClusterChunk
        IMangedChunkData oreClusterChunk = new ManagedOreClusterChunk();
        IMangedChunkData instance = oreClusterChunk.getStaticInstance(this.level, this.id);
        if (instance == null) {
            oreClusterChunk.deserializeNBT(details.getCompound(oreClusterChunk.getClass().getName()));
            setSubclass(ManagedOreClusterChunk.class, oreClusterChunk);
        } else {
            setSubclass(ManagedOreClusterChunk.class, instance);
        }

        //if id is not null and contains 6, log debug
        if( this.id != null && this.id.contains("6") )
        {
            LoggerBase.logInfo("003005", "Deserializing ManagedChunk: " + tag.toString());
        }
    }
}
