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

        //Loop over all subclasses and deserialize
        for(Map.Entry entry : managedChunkData.entrySet())
        {
            try {
                IMangedChunkData sub = (IMangedChunkData) entry.getKey().getClass().newInstance();
                IMangedChunkData instance = sub.getStaticInstance(this.level, this.id);

                if (instance == null)
                {
                    sub.deserializeNBT(details.getCompound(instance.getClass().getName()));
                    setSubclass(ManagedOreClusterChunk.class, sub);
                }
                else
                {
                    setSubclass(ManagedOreClusterChunk.class, instance);
                }

            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

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
                try{
                    c.init(level, chunkId);
                } catch (InvalidId e) {
                    if( isLinked.get(chunkId) == null ) {
                        isLinked.put(chunkId, 1);
                    }
                    else {
                        Integer times = isLinked.get(chunkId) + 1;
                        isLinked.put(chunkId, times);
                        LoggerProject.logError("002021", "Error initializing ManagedChunk with id: " + chunkId + " times " + times);
                    }
                }
            });

        }

        //loadedChunks.remove(chunkId);

    }


}
//END CLASS