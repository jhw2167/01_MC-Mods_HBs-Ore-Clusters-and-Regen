package com.holybuckets.orecluster.model;

import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.HolyBucketsUtility.ChunkUtil;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.core.OreClusterManager;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class: ManagedChunk
 * Description: Dedicating a class to hold the many manged states of a chunk preparing for cluster generation
 *
 *  #Variables
 *  - ChunkAccess chunk: The chunk object
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

    private static final String NBT_KEY_HEADER = "managedChunk";

    public static enum ClusterStatus {
        NONE,
        EXPLORED,
        CLEANED,
        GENERATED,
        MANIFESTED
    }

    /** Variables **/
    private LevelAccessor level;
    private String id;
    private ChunkAccess chunk;
    private ChunkPos pos;
    private ClusterStatus status;
    private HashMap<String, Vec3i> clusterTypes;
    private List<Pair<String, Vec3i>> clusters;


    /** Constructors **/

    //Default constructor - creates dummy node to be loaded from HashMap later
    public ManagedOreClusterChunk(LevelAccessor level)
    {
        super();
        this.level = level;
        this.chunk = null;
        this.id = null;
        this.pos = null;
        this.status = ClusterStatus.NONE;
        this.clusterTypes = new HashMap<>();
        this.clusters = new LinkedList<>();
    }

    //One for building with chunk
    public ManagedOreClusterChunk(LevelAccessor level, ChunkAccess chunk)
    {
        this(level);
        this.chunk = chunk;
        this.pos = chunk.getPos();
        this.id = ChunkUtil.getId( this.pos );
    }

    //One for building with id
    public ManagedOreClusterChunk(LevelAccessor level, String id)
     {
        this(level);
        this.id = id;
        this.pos = ChunkUtil.getPos( id );
    }


    /** Getters **/
    public ChunkAccess getChunk() {
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

    public List<Pair<String, Vec3i>> getClusters() {
        return clusters;
    }

    public boolean hasClusters() {
        return clusters.size() > 0;
    }


    public HashMap<String, Vec3i> getClusterTypes() {
        return clusterTypes;
    }


    /** Setters **/

    public void setChunk(ChunkAccess chunk) {
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

    public void setClusters(HashMap<String, Vec3i> clusters) {
        this.clusterTypes = clusters;
    }


    public void addClusterTypes(HashMap<String, Vec3i> clusterMap)
    {
        if( clusterMap == null )
            return;

        if( clusterMap.size() == 0 )
            return;

        this.clusterTypes.putAll( clusterMap );
    }

    /**
     * Checks if the chunk is loaded and makes a deep copy, or thows exception
     *
     * @param id
     * @throws InvalidId
     */
    @Override
    public void init(String id) throws InvalidId
    {
        ManagedOreClusterChunk chunk = this.getInstance(id);
        CompoundTag tag = chunk.serializeNBT();
        this.deserializeNBT(tag);
    }

    @Override
    public boolean isInit(String subClass) {
        return this.id != null;
    }

    /**
     * Gets the instance of the chunk from the loadedChunks HashMap
     * @param id
     * @return
     * @throws InvalidId
     */
    @Override
    public ManagedOreClusterChunk getInstance(String id) throws InvalidId
    {
        if(id == null) {
            throw new InvalidId(null);
        }
        //Reference to OreClusterManager's array of ManagedOreClusterChunks
        OreClusterManager manager = OreClustersAndRegenMain.oreClusterManagers.get( this.level );
        ConcurrentHashMap<String, ManagedOreClusterChunk> loadedChunks = manager.getLoadedChunks();
        ManagedOreClusterChunk chunk = loadedChunks.get(id);

        if(chunk == null) {
            throw new InvalidId(null);
        }
        return chunk;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_KEY_HEADER, this.id);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        this.id = compoundTag.getString(NBT_KEY_HEADER);
    }


}
//END CLASS
