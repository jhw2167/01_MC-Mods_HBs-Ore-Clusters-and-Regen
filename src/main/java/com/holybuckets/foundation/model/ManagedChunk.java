package com.holybuckets.foundation.model;

import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.foundation.HolyBucketsUtility;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.foundation.modelInterface.IMangedChunkManager;
import com.holybuckets.orecluster.LoggerProject;
import com.holybuckets.orecluster.core.OreClusterManager;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;

import java.util.HashMap;
import java.util.Map;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    public static final String NBT_KEY_HEADER = "managedChunk";
    public static final GeneralRealTimeConfig GENERAL_CONFIG = GeneralRealTimeConfig.getInstance();
    public static final HashMap<Integer, ManagedChunk> MANAGED_CHUNKS = new HashMap<>();
    public static final HashMap<Integer, IMangedChunkData> MANAGED_SUBCLASSES = new HashMap<>();

    private String id;
    private LevelAccessor level;
    private ChunkAccess chunk;
    private int tickLastLoaded;
    private HashMap<Class<? extends IMangedChunkData>, IMangedChunkData> managedChunkData = new HashMap<>();


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
        return managedChunkData.get(classObject);
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
        managedChunkData.put(classObject, data);
        return true;
    }

    private void initSubclassesFromMemory(LevelAccessor level, String chunkId)
    {
        for(IMangedChunkData data : MANAGED_SUBCLASSES.values()) {
            setSubclass( data.getClass(), data.getStaticInstance(level, chunkId));
        }

    }


    /** OVERRIDES **/
    private void init(LevelAccessor level, String id, CompoundTag nbtData) throws InvalidId
    {
        this.id = id;
        this.level = level;
        HashMap<String, String> errors = new HashMap<>();

        //Attempt to link existing subclasses from RAM
        this.initSubclassesFromMemory(level, id);

        //Loop over all subclasses and deserialize if matching chunk not found in RAM
        for(Map.Entry entry : managedChunkData.entrySet())
        {
            if (entry.getValue() != null)
                continue;

            try {
                IMangedChunkData sub = (IMangedChunkData) entry.getKey().getClass().newInstance();
                sub.deserializeNBT(nbtData.getCompound(sub.getClass().getName()));
                setSubclass(sub.getClass(), sub);

            } catch (Exception e) {
                errors.put(entry.getKey().getClass().getName(), e.getMessage());
            }

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

        if( this.id == null || this.level == null )
            return wrapper;

        details.putString("id", this.id);
        details.putInt("level", this.level.hashCode());
        for(IMangedChunkData data : managedChunkData.values()) {
            details.put(data.getClass().getName(), data.serializeNBT());
        }

        if( this.id != null)
            wrapper.put(NBT_KEY_HEADER, details);

        LoggerBase.logDebug("003002", "Serializing ManagedChunk with data: " + wrapper.toString());

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

        try {
            this.init(level, id, details );
        } catch (InvalidId e) {
            LoggerProject.logError("002021", "Error initializing ManagedChunk with id: " + id);
        }

    }

    /** STATIC UTILITY METHODS **/

    public static HashMap<String, Integer> isLinked = new HashMap<>();
    public static void onChunkLoad( final ChunkEvent.Load event )
    {
        // Implementation for chunk unload
        LevelAccessor level = event.getLevel();
        ChunkAccess chunk = event.getChunk();
        String chunkId = HolyBucketsUtility.ChunkUtil.getId(event.getChunk());
        //LevelChunk levelChunk = level.getChunkSource().getChunk(chunk.getPos().x, chunk.getPos().z, false);
        LevelChunk levelChunk = level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z);

        if (levelChunk == null)
        {
            //LoggerProject.logDebug("002021", "Chunk " + chunkId + " unloaded before data could be written");
        }
        else
        {
            levelChunk.getCapability(ManagedChunkCapabilityProvider.MANAGED_CHUNK).ifPresent(c -> {
                    c.initSubclassesFromMemory(level, chunkId);
            });
        }

        //loadedChunks.remove(chunkId);

    }


}
//END CLASS