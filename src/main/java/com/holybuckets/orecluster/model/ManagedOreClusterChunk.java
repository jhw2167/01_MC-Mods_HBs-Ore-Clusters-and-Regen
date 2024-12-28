package com.holybuckets.orecluster.model;

import com.google.gson.Gson;
import com.holybuckets.foundation.GeneralConfig;
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

import com.holybuckets.foundation.HBUtil.*;


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
        GENERATED,
        REGENERATED
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
    public LevelChunk getChunk(boolean forceLoad)
    {
        ManagedChunk parent = ManagedOreClusterChunk.getParent(level, id);
        if(parent == null)
            return null;
        return parent.getChunk(forceLoad);
    }

    public boolean hasChunk() {
        return getChunk(false) != null;
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

    public Random getChunkRandom() {
      ManagedChunk parent = ManagedOreClusterChunk.getParent(level, id);
        if(parent == null)
            return null;

        return parent.getChunkRandom();
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

    public void addClusterTypes(Map<Block, BlockPos> clusterMap)
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

    public static boolean isDetermined(ManagedOreClusterChunk chunk) {
        return chunk.getStatus() == ClusterStatus.DETERMINED;
    }

    public static boolean isCleaned(ManagedOreClusterChunk chunk) {
        return chunk.getStatus() == ClusterStatus.CLEANED;
    }

    public static boolean isPendingGeneration(ManagedOreClusterChunk chunk) {
        return chunk.getStatus() == ClusterStatus.PENDING_GENERATION;
    }

    public static boolean isGenerated(ManagedOreClusterChunk chunk) {
        return chunk.getStatus() == ClusterStatus.GENERATED;
    }

    public static boolean isRegenerated(ManagedOreClusterChunk chunk) {
        return chunk.getStatus() == ClusterStatus.REGENERATED;
    }


    /** SERIALIZERS **/

    @Override
    public CompoundTag serializeNBT()
    {
        //LoggerProject.logDebug("003002", "Serializing ManagedOreClusterChunk");

        CompoundTag details = new CompoundTag();
        details.putString("id", this.id);
        details.putString("status", this.status.toString());

        //Cluster Types
        {
            if(this.clusterTypes == null || this.clusterTypes.size() == 0) {
                details.putString("clusterTypes", "");
            }
            else
            {
                Map<Block, List<BlockPos>> clusters = new HashMap<>();
                this.clusterTypes.keySet().forEach((k) -> clusters.put(k, new ArrayList<>()));
                for(Map.Entry<Block, BlockPos> entry : this.clusterTypes.entrySet())
                {
                    Block block = entry.getKey();
                    BlockPos pos = entry.getValue();
                    if(pos != null)
                        clusters.get(block).add(pos);
                }
                String clusterTypes = BlockUtil.serializeBlockPairs(clusters);
                details.putString("clusterTypes", clusterTypes);
            }
        }




        //blockStateUpdates

        {
            if(this.blockStateUpdates == null || this.blockStateUpdates.size() == 0) {
                details.putString("blockStateUpdates", "");
            }
            else
            {
                Map<Block, List<BlockPos>> blocks = new HashMap<>();
                this.blockStateUpdates.forEach((pair) -> {
                    Block block = pair.getLeft();
                    if(!blocks.containsKey(block))
                        blocks.put(block, new ArrayList<>());
                });

                for(Pair<Block, BlockPos> pair : this.blockStateUpdates)
                {
                    Block block = pair.getLeft();
                    BlockPos pos = pair.getRight();
                    blocks.get(block).add(pos);
                }

                String blockStateUpdates = BlockUtil.serializeBlockPairs(blocks);
                details.putString("blockStateUpdates", blockStateUpdates);
            }
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

        //Cluster Types
        {
            String clusterTypes = tag.getString("clusterTypes");

            this.clusterTypes = null;
            if(clusterTypes == null || clusterTypes.isEmpty()) {
                //add nothing
            }
            else
            {
                Map<Block,List<BlockPos>> clusters =  BlockUtil.deserializeBlockPairs(clusterTypes);
                this.clusterTypes = new HashMap<>();
                for(Map.Entry<Block, List<BlockPos>> entry : clusters.entrySet())
                {
                    Block block = entry.getKey();
                    List<BlockPos> positions = entry.getValue();
                    for(BlockPos pos : positions)
                    {
                        this.clusterTypes.put(block, pos);
                    }

                    if(this.clusterTypes.size() == 0)
                        this.clusterTypes.put(block, null);
                }
            }

            LoggerProject.logDebug("003008", "Deserializing clusterTypes: " + clusterTypes);
        }

        //Block State Updates
        {
            String blockStateUpdates = tag.getString("blockStateUpdates");
            this.blockStateUpdates = new ConcurrentLinkedQueue<>();
            if(blockStateUpdates == null || blockStateUpdates.isEmpty()) {
               //add nothing
            }
            else {
                Map<Block,List<BlockPos>> blocks =  BlockUtil.deserializeBlockPairs(blockStateUpdates);
                for(Map.Entry<Block, List<BlockPos>> entry : blocks.entrySet())
                {
                    Block block = entry.getKey();
                    List<BlockPos> positions = entry.getValue();
                    for(BlockPos pos : positions)
                    {
                        this.blockStateUpdates.add( Pair.of(block, pos) );
                    }
                }
            }



            LoggerProject.logDebug("003009", "Deserializing blockStateUpdates: " + blockStateUpdates);


        }


    }


}
//END CLASS
