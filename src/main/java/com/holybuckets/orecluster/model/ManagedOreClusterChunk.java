package com.holybuckets.orecluster.model;

import com.google.gson.Gson;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.HolyBucketsUtility.ChunkUtil;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.foundation.modelInterface.IMangedChunkManager;
import com.holybuckets.orecluster.core.OreClusterManager;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.tuple.Pair;

import java.security.Provider;
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

    private static final String NBT_KEY_HEADER = "managedOreClusterChunk";

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
        this.clusterTypes = new HashMap<>();
        this.clusters = new LinkedList<>();
    }

    //One for building with chunk
    private ManagedOreClusterChunk(LevelAccessor level, ChunkAccess chunk)
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
    public static ManagedOreClusterChunk getInstance(LevelAccessor level, ChunkAccess chunk) {
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
        return new ManagedOreClusterChunk(level, id);
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


    @Override
    public boolean isInit(String subClass) {
        return this.id != null;
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

        CompoundTag clustersWrapper = new CompoundTag();
        {
            Gson gson = new Gson();
            CompoundTag clusters = new CompoundTag();
            clustersWrapper.putString("clusters", gson.toJson(this.clusters));
            /*
            for(Pair<String, Vec3i> cluster : this.clusters)
            {
                StringBuilder clusterData = new StringBuilder();
                clusterData.append(cluster.getLeft());
                clusterData.append(" ");

                clusters.putInt(cluster.getLeft(), cluster.getRight().hashCode());
            }
            */

        }
        details.put("clusters", clustersWrapper);

        CompoundTag clusterTypes = new CompoundTag();
        {
            Gson gson = new Gson();
            clusterTypes.putString("clusterTypes", gson.toJson(this.clusterTypes));

        }
        details.put("clusterTypes", clusterTypes);

        //details.put("clusters", OreClustersAndRegenMain.serializeClusters(this.clusters));
        //details.put("clusterTypes", OreClustersAndRegenMain.serializeClusterTypes(this.clusterTypes));

        if(this.id != null)
            wrapper.put(NBT_KEY_HEADER, details);

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

    }


}
//END CLASS
