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
    private IMangedChunkData managedOreClusterChunk;


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
    public IMangedChunkData getManagedOreClusterChunk() {
        return managedOreClusterChunk;
    }

    /**
     *
     * @param classObject, managedChunkData
     * @return
     */
    public Boolean setSubclass(Class<IMangedChunkData> classObject, IMangedChunkData managedChunkData) {
        if (classObject == null || managedChunkData == null) {
            return false;
        }
        if (classObject.getName().equals(ManagedOreClusterChunk.class.getName())) {
            this.managedOreClusterChunk = managedChunkData;
            return true;
        }
        return false;
    }


    /** OVERRIDES **/
    @Override
    public void init(LevelAccessor level, String id) throws InvalidId
    {
        this.id = id;
        this.level = level;

        //LoggerBase.logInfo("003000", "Initializing ManagedChunk with id: " + id);
        HashMap<String, String> errors = new HashMap<>();

        IMangedChunkData sub = new ManagedOreClusterChunk();
        this.managedOreClusterChunk = sub.getStaticInstance(level, id);
        if(this.managedOreClusterChunk == null) {
            errors.put(sub.getClass().getName(), "returned null");
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
        boolean isInit = false;
        IMangedChunkData sub = this.managedOreClusterChunk;
        boolean subClassInit =  (sub != null && sub.isInit(subClass));

        if(subClass.equals("ManagedOreClusterChunk")) {
            isInit = subClassInit;
        }

        return isInit;
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
        if(this.managedOreClusterChunk != null) {
            IMangedChunkData sub = this.managedOreClusterChunk;
            details.put(sub.getClass().getName(), sub.serializeNBT());
        }

        if( this.id != null)
            wrapper.put(NBT_KEY_HEADER, details);

        if( this.id != null && this.id.contains("16") )
        {
            LoggerBase.logDebug("003003", "Serializing ManagedChunk " + this.id);
            LoggerBase.logDebug("003004", "This String: " + this.toString());
            LoggerBase.logDebug("003005", "Wrapper String: " + wrapper.toString());
            if(this.managedOreClusterChunk != null)
                LoggerBase.logDebug("003006", "OreCluster String: " + this.managedOreClusterChunk.toString());

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

        //For all subclasses, attempt to pull from RAM, otherwise deserialize
        IMangedChunkData sub = new ManagedOreClusterChunk();
        sub = sub.getStaticInstance(this.level, this.id);
        if (sub == null) {
            sub.deserializeNBT(details.getCompound(sub.getClass().getName()));
        }

        //if id is not null and contains 6, log debug
        if( this.id != null && this.id.contains("6") )
        {
            LoggerBase.logInfo("003005", "Deserializing ManagedChunk: " + tag.toString());
        }
    }
}
