package com.holybuckets.orecluster.model;

import com.holybuckets.foundation.Exception.InvalidId;
import com.holybuckets.foundation.HolyBucketsUtility.ChunkUtil;
import com.holybuckets.foundation.modelInterface.IMangedChunkData;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.ChunkPos;
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
    private String id;
    private ChunkAccess chunk;
    private ChunkPos pos;
    private ClusterStatus status;
    private HashMap<String, Vec3i> clusterTypes;
    private List<Pair<String, Vec3i>> clusters;

    //Reference to OreClusterManager's array of ManagedOreClusterChunks
    private static ConcurrentHashMap<String, ManagedOreClusterChunk> loadedChunks =
        OreClustersAndRegenMain.oreClusterManager.getLoadedChunks();


    /** Constructors **/

    //Default constructor - creates dummy node to be loaded from HashMap later
    public ManagedOreClusterChunk()
    {
        super();
        this.chunk = null;
        this.id = null;
        this.pos = null;
        this.status = ClusterStatus.NONE;
        this.clusterTypes = new HashMap<>();
        this.clusters = new LinkedList<>();
    }

    //One for building with chunk
    public ManagedOreClusterChunk(ChunkAccess chunk)
    {
        super();
        this.chunk = chunk;
        this.pos = chunk.getPos();
        this.id = ChunkUtil.getId( this.pos );
        this.status = ClusterStatus.NONE;
        this.clusterTypes = new HashMap<>();
        this.clusters = new LinkedList<>();
    }

    //One for building with id
    public ManagedOreClusterChunk(String id)
     {
        super();
        this.chunk = null;
        this.id = id;
        this.pos = ChunkUtil.getPos( id );
        this.status = ClusterStatus.NONE;
        this.clusterTypes = new HashMap<>();
        this.clusters = new LinkedList<>();
    }


    //Constructor for building from NBT data
    public ManagedOreClusterChunk(ServerLevel pLevel, CompoundTag pCompound)
    {
    /*
        this.raidEvent = new ServerBossEvent(RAID_NAME_COMPONENT, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        this.random = RandomSource.create();
        this.waveSpawnPos = Optional.empty();
        this.level = pLevel;
        this.id = pCompound.getInt("Id");
        this.started = pCompound.getBoolean("Started");
        this.active = pCompound.getBoolean("Active");
        this.ticksActive = pCompound.getLong("TicksActive");
        this.badOmenLevel = pCompound.getInt("BadOmenLevel");
        this.groupsSpawned = pCompound.getInt("GroupsSpawned");
        this.raidCooldownTicks = pCompound.getInt("PreRaidTicks");
        this.postRaidTicks = pCompound.getInt("PostRaidTicks");
        this.totalHealth = pCompound.getFloat("TotalHealth");
        this.center = new BlockPos(pCompound.getInt("CX"), pCompound.getInt("CY"), pCompound.getInt("CZ"));
        this.numGroups = pCompound.getInt("NumGroups");
        this.status = Raid.RaidStatus.getByName(pCompound.getString("Status"));
        this.heroesOfTheVillage.clear();
        if (pCompound.contains("HeroesOfTheVillage", 9)) {
            ListTag listtag = pCompound.getList("HeroesOfTheVillage", 11);

            for(int i = 0; i < listtag.size(); ++i) {
                this.heroesOfTheVillage.add(NbtUtils.loadUUID(listtag.get(i)));
            }
        }
    */
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
     * Checks if the chunk is loaded and makes a deep copy, or initializes itself
     *
     * @param id
     * @throws InvalidId
     */
    @Override
    public void init(String id) throws InvalidId
    {
        ManagedOreClusterChunk chunk = loadedChunks.get(id);

        if(chunk == null)
            throw new InvalidId(null);

        CompoundTag tag = chunk.serializeNBT();
        this.deserializeNBT(tag);
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
        ManagedOreClusterChunk chunk = loadedChunks.get(id);

        if(chunk == null) {
            throw new InvalidId(null);
        }
        return chunk;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        //id may be null
        tag.putString(NBT_KEY_HEADER, this.id);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        //may be null
        this.id = compoundTag.getString(NBT_KEY_HEADER);
    }


}
//END CLASS
