package com.holybuckets.orecluster.model;

import com.google.gson.Gson;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.HBUtil.ChunkUtil;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.orecluster.LoggerProject;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.core.OreClusterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Class: ManagedChunk
 * Description: Dedicating a class to hold the many manged states of a chunk preparing for cluster generation
 *
 *  #Variables
 *  - LevelChunk chunk: The chunk object
 *  - ChunkPos pos: The 2D position of the chunk in the world
 *  - String id: The unique id of the chunk
 *  - String status: The status of the chunk in the cluster generation process
 *
 *  - HashMap<String, Vec3i> clusters: The clusters in the chunk
 *  - isLoaded: The chunk is loaded
 *
 *  #Methods
 *  - Getters and Setters
 *  - save: Save the data as NBT using compoundTag
 *
 */

public class ManagedOreClusterChunk implements IMangedChunkData {

    private static final String CLASS_ID = "003";
    private static final String NBT_KEY_HEADER = "managedOreClusterChunk";

    public static enum ClusterStatus {
        NONE,
        DETERMINED,
        CLEANED,
        PENDING_GENERATION,
        GENERATED
    }

    public static void registerManagedChunkData() {
        ManagedChunk.registerManagedChunkData(ManagedOreClusterChunk.class, () -> new ManagedOreClusterChunk(null) );
    }

    /** Variables **/
    private LevelAccessor level;
    private String id;
    private ChunkPos pos;
    private ClusterStatus status;

    private HashMap<Block, BlockPos> clusterTypes;
    private Map<Block, HBUtil.Fast3DArray> originalOres;
    private ConcurrentLinkedQueue<Pair<Block, BlockPos>> blockStateUpdates;

    private ReentrantLock lock = new ReentrantLock();

    //private List<Pair<String, Vec3i>> clusters;

    /** Constructors **/

    //Default constructor - creates dummy node to be loaded from HashMap later
    private ManagedOreClusterChunk(LevelAccessor level)
    {
        super();
        this.level = level;
        this.id = null;
        this.pos = null;
        this.status = ClusterStatus.NONE;
        this.clusterTypes = null;
        this.blockStateUpdates = new ConcurrentLinkedQueue<>();

    }

    //One for building with chunk
    private ManagedOreClusterChunk(LevelAccessor level, LevelChunk chunk)
    {
        this(level);
        this.pos = chunk.getPos();
        this.id = ChunkUtil.getId( this.pos );

        //Use getChgunkSource to get LevelChunk, then use capability to add this reference to the ManagedChunk
        LevelChunk c = chunk;
        c.getCapability(ManagedChunkCapabilityProvider.MANAGED_CHUNK).ifPresent(cap -> {
            cap.setSubclass(ManagedOreClusterChunk.class, this);
        });
    }

    //One for building with id
    private ManagedOreClusterChunk(LevelAccessor level, String id)
     {
        this(level);
        this.id = id;
        this.pos = ChunkUtil.getPos( id );
    }



    /** Getters **/
    public LevelChunk getChunk()
    {
        ManagedChunk parent = ManagedOreClusterChunk.getParent(level, id);
        if(parent == null)
            return null;
        return parent.getChunk();
    }

    public boolean hasChunk() {
        return getChunk() != null;
    }

    public ChunkPos getPos() {
        return pos;
    }

    public String getId() {
        return id;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    @NotNull
    public HashMap<Block, BlockPos> getClusterTypes() {
    if(this.clusterTypes == null)
        return new HashMap<>();
        return clusterTypes;
    }

    public boolean hasClusters() {
        if(this.clusterTypes == null)
            return false;
        return this.clusterTypes.size() > 0;
    }

    public Queue<Pair<Block, BlockPos>> getBlockStateUpdates() {
        return blockStateUpdates;
    }

    public Map<Block, HBUtil.Fast3DArray> getOriginalOres() {
        return originalOres;
    }

    /**
     * Multiplies the Object's hashcode by the provided seed to get a random number
     * to this chunk
     * @return
     */
    public Random getChunkRandom() {
        final GeneralConfig CONFIG = GeneralConfig.getInstance();
        return new Random( this.hashCode() * CONFIG.getWORLD_SEED() );
    }

    public synchronized ReentrantLock getLock() {
        return lock;
    }

    /** Setters **/

    public void setPos(ChunkPos pos) {
        this.pos = pos;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
    }


    public void setOriginalOres(Map<Block, HBUtil.Fast3DArray> originalOres) {
        this.originalOres = originalOres;
    }

    public void addClusterTypes(HashMap<Block, BlockPos> clusterMap)
    {
        if( clusterMap == null )
            return;

        if( clusterMap.size() == 0 )
            return;

        if( this.clusterTypes == null )
            this.clusterTypes = new HashMap<>();

        this.clusterTypes.putAll( clusterMap );
        //LoggerProject.logDebug("003010", "Adding clusterTypes: " + this.clusterTypes);
    }

    public void addBlockStateUpdate(Block block, BlockPos pos) {
        this.blockStateUpdates.add( Pair.of(block, pos) );
    }


    public ManagedOreClusterChunk getStaticInstance(LevelAccessor level, String id)
    {
        if(id == null || level == null )
         return null;

        OreClusterManager manager = OreClustersAndRegenMain.ORE_CLUSTER_MANAGER_BY_LEVEL.get(level);
        if(manager != null)
        {
            if(manager.getLoadedChunk(id) != null)
                return manager.getLoadedChunk(id);
        }

        ManagedOreClusterChunk chunk = ManagedOreClusterChunk.getInstance(level, id);
        return chunk;
    }

    @Override
    public boolean isInit(String subClass) {
        return subClass.equals(ManagedOreClusterChunk.class.getName()) && this.id != null;
    }

    @Override
    public void handleChunkLoaded(ChunkEvent.Load event) {
        this.level = event.getLevel();
        OreClusterManager.onChunkLoad(event, this);
    }

    @Override
    public void handleChunkUnloaded(ChunkEvent.Unload event)
    {
        OreClusterManager.onChunkUnload(event);
    }

    /** STATIC METHODS **/

    /**
     * Get an instance of the ManagedOreClusterChunk using a loaded chunk
     * @param level
     * @param chunk
     * @return
     */
    public static ManagedOreClusterChunk getInstance(LevelAccessor level, LevelChunk chunk) {
        return ManagedOreClusterChunk.getInstance(level, ChunkUtil.getId( chunk ));
    }

    /**
     * Get an instance of the ManagedOreClusterChunk using an existing id, for a chunk that may not be loaded yet
     * @param level
     * @param id
     * @return
     */
    public static ManagedOreClusterChunk getInstance(LevelAccessor level, String id)
    {

        ManagedChunk parent = getParent(level, id);
        if(parent == null)
            return new ManagedOreClusterChunk(level, id);

        ManagedOreClusterChunk c = (ManagedOreClusterChunk) parent.getSubclass(ManagedOreClusterChunk.class);
        if( c == null)
            return new ManagedOreClusterChunk(level, id);

        return c;
    }

    public static ManagedChunk getParent(LevelAccessor level, String id) {
        return ManagedChunk.getManagedChunk(level, id);
    }


    /** SERIALIZERS **/

    @Override
    public CompoundTag serializeNBT()
    {
        //LoggerProject.logDebug("003002", "Serializing ManagedOreClusterChunk");

        CompoundTag details = new CompoundTag();
        details.putString("id", this.id);
        details.putString("status", this.status.toString());

        Gson gson = ManagedChunk.GSON_BUILDER;

        //Cluster Types
        if(this.clusterTypes == null || this.clusterTypes.size() == 0) {
            details.putString("clusterTypes", "{}");
        }
        else
        {
            String clusterTypes = gson.toJson(this.clusterTypes);
            //LoggerProject.logDebug("003006", "Serializing clusterTypes: " + this.clusterTypes);
            //LoggerProject.logDebug("003008", "Serializing clusterTypes 2: " + clusterTypes);
            details.putString("clusterTypes", clusterTypes);
        }

        //blockStateUpdates
        if(this.blockStateUpdates == null || this.blockStateUpdates.size() == 0) {
            details.putString("blockStateUpdates", "");
        }
        else
        {
            StringBuilder blockStateUpdates = new StringBuilder();
            for(Pair<Block, BlockPos> pair : this.blockStateUpdates)
            {
                blockStateUpdates.append("{");
                String block = HBUtil.BlockUtil.blockToString(pair.getLeft());
                blockStateUpdates.append(block);
                blockStateUpdates.append("=");

                BlockPos pos = pair.getRight();
                HBUtil.TripleInt vec = new HBUtil.TripleInt(pos);
                blockStateUpdates.append("[" + vec.x + "," + vec.y + "," + vec.z + "]");
                blockStateUpdates.append("}, ");
            }
            //remove trailing comma and space
            blockStateUpdates.deleteCharAt(blockStateUpdates.length() - 1);
            blockStateUpdates.deleteCharAt(blockStateUpdates.length() - 1);

            LoggerProject.logDebug("003009", "Serializing blockStateUpdates: " + blockStateUpdates);
            details.putString("blockStateUpdates", blockStateUpdates.toString());
        }

        LoggerProject.logDebug("003007", "Serializing ManagedOreChunk: " + details);

        return details;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        LoggerProject.logDebug("003003", "Deserializing ManagedOreClusterChunk");
        if(tag == null || tag.isEmpty())
            return;

        this.id = tag.getString("id");
        this.status = ClusterStatus.valueOf( tag.getString("status") );

        Gson gson = ManagedChunk.GSON_BUILDER;

        //Cluster Types
        {
            String clusterTypes = tag.getString("clusterTypes");
            this.clusterTypes = gson.fromJson(clusterTypes, HashMap.class);

            if(this.clusterTypes != null && this.clusterTypes.size() == 0 )
                this.clusterTypes = null;
            LoggerProject.logDebug("003008", "Deserializing clusterTypes: " + clusterTypes);
        }

        //Block State Updates
        {
            String blockStateUpdates = tag.getString("blockStateUpdates");
            /**
             * Rfor(Pair<Block, BlockPos> pair : this.blockStateUpdates)
             *             {
             *                 blockStateUpdates.append("{");
             *                 String block = HolyBucketsUtility.BlockUtil.blockToString(pair.getLeft());
             *                 blockStateUpdates.append(block);
             *                 blockStateUpdates.append(":");
             *
             *                 BlockPos pos = pair.getRight();
             *                 HolyBucketsUtility.TripleInt vec = new HolyBucketsUtility.TripleInt(pos);
             *                 blockStateUpdates.append("[" + vec.x + "-" + vec.y + "-" + vec.z + "]");
             *                 blockStateUpdates.append("},");
             *             }
             */
             String[] pairs = blockStateUpdates.split(", ");
             for (String pair : pairs)
             {
                //remove curly braces
                 pair = pair.substring(1, pair.length() - 1);

                 String[] parts = pair.split("=");
                 String block = parts[0];
                 Block blockType = HBUtil.BlockUtil.blockNameToBlock(block);

                 String[] pos = parts[1].replace("[", "").replace("]","").split(",");
                 int x = Integer.parseInt(pos[0]);
                 int y = Integer.parseInt(pos[1]);
                 int z = Integer.parseInt(pos[2]);
                 BlockPos blockPos = new BlockPos(x, y, z);

                 this.blockStateUpdates.add(Pair.of(blockType, blockPos));
             }



            LoggerProject.logDebug("003009", "Deserializing blockStateUpdates: " + blockStateUpdates);


        }


    }


}
//END CLASS
