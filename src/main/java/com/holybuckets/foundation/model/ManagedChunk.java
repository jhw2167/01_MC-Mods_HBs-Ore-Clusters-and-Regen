package com.holybuckets.foundation.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.datastore.LevelSaveData;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.HBUtil;
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
import net.minecraftforge.event.level.LevelEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.units.qual.C;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ManagedChunk implements IMangedChunkData {

    public static final String CLASS_ID = "003";
    //public static final String NBT_KEY_HEADER = "managedChunk";

    public static final GeneralConfig GENERAL_CONFIG = GeneralConfig.getInstance();
    public static final Map<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> MANAGED_SUBCLASSES = new ConcurrentHashMap<>();
    public static final Map<LevelAccessor, Map<String, ManagedChunk>> LOADED_CHUNKS = new ConcurrentHashMap<>();
    public static final Map<LevelAccessor, Map<String, Object>> INITIALIZED_CHUNKS = new ConcurrentHashMap<>();
    public static final Gson GSON_BUILDER = new GsonBuilder().serializeNulls().create();

    private String id;
    private ChunkPos pos;
    private LevelAccessor level;
    private LevelChunk chunk;
    private int tickWritten;
    private int tickLoaded;
    private boolean isLoaded;
    private final HashMap<Class<? extends IMangedChunkData>, IMangedChunkData> managedChunkData = new HashMap<>();



    /** CONSTRUCTORS **/
    private ManagedChunk() {
        super();
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
        this.pos = HBUtil.ChunkUtil.getPos(id);
        this.level = level;

        this.tickLoaded = GENERAL_CONFIG.getSERVER().getTickCount();
        this.chunk = chunk;
        this.initSubclassesFromMemory(level, id);

        LOADED_CHUNKS.get(this.level).put(this.id, this);
        INITIALIZED_CHUNKS.get(this.level).put(this.id, this);
    }


    /** GETTERS & SETTERS **/
    public IMangedChunkData getSubclass(Class<? extends IMangedChunkData> classObject) {
        return managedChunkData.get(classObject);
    }

    //set chunk to null on chunkUnload
    public LevelChunk getChunk(boolean forceLoad)
    {
        if( this.chunk == null ) {
            this.setChunk(level, id, forceLoad);
        }
        return this.chunk;
    }

    public LevelAccessor getLevel() {
        return this.level;
    }

    public String getId() {
        return this.id;
    }

    /**
     * Get a new instance of random object unique to this chunks coordinates
     * and the Minecraft world seed value
     * @return
     */
    public Random getChunkRandom()
    {
        if( this.id == null || this.pos == null )
            return null;

        final GeneralConfig CONFIG = GeneralConfig.getInstance();
        final Long SEED = CONFIG.getWORLD_SEED();

        Double RAND = HBUtil.ChunkUtil.getChunkRandom(this.pos) *1d;

        if( RAND.equals( 0d ) )
            return new Random( SEED );

        RAND = SEED / RAND;

        if( RAND < 1d && RAND > -1d)
            RAND = 1d / RAND;

        return new Random( RAND.intValue() );
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

    public void setChunk(LevelAccessor level, String id, boolean forceLoad) {
        this.chunk = ManagedChunk.getChunk(level, id, forceLoad);
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
        for(Map.Entry<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> data : MANAGED_SUBCLASSES.entrySet() ) {
            setSubclass( data.getKey() , data.getValue().get().getStaticInstance(level, chunkId) );
        }

    }

    private void initSubclassesFromNbt(CompoundTag tag) throws InvalidId
    {
        //Loop over all subclasses and deserialize if matching chunk not found in RAM
        HashMap<String, String> errors = new HashMap<>();
        for(Map.Entry<Class<? extends IMangedChunkData>, Supplier<IMangedChunkData>> data : MANAGED_SUBCLASSES.entrySet() )
        {
            IMangedChunkData sub = data.getValue().get();
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

        this.level = HBUtil.LevelUtil.toLevel(tag.getString("level"));
        this.tickWritten = tag.getInt("tickWritten");

        /** If tickWritten is < tickLoaded, then this data
         * was written previously and removed from memory. Replace the dummy
         * with serialized data.
         */
         //if( this.tickWritten < this.tickLoaded )
         if( this.isLoaded )
         {
             LoggerBase.logInfo(null, "003007", "Init from nbt id: " + this.id);
             this.initSubclassesFromMemory(level, id);
         }
         else
         {
             LoggerBase.logInfo(null, "003006", "Init from memory id: " + this.id);
             this.initSubclassesFromNbt(tag);
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
        LoggerBase.logInfo(null, "003005", "Loading ManagedChunk with id: " + this.id);

        for(IMangedChunkData data : managedChunkData.values()) {
            data.handleChunkLoaded(event);
        }
        this.isLoaded = true;
    }

    @Override
    public void handleChunkUnloaded(ChunkEvent.Unload event)
    {
        this.chunk = null;
        for(IMangedChunkData data : managedChunkData.values()) {
              data.handleChunkUnloaded(event);
         }
        this.isLoaded = false;
    }


    /** STATIC UTILITY METHODS **/

    /**
     * Get a chunk from a level using a chunk id
     * @param level
     * @param chunkId
     * @return
     */
    public static LevelChunk getChunk(LevelAccessor level, String chunkId, boolean forceLoad)
    {
        ChunkPos p = HBUtil.ChunkUtil.getPos(chunkId);
        return HBUtil.ChunkUtil.getLevelChunk(level, p.x, p.z, forceLoad);
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

    static {
         EventRegistrar reg = EventRegistrar.getInstance();
        reg.registerOnLevelLoad(ManagedChunk::onWorldLoad);
        reg.registerOnLevelUnload(ManagedChunk::onWorldUnload);

        reg.registerOnChunkLoad(ManagedChunk::onChunkLoad);
        reg.registerOnChunkUnload(ManagedChunk::onChunkUnload);
    }

    public static void onWorldLoad( final LevelEvent.Load event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;
        DataStore ds = DataStore.getInstance();
        LevelSaveData levelData = ds.getOrCreateLevelSaveData( HBUtil.NAME, level);


        if(LOADED_CHUNKS.get(level) == null)
        {
            LOADED_CHUNKS.put(level, new ConcurrentHashMap<>());
            INITIALIZED_CHUNKS.put(level,  new ConcurrentHashMap<>());
        }

        JsonElement chunksIds = levelData.get("chunkIds");
        if( chunksIds == null )
        {
            String[] ids = new String[0];
            levelData.addProperty("chunkIds", GSON_BUILDER.toJsonTree(ids));
            chunksIds = levelData.get("chunkIds");
        }

        Map<String, Object> initChunks = INITIALIZED_CHUNKS.get(level);
        chunksIds.getAsJsonArray().forEach( chunkId -> {
            initChunks.put(chunkId.getAsString(), "");
        });

    }

    public static void onWorldUnload( final LevelEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        //Write out initialzed chunks to levelSaveData
        DataStore ds = DataStore.getInstance();
        LevelSaveData levelData = ds.getOrCreateLevelSaveData( HBUtil.NAME, level);

        Map<String, Object> initChunks = INITIALIZED_CHUNKS.get(level);
        String[] chunkIds = initChunks.keySet().toArray(new String[0]);

        levelData.addProperty("chunkIds", GSON_BUILDER.toJsonTree(chunkIds));

        /**
         * Because of the Json tree, or some other reason no ids are written out to datastore
         */

    }

    public static void onChunkLoad( final ChunkEvent.Load event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        String chunkId = HBUtil.ChunkUtil.getId(event.getChunk());
        LevelChunk levelChunk = ManagedChunk.getChunk(level, chunkId, false);

        //Should never be null
        if( levelChunk == null)
         return;

        ManagedChunk loadedChunk = LOADED_CHUNKS.get(level).get(chunkId);
        if(  loadedChunk != null )
        {
            LoggerBase.logDebug(null, "003008.1", "ManagedChunk already loaded with id: " + chunkId);
        }
        else if (INITIALIZED_CHUNKS.get(level).containsKey(chunkId) )
        {
            LoggerBase.logDebug(null, "003008.2", "Skipping for initialized Managed Chunk: " + chunkId);
            //Block until chunk arrives in loaded chunks, timeout after 30s, report error
            int sleepTime = 0;
            while (LOADED_CHUNKS.get(level).get(chunkId) == null)
            {
                try {
                    Thread.sleep(10);
                    sleepTime += 10;
                } catch (InterruptedException e) {
                    LoggerBase.logError(null, "003009", "Error waiting for chunk to load: " + chunkId);
                }

                if(sleepTime > 30000)
                {
                    LoggerBase.logError(null, "003010", "Timeout waiting for chunk to load: " + chunkId);
                    return;
                }

            }
        }
        else
        {
            //brand new chunk, create ManagedChunk
            LoggerBase.logDebug(null, "003008.3", "Creating new managed Chunk: " + chunkId);
            loadedChunk = new ManagedChunk(level, chunkId, levelChunk);
        }

        loadedChunk.handleChunkLoaded(event);


    }

    public static void onChunkUnload( final ChunkEvent.Unload event )
    {
        LevelAccessor level = event.getLevel();
        if(level.isClientSide())
            return;

        ChunkAccess chunk = event.getChunk();
        ManagedChunk c = getManagedChunk(level, HBUtil.ChunkUtil.getId(chunk));
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
            Level level = chunk.getLevel();
            Pair<BlockState, BlockPos> last = null;

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

                //level.setBlock(bPos, update.getLeft(), Block.UPDATE_IMMEDIATE );
                level.setBlock(bPos, update.getLeft(), 0 );
                last = update;
            }

            //level.setBlock(last.getRight(), last.getLeft(), Block.UPDATE_ALL_IMMEDIATE | Block.UPDATE_CLIENTS );
        }
        catch (IllegalStateException e)
        {
            LoggerBase.logWarning(null, "003023", "Illegal state exception " +
             "updating chunk block states. Updates may be replayed later. At Chunk: " + HBUtil.ChunkUtil.getId( chunk));
            return false;
        }
        catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error updating chunk block states. At Chunk: ");
            error.append(HBUtil.ChunkUtil.getId( chunk));
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
        MANAGED_SUBCLASSES.put(classObject, data);
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
            details.putString("level", HBUtil.LevelUtil.toId(this.level)); count++;
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


        }
        catch (Exception e)
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