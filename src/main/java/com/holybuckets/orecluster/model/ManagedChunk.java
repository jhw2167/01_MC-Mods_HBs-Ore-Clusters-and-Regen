package com.holybuckets.orecluster.model;

import com.holybuckets.foundation.HolyBucketsUtility.ChunkUtil;
import com.holybuckets.orecluster.modelinterface.IMangedChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

public class ManagedChunk implements IMangedChunk {

    private static final String NBT_KEY_HEADER = "managedChunk";

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        //Needs to be reworked to put a tag of data
        tag.putString(NBT_KEY_HEADER, this.id);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag compoundTag) {
        this.id = compoundTag.getString(NBT_KEY_HEADER);
    }

    public static enum ClusterStatus {
        NONE,
        EXPLORED,
        CLEANED,
        GENERATED,
        MANIFESTED
    }

    /** Variables **/
    private ChunkAccess chunk;
    private ChunkPos pos;
    private String id;
    private ClusterStatus status;
    private HashMap<String, Vec3i> clusterTypes;
    private List<Pair<String, Vec3i>> clusters;



    /** Constructors **/

    //One for building with chunk
    public ManagedChunk(ChunkAccess chunk)
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
    public ManagedChunk(String id)
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
    public ManagedChunk(ServerLevel pLevel, CompoundTag pCompound)
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



    //@Override
    public CompoundTag save(CompoundTag pCompound)
    {
        /*
        pCompound.putInt("Id", this.id);
        pCompound.putBoolean("Started", this.started);
        pCompound.putBoolean("Active", this.active);
        pCompound.putLong("TicksActive", this.ticksActive);
        pCompound.putInt("BadOmenLevel", this.badOmenLevel);
        pCompound.putInt("GroupsSpawned", this.groupsSpawned);
        pCompound.putInt("PreRaidTicks", this.raidCooldownTicks);
        pCompound.putInt("PostRaidTicks", this.postRaidTicks);
        pCompound.putFloat("TotalHealth", this.totalHealth);
        pCompound.putInt("NumGroups", this.numGroups);
        pCompound.putString("Status", this.status.getName());
        pCompound.putInt("CX", this.center.getX());
        pCompound.putInt("CY", this.center.getY());
        pCompound.putInt("CZ", this.center.getZ());
        ListTag listtag = new ListTag();
        Iterator var3 = this.heroesOfTheVillage.iterator();

        while(var3.hasNext()) {
            UUID uuid = (UUID)var3.next();
            listtag.add(NbtUtils.createUUID(uuid));
        }

        pCompound.put("HeroesOfTheVillage", listtag);

        */
        return pCompound;
    }


}
//END CLASS
