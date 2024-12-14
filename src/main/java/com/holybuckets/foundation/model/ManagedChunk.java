package com.holybuckets.foundation.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.foundation.HolyBucketsUtility;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.event.level.ChunkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    //public static final String NBT_KEY_HEADER = "managedChunk";
    public static final GeneralRealTimeConfig GENERAL_CONFIG = GeneralRealTimeConfig.getInstance();
    public static final HashMap<Class<? extends IMangedChunkData>, IMangedChunkData> MANAGED_SUBCLASSES = new HashMap<>();
    public static final HashMap<LevelAccessor, HashMap<String, ManagedChunk>> LOADED_CHUNKS = new HashMap<>();
    public static final Gson GSON_BUILDER = new GsonBuilder().serializeNulls().create();

    private String id;
    private LevelAccessor level;
    private LevelChunk chunk;
    private int tickWritten;
    private int tickLoaded;
    private boolean isLoaded;
    private final HashMap<Class<? extends IMangedChunkData>, IMangedChunkData> managedChunkData = new HashMap<>();



    /** CONSTRUCTORS **/
    private ManagedChunk() {
        super();
        this.isLoaded = true;
    }

    public ManagedChunk( CompoundTag tag ) {
        super();
        this.deserializeNBT(tag);
    }

    public ManagedChunk(LevelAccessor level, String id, LevelChunk chunk)
    {
        this();
        this.id = id;
        this.level = level;
        this.tickWritten = GENERAL_CONFIG.getSERVER().getTickCount();
        this.tickLoaded = GENERAL_CONFIG.getSERVER().getTickCount();
        this.chunk = chunk;
        //Dont init here, working static maps will never be ready at this time
    }


    /** GETTERS & SETTERS **/
    public IMangedChunkData getSubclass(Class<? extends IMangedChunkData> classObject) {
        return managedChunkData.get(classObject);
    }

    public LevelChunk getChunk() {
        return this.chunk;
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

    public void setChunk(LevelAccessor level, String id) {
        this.chunk = getChunk(level, id);
    }



    private void initSubclassesFromMemory(LevelAccessor level, String chunkId)
    {
        for(Map.Entry<Class<? extends IMangedChunkData>, IMangedChunkData> data : MANAGED_SUBCLASSES.entrySet() ) {
            setSubclass( data.getKey() , data.getValue().getStaticInstance(level, chunkId) );
        }

    }


    /** OVERRIDES **/
    private void init(LevelAccessor level, String id, CompoundTag nbtData) throws InvalidId
    {
        this.id = id;
        this.level = level;
        this.setChunk(level, id);
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
        for(IMangedChunkData data : managedChunkData.values())
        {
            if( !data.getClass().getName().equals(subClass) )
                continue;

            if( !data.isInit(subClass) )
                return false;
            else
                return true;
        }
        return false;
    }

    /**
     * Override, dummy method
     * @param level
     * @param id
     * @return
     */
    @Override
    public IMangedChunkData getStaticInstance(LevelAccessor level, String id) {
        return getManagedChunk(level, id);
    }

    public void handleChunkLoaded() {
        this.isLoaded = true;
        initSubclassesFromMemory(level, id);
    }

    public void handleChunkUnloaded() {
       this.isLoaded = false;
    }


    /** STATIC UTILITY METHODS **/

    public static LevelChunk getChunk(LevelAccessor level, String chunkId)
    {
        ChunkPos p = HolyBucketsUtility.ChunkUtil.getPos(chunkId);
        return level.getChunkSource().getChunkNow(p.x, p.z);
    }

    public static ManagedChunk getManagedChunk(LevelAccessor level, String id) throws NullPointerException
    {
        try {
            return LOADED_CHUNKS.get(level).get(id);
        }
        catch (NullPointerException e) {
            LoggerBase.logError(null, "003003", "Error getting static instance of ManagedChunk with id: " + id);
        }
        return null;
    }


    public static void onChunkLoad( final ChunkEvent.Load event )
    {
        // Implementation for chunk unload
        LevelAccessor level = event.getLevel();
        String chunkId = HolyBucketsUtility.ChunkUtil.getId(event.getChunk());
        LevelChunk levelChunk = getChunk(level, chunkId);

        if(LOADED_CHUNKS.get(level) == null) {
            LOADED_CHUNKS.put(level, new HashMap<>());
        }

        if (levelChunk == null) {
            //LoggerProject.logDebug("002021", "Chunk " + chunkId + " unloaded before data could be written");
        }
        else
        {
            if( LOADED_CHUNKS.get(level).containsKey(chunkId) ) {
                LoggerBase.logDebug(null, "002020", "Chunk " + chunkId + " already loaded");
            } else {
                ManagedChunk managedChunk = new ManagedChunk(level, chunkId, levelChunk);
                LOADED_CHUNKS.get(level).put(chunkId, managedChunk);
            }
            ManagedChunk c = getManagedChunk(level, chunkId);
            c.handleChunkLoaded();
        }

    }

    public static void onChunkUnload( final ChunkEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        LOADED_CHUNKS.get(level).get(HolyBucketsUtility.ChunkUtil.getId(event.getChunk())).handleChunkUnloaded();
    }


    /**
     * Update the block states of a chunk. It is important that it is synchronized to prevent
     * concurrent modifications to the chunk. The block at the given position is updated to the requisite
     * block state.
     * @param chunk
     * @param updates
     * @return true if successful, false if some element was null
     */
    public static synchronized boolean updateChunkBlockStates(LevelChunk chunk, Queue<Pair<BlockState, BlockPos>> updates)
    {
        if( chunk == null || updates == null || updates.size() == 0 )
            return false;

        if( chunk.getStatus() != ChunkStatus.FULL )
            return false;

        //LoggerBase.logDebug(null, "002022", "Updating chunk block states: " + HolyBucketsUtility.ChunkUtil.getId( chunk));

        try
        {
            BlockPos worldPos = chunk.getPos().getWorldPosition();
            Map<Integer, LevelChunkSection> sections = new HashMap<>();

            int i = 0;
            for(LevelChunkSection s : chunk.getSections()) {
                sections.put(i++, s);
            }
            for(Pair<BlockState, BlockPos> update : updates)
            {
                //chunk.setBlockState(update.getRight(), update.getLeft(), false);
                BlockPos bPos = update.getRight();
                //HolyBucketsUtility.WorldPos wPos = new HolyBucketsUtility.WorldPos(bPos, chunk);
                //LevelChunkSection section = sections.get(wPos.getSectionIndex());
                //HolyBucketsUtility.TripleInt t = wPos.getIndices();
                //section.acquire();
                //section.setBlockState(t.x, t.y, t.z, update.getLeft(), true);
                //section.release();

                Level level = chunk.getLevel();
                level.setBlock(bPos, update.getLeft(), 0);
            }
        }
        catch (IllegalStateException e)
        {
            LoggerBase.logWarning(null, "002023", "Illegal state exception " +
             "updating chunk block states. Updates may be replayed later. At Chunk: " + HolyBucketsUtility.ChunkUtil.getId( chunk));
            return false;
        }
        catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error updating chunk block states. At Chunk: ");
            error.append(HolyBucketsUtility.ChunkUtil.getId( chunk));
            error.append("\n");
            error.append("Corresponding exception message: \n");
            error.append(e.getMessage());

            LoggerBase.logError(null, "002024", error.toString());

            return false;
        }
        finally
        {
            //Release all locks
            for(LevelChunkSection s : chunk.getSections()) {
                //s.release();
            }
        }


        return true;
    }

    /**
     * Update the blocks of a chunk. Calls updateChunkBlockStates with the default block state of the block.
     * @param chunk
     * @param updates
     * @return
     */
    public static synchronized boolean updateChunkBlocks(LevelChunk chunk, Queue<Pair<Block, BlockPos>> updates)
    {
        if( chunk == null || updates == null || updates.size() == 0 )
            return false;

        Queue<Pair<BlockState, BlockPos>> blockStates = new LinkedList<>();

        for(Pair<Block, BlockPos> update : updates) {
            blockStates.add( Pair.of(update.getLeft().defaultBlockState(), update.getRight()) );
        }

        return updateChunkBlockStates(chunk, blockStates);
    }


    /** SERIALIZERS **/

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag details = new CompoundTag();

        if( this.id == null || this.level == null ) {
            LoggerBase.logError(null, "003004", "ManagedChunk not initialized with id or level and cannot be serialized");
            return details;
        }


        details.putString("id", this.id);
        details.putInt("level", this.level.hashCode());
        details.putInt("tickWritten", GENERAL_CONFIG.getSERVER().getTickCount());

        this.initSubclassesFromMemory(level, id);

        for(IMangedChunkData data : managedChunkData.values()) {
            if( data == null )
                continue;
            //TO DO: Thread this operation and lock until data object is done with current operation
            //to ensure write is most recent info
            details.put(data.getClass().getName(), data.serializeNBT());
        }

        LoggerBase.logDebug( null,"003002", "Serializing ManagedChunk with data: " + details);

        return details;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        if(tag == null || tag.isEmpty()) {
            return;
        }

        //print tag as string, info
        this.id = tag.getString("id");
        this.level = GENERAL_CONFIG.getLEVELS().get( tag.get("level") );
        this.tickWritten = tag.getInt("tickWritten");
        this.tickLoaded = GENERAL_CONFIG.getSERVER().getTickCount();

        try {
            this.init(level, id, tag );
        } catch (InvalidId e) {
            LoggerBase.logError(null, "002021", "Error initializing ManagedChunk with id: " + id);
        }

    }


}
//END CLASS