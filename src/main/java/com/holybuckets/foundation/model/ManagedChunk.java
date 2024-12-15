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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    //public static final String NBT_KEY_HEADER = "managedChunk";
    public static final GeneralRealTimeConfig GENERAL_CONFIG = GeneralRealTimeConfig.getInstance();
    public static final Map<Class<? extends IMangedChunkData>, IMangedChunkData> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    public static final Map<LevelAccessor, Map<String, ManagedChunk>> LOADED_CHUNKS = new ConcurrentHashMap<>();
    public static final Map<LevelAccessor, Map<String, ManagedChunk>> INITIALIZED_CHUNKS = new ConcurrentHashMap<>();
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
        this();
        this.deserializeNBT(tag);
        LOADED_CHUNKS.get(this.level).put(this.id, this);
    }

    public ManagedChunk(LevelAccessor level, String id, LevelChunk chunk)
    {
        this();
        this.id = id;
        this.level = level;

        this.tickLoaded = GENERAL_CONFIG.getSERVER().getTickCount();
        this.chunk = chunk;
        this.initSubclassesFromMemory(level, id);

        if(LOADED_CHUNKS.get(this.level) == null) {
            LOADED_CHUNKS.put(this.level, new ConcurrentHashMap<>());
            INITIALIZED_CHUNKS.put(this.level,  new ConcurrentHashMap<>());
        }
        LOADED_CHUNKS.get(this.level).put(this.id, this);
        INITIALIZED_CHUNKS.get(this.level).put(this.id, this);
    }


    /** GETTERS & SETTERS **/
    public IMangedChunkData getSubclass(Class<? extends IMangedChunkData> classObject) {
        return managedChunkData.get(classObject);
    }

    public LevelChunk getChunk() {
        return this.chunk;
    }

    public String getId() {
        return this.id;
    }

    /**
     * Set a managed chunk data subclass
     * @param classObject The class of the managed chunk data
     * @param data The managed chunk data instance
     * @return true if set successfully
     */
    public Boolean setSubclass(Class<? extends IMangedChunkData> classObject, IMangedChunkData data)
    {
        if (classObject == null || data == null) {
            return false;
        }
        managedChunkData.put(classObject, data);
        return true;
    }

    public void setChunk(LevelAccessor level, String id) {
        this.chunk = ManagedChunk.getChunk(level, id);
    }


    /**
     * Initialize subclasses using getStaticInstance method from each subclass. Which
     * allows each subclass to initialize itself from an existing datastructure owned by
     * a class unknown to Managed Chunk. useSerialize is a boolean, that if set to true
     * the subclass will be skipped since more correct data is from the serialized data.
     * @param level
     * @param chunkId
     */
    private void initSubclassesFromMemory(LevelAccessor level, String chunkId)
    {
        for(Map.Entry<Class<? extends IMangedChunkData>, IMangedChunkData> data : MANAGED_SUBCLASSES.entrySet() ) {
            setSubclass( data.getKey() , data.getValue().getStaticInstance(level, chunkId) );
        }

    }

    private void initSubclassesFromNbt(CompoundTag tag) throws InvalidId
    {
        //Loop over all subclasses and deserialize if matching chunk not found in RAM
        HashMap<String, String> errors = new HashMap<>();
        for(Map.Entry<Class<? extends IMangedChunkData>, IMangedChunkData> data : MANAGED_SUBCLASSES.entrySet() )
        {
            IMangedChunkData sub = data.getValue();
            try {
                sub.deserializeNBT(tag.getCompound(sub.getClass().getName()));
                setSubclass(sub.getClass(), sub);
            } catch (Exception e) {
                errors.put(sub.getClass().getName(), e.getMessage());
            }

        }

        if(!errors.isEmpty())
        {
            //Add all errors in list to error message
            StringBuilder error = new StringBuilder();
            for (String key : errors.keySet()) {
                error.append(key).append(": ").append(errors.get(key)).append("\n");
            }
            throw new InvalidId(error.toString());
        }
    }


    /** OVERRIDES **/
    private void init(CompoundTag tag) throws InvalidId
    {
        //print tag as string, info
        this.id = tag.getString("id");

        this.level = GENERAL_CONFIG.getLEVELS().get( tag.get("level") );
        this.tickWritten = tag.getInt("tickWritten");
        this.setChunk(level, id);

        /** If tickWritten is < tickLoaded, then this data
         * was written previously and removed from memory. Replace the dummy
         * with serialized data.
         */
         if( this.tickWritten < this.tickLoaded )
         {
            LoggerBase.logInfo(null, "003006", "Init from memory id: " + this.id);
             this.initSubclassesFromNbt(tag);
         }
         else
         {
            LoggerBase.logInfo(null, "003007", "Init from nbt id: " + this.id);
             this.initSubclassesFromMemory(level, id);
         }
         
         this.tickLoaded = GENERAL_CONFIG.getSERVER().getTickCount();

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

    @Override
    public void handleChunkLoaded(ChunkEvent.Load event)
    {
        this.isLoaded = true;
        LoggerBase.logInfo(null, "003005", "Loading ManagedChunk with id: " + this.id);

        for(IMangedChunkData data : managedChunkData.values()) {
            data.handleChunkLoaded(event);
        }
    }

    @Override
    public void handleChunkUnloaded(ChunkEvent.Unload event)
    {
       this.isLoaded = false;
       this.tickWritten = GENERAL_CONFIG.getSERVER().getTickCount();

         for(IMangedChunkData data : managedChunkData.values()) {
              data.handleChunkUnloaded(event);
         }
    }


    /** STATIC UTILITY METHODS **/

    /**
     * Get a chunk from a level using a chunk id
     * @param level
     * @param chunkId
     * @return
     */
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
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        String chunkId = HolyBucketsUtility.ChunkUtil.getId(event.getChunk());
        LevelChunk levelChunk = ManagedChunk.getChunk(level, chunkId);

        if(LOADED_CHUNKS.get(level) == null) {
            LOADED_CHUNKS.put(level, new ConcurrentHashMap<>());
            INITIALIZED_CHUNKS.put(level,  new ConcurrentHashMap<>());
        }

        if (levelChunk == null) {
            //LoggerProject.logDebug("003021", "Chunk " + chunkId + " unloaded before data could be written");
        }
        else
        {

            ManagedChunk c = LOADED_CHUNKS.get(level).get(chunkId);
            if(  c == null ) {
                c = new ManagedChunk(level, chunkId, levelChunk);
            }

            c.handleChunkLoaded(event);
        }

    }

    public static void onChunkUnload( final ChunkEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        ChunkAccess chunk = event.getChunk();
        ManagedChunk c = getManagedChunk(level, HolyBucketsUtility.ChunkUtil.getId(chunk));
        c.handleChunkUnloaded(event);
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

        //LoggerBase.logDebug(null, "003022", "Updating chunk block states: " + HolyBucketsUtility.ChunkUtil.getId( chunk));

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
            LoggerBase.logWarning(null, "003023", "Illegal state exception " +
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

            LoggerBase.logError(null, "003024", error.toString());

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
        Queue<Pair<BlockState, BlockPos>> blockStates = new LinkedList<>();
        for(Pair<Block, BlockPos> update : updates) {
            blockStates.add( Pair.of(update.getLeft().defaultBlockState(), update.getRight()) );
        }

        return updateChunkBlockStates(chunk, blockStates);
    }

    public static void registerManagedChunkData(Class<? extends IMangedChunkData> classObject, Supplier<IMangedChunkData> data)
    {
        MANAGED_SUBCLASSES.put(classObject, data.get());
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

        int count = 0;
        try {

            details.putString("id", this.id); count++;
            details.putInt("level", this.level.hashCode()); count++;
            this.tickWritten = GENERAL_CONFIG.getSERVER().getTickCount(); count++;
            details.putInt("tickWritten", this.tickWritten); count++;

            for(IMangedChunkData data : managedChunkData.values())
            {
                if( data == null )
                    continue;
                //TO DO: Thread this operation and lock until data object is done with current operation
                //to ensure write is most recent info
                details.put(data.getClass().getName(), data.serializeNBT());
                count++;
            }


        } catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error serializing ManagedChunk with id: ");
            error.append(this.id);
            error.append("\nError: ");
            error.append(e.getClass());
            error.append(" - ");
            error.append(e.getMessage());
            error.append("\nCount: ");
            error.append(count);

            LoggerBase.logError(null, "003020", error.toString());
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

        LoggerBase.logInfo(null, "003001", "Deserializing ManagedChunk with id: " + this.id);

        //Deserialize subclasses
        try {
            this.init(tag);
        } catch (InvalidId e) {
            LoggerBase.logError(null, "003021", "Error initializing ManagedChunk with id: " + id);
        }

    }


}
//END CLASS