package com.holybuckets.orecluster.model;

import com.google.gson.Gson;
import com.holybuckets.foundation.HolyBucketsUtility;
import com.holybuckets.foundation.HolyBucketsUtility.ChunkUtil;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.orecluster.LoggerProject;
import com.holybuckets.orecluster.core.OreClusterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        GENERATED,
        MANIFESTED
    }

    static {
        ManagedChunk.MANAGED_SUBCLASSES.put(ManagedOreClusterChunk.class, new ManagedOreClusterChunk());
    }

    /** Variables **/
    private LevelAccessor level;
    private String id;
    private LevelChunk chunk;
    private ChunkPos pos;
    private ClusterStatus status;
    private HashMap<Block, BlockPos> clusterTypes;
    private Map<Block, BlockPos[]> originalOres;

    private Queue<Pair<Block, BlockPos>> blockStateUpdates;


    //private List<Pair<String, Vec3i>> clusters;

    /** Constructors **/
    /**
        Dummy Constructor for using getInstance, should only have local scope
     */
    public ManagedOreClusterChunk() {
        super();
    }

    //Default constructor - creates dummy node to be loaded from HashMap later
    private ManagedOreClusterChunk(LevelAccessor level)
    {
        super();
        this.level = level;
        this.chunk = null;
        this.id = null;
        this.pos = null;
        this.status = ClusterStatus.NONE;
        this.clusterTypes = new HashMap<Block, BlockPos>();
        this.blockStateUpdates = new LinkedList<Pair<Block, BlockPos>>();

    }

    //One for building with chunk
    private ManagedOreClusterChunk(LevelAccessor level, LevelChunk chunk)
    {
        this(level);
        this.chunk = chunk;
        this.pos = chunk.getPos();
        this.id = ChunkUtil.getId( this.pos );

        //Use getChgunkSource to get LevelChunk, then use capability to add this reference to the ManagedChunk
        LevelChunk c = level.getChunkSource().getChunkNow( this.pos.x, this.pos.z );
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

    /**
     * Get an instance of the ManagedOreClusterChunk using a loaded chunk
     * @param level
     * @param chunk
     * @return
     */
    public static ManagedOreClusterChunk getInstance(LevelAccessor level, LevelChunk chunk)
    {
        ManagedOreClusterChunk c = getOreClusterChunkByID(level, ChunkUtil.getId( chunk ));
        if( c != null)
            return c;
        return new ManagedOreClusterChunk(level, chunk);
    }

    /**
     * Get an instance of the ManagedOreClusterChunk using an existing id, for a chunk that may not be loaded yet
     * @param level
     * @param id
     * @return
     */
    public static ManagedOreClusterChunk getInstance(LevelAccessor level, String id)
    {
        ManagedOreClusterChunk c = getOreClusterChunkByID(level, id);
        if( c != null)
            return c;

        return new ManagedOreClusterChunk(level, id);
    }

    public static ManagedOreClusterChunk getOreClusterChunkByID(LevelAccessor level, String id) {
        return   OreClusterManager.oreClusterManagers.getOrDefault(level, null).getDeterminedChunks().getOrDefault(id, null);
    }

    /** Getters **/
    public LevelChunk getChunk() {
        return chunk;
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
        return clusterTypes;
    }

    public Map<Block, BlockPos[]> getOriginalOres() {
        return originalOres;
    }

    public boolean hasClusters() {
        return this.clusterTypes.size() > 0;
    }

    public Queue<Pair<Block, BlockPos>> getBlockStateUpdates() {
        return blockStateUpdates;
    }


    /** Setters **/

    public void setChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }

    public void setPos(ChunkPos pos) {
        this.pos = pos;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
    }

    public void setClusters(HashMap<Block, BlockPos> clusters) {
        this.clusterTypes = clusters;
    }

    public void addClusterTypes(HashMap<Block, BlockPos> clusterMap)
    {
        if( clusterMap == null )
            return;

        if( clusterMap.size() == 0 )
            return;

        this.clusterTypes.putAll( clusterMap );
        //LoggerProject.logDebug("003010", "Adding clusterTypes: " + this.clusterTypes);
    }

    public void setOriginalOres(Map<Block, BlockPos[]> originalOres) {
        this.originalOres = originalOres;
    }


    @Override
    public boolean isInit(String subClass) {
        return subClass.equals(ManagedOreClusterChunk.class.getName()) && this.id != null;
    }


    public ManagedOreClusterChunk getStaticInstance(LevelAccessor level, String id)
    {
        if(id == null) { return null; }

        //Reference to OreClusterManager's array of ManagedOreClusterChunks
        OreClusterManager manager = OreClusterManager.oreClusterManagers.getOrDefault(level, null);
        if(manager == null) { return null; }

        ConcurrentHashMap<String, ManagedOreClusterChunk> loadedChunks = manager.getDeterminedChunks();
        if(loadedChunks == null) { return null; }

        ManagedOreClusterChunk chunk = loadedChunks.get(id);
        return chunk;
    }



    @Override
    public CompoundTag serializeNBT()
    {
        //LoggerProject.logDebug("003002", "Serializing ManagedOreClusterChunk");
        CompoundTag wrapper = new CompoundTag();

        CompoundTag details = new CompoundTag();
        details.putString("id", this.id);
        details.putString("status", this.status.toString());

        Gson gson = ManagedChunk.GSON_BUILDER;
        //clusters
        {
            //String clusters = gson.toJson(this.clusters);
            //LoggerProject.logDebug("003005", "Serializing clusters: " + this.clusters);
            //details.putString("clusters", clusters);
        }

        //Cluster Types
        {
            String clusterTypes = gson.toJson(this.clusterTypes);
            //LoggerProject.logDebug("003006", "Serializing clusterTypes: " + this.clusterTypes);
            //LoggerProject.logDebug("003008", "Serializing clusterTypes 2: " + clusterTypes);
            details.putString("clusterTypes", clusterTypes);
        }

        //blockStateUpdates
        {
            StringBuilder blockStateUpdates = new StringBuilder();
            //blockPos andBlockState are private and cant be accesed by gson, do manually
            for(Pair<Block, BlockPos> pair : this.blockStateUpdates)
            {
                String block = HolyBucketsUtility.BlockUtil.blockToString(pair.getLeft());
                BlockPos pos = pair.getRight();
                Vector3d vec = new Vector3d(pos.getX(), pos.getY(), pos.getZ());
                blockStateUpdates.append(block + ":");
                blockStateUpdates.append(gson.toJson(vec));
            }
            LoggerProject.logDebug("003009", "Serializing blockStateUpdates: " + blockStateUpdates);
            details.putString("blockStateUpdates", blockStateUpdates.toString());

        }


        if(this.id != null)
            wrapper.put(NBT_KEY_HEADER, details);


        LoggerProject.logDebug("003007", "Serializing ManagedOreChunk: " + wrapper);

        return wrapper;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag)
    {
        if(compoundTag == null)
            return;

        //LoggerProject.logDebug("003003", "Deserializing ManagedOreClusterChunk");
        CompoundTag wrapper = compoundTag.getCompound(NBT_KEY_HEADER);

        if(wrapper == null)
            return;

        this.id = wrapper.getString("id");
        this.status = ClusterStatus.valueOf( wrapper.getString("status") );

        Gson gson = ManagedChunk.GSON_BUILDER;
        //Clusters
        {
            String clusters = wrapper.getString("clusters");
            LoggerProject.logDebug("003007", "Deserializing clusters: " + clusters);
            //this.clusters = gson.fromJson(clusters, List.class);
        }

        //Cluster Types
        {
            String clusterTypes = wrapper.getString("clusterTypes");
            this.clusterTypes = gson.fromJson(clusterTypes, HashMap.class);
            LoggerProject.logDebug("003008", "Deserializing clusterTypes: " + clusterTypes);
        }

        //Block State Updates
        {
            String blockStateUpdates = wrapper.getString("blockStateUpdates");
            String[] pairs = blockStateUpdates.split(",");
        }


    }


}
//END CLASS
