package com.holybuckets.orecluster;

import com.holybuckets.foundation.HolyBucketsUtility.*;
import com.holybuckets.foundation.LoggerBase;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

//Java Imports

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Class: OreClusterManager
 *
 * Description: This class will manage all ore clusters that exist in the instance
 *  - Determines which chunks clusters will appear in
 *  - Determines the type of cluster that will appear
 *  - All variables and methods are static
 *
 *  - Variables
 *  ConcurrentHashMap called existingClusters that maps Chunk Id to Pair<String, Vector3> where the String is the type of cluster and the Vector3 is the location of the cluster
 *  Float maxDistance which maintains the current maximum distance between the worldSpawn and the furthest cluster
 *  String lastDeterminedClusterChunkId which maintains the last chunk id that was processed in the batch process
 *  ConcurrentQueue that holds Chunk ids of all newly loaded chunks to produce generate for
 *  Static RealTimeConfig config object that holds the default and custom cluster configurations
 *
 *  - Behavior:
 *  - Constructor: Initializes variables to empty formats
 *  - init: Reads  chunk NBT data of all existing chunks and loads them into the existingClusters map if the chunk owns a cluster
 *  - determineClusterLocations: Batch process that takes an integer n and processes either n chunks or all chunks until maxDistance of the worldSpawn
 *      1. The next chunk up processes in a spiral pattern from the lastDeterminedClusterChunkId
 *      2. OreClusterCalculator.calculateClusterLocations() determines which chunk ids will have clusters, processing CHUNK_NORMALIZATION_TOTAL chunks at a time
 *  - onChunkLoad (or the appropriate name): subscribe to the chunk load event and add the chunk id to the queue
 *      1. Check distance between worldSpawn and the chunk
 *      2. If the distance is greater than maxDistance, call determineClusterLocations
 **/


public class OreClusterManager {

    /** Varialbes **/
    public static RealTimeConfig config;
    public static final Random RANDOM = new Random( RealTimeConfig.CLUSTER_SEED );

    public static final ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters = new ConcurrentHashMap<>();
    public static final OreClusterCalculator oreClusterCalculator = new OreClusterCalculator(existingClusters);
    public static final ConcurrentLinkedQueue<String> chunksPendingClusterGen = new ConcurrentLinkedQueue<>();

    public static final HashSet<String> exploredChunks = new HashSet<>();
    public static final ChunkGenerationOrderHandler mainSpiral = new ChunkGenerationOrderHandler(null);


    /** Constructor **/
    public OreClusterManager() {
        init();
    }

    /** Behavior **/
    public static void init() {

        //1. Starting at worldspawn, read chunk NBT data of all existing chunks to determine if they own a cluster
        //2. If the chunk owns a cluster, add it to the existingClusters map

        Vec3i worldSpawn = RealTimeConfig.WORLD_SPAWN;

    }

    /**
     * Handle newly loaded chunk
     *
     *  1. If this chunkId exists in existingClusters, check regen
     *  2. If the chunkId exists in exploredChunks, ignore
     *  3. If the chunkId does not exist in exploredChunks, queue a batch
     *
     */
    public static void handleClustersForChunk( ChunkAccess c )
    {
        String chunkId = ChunkUtil.getId(c);
        if( existingClusters.containsKey(chunkId) )
        {
            LoggerBase.logDebug("Chunk " + chunkId + " contains a cluster");
            //Check regen
        }
        else if( exploredChunks.contains(chunkId) )
        {
            LoggerBase.logDebug("Chunk " + chunkId + " has already been explored");
        }
        else
        {
            LoggerBase.logDebug("Chunk " + chunkId + " has not been explored");
            handlePrepareNewChunksForClusters(config.ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL, c);
        }

    }

    /**
     * Batch process that determines the location of clusters in the next n chunks
     * @param batchSize
     * @param start
     */

    private static void handlePrepareNewChunksForClusters(int batchSize, ChunkAccess start)
    {
        //1. Determine the location of clusters in the next n chunks
        HashSet<String> chunkIds = getBatchedChunkList(batchSize, start);
        LoggerBase.logDebug("Queued " + chunkIds.size() + " chunks for cluster determination");

        //2. Produce array of chunkAccess objects from chunkIds
        List<ChunkAccess> chunks = chunkIds.stream().map( (id) -> getChunkAccess(id) )
        .collect(Collectors.toList());

        //3. Determine the clusters locations
        HashMap<String, HashMap<String, Vec3i>> clusters;
        clusters = oreClusterCalculator.calculateClusterLocations(chunks, RANDOM);

        //4. Add any loaded cluster to queue to generate ores now, stream the HashMap
        clusters.entrySet().stream().forEach( (entry) -> {
            String chunkId = entry.getKey();
            if( getChunkAccess(chunkId).getStatus().equals(ChunkStatus.FULL) )  //ASSUMING THIS IS VALID
                chunksPendingClusterGen.add(chunkId);
            existingClusters.put(chunkId, entry.getValue());
        });

    }

    /**
     * Batch process that determines the location of clusters in the next n chunks
     * Chunk cluster determinations are made spirally from the 'start' chunk, up, right, down, left
     */
    private static HashSet<String> getBatchedChunkList(int batchSize, ChunkAccess start)
    {
        //Create a hashset to store chunk ids
        //ids are of form x,z
        HashSet<String> chunkIds = new HashSet<>();
        ChunkGenerationOrderHandler spiralHandler = mainSpiral;
        if( exploredChunks.size() < Math.pow(config.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2) ) {
            spiralHandler = new ChunkGenerationOrderHandler(start.getPos());
        }

        //use a for loop to spiral out from center of new batch
        for( int i = 0; i < batchSize; i++ )
        {
            ChunkPos next = spiralHandler.getNextSpiralChunk();
            if (exploredChunks.contains(ChunkUtil.getId(next)))
                continue;
            chunkIds.add(ChunkUtil.getId(next));
            exploredChunks.add(ChunkUtil.getId(next));
        }

        return chunkIds;
    }


    /**
     * Handles creation of each type of ore cluster within each chunk
     */

    private static void handleClusterGeneration()
    {
        while( !chunksPendingClusterGen.isEmpty() )
        {
            String chunkId = chunksPendingClusterGen.poll();
            ChunkAccess chunk = getChunkAccess(chunkId);
            if( chunk == null )
                continue;
            LoggerBase.logDebug("Generating clusters for chunk: " + chunkId);
            HashMap<String, Vec3i> clusters = existingClusters.get(chunkId);
            clusters.entrySet().stream().forEach( (entry) -> {
                String oreType = entry.getKey();
                //Set source of cluster
                //Generate cluster
            });
        }

    }



    /**
     * Determines how to handle the newly loaded chunk upon loading
     * @param chunk
     */
    public static void onChunkLoad(ChunkAccess chunk)
    {
        LoggerBase.logDebug("Chunk loaded: " + chunk.getPos());
        handleClustersForChunk( chunk );
    }



    public static void onChunkUnload(ChunkAccess chunk) {

    }

    /**
     *              UTILITY SECTION
     */

    public static final ServerLevel overworld = RealTimeConfig.LEVEL.getServer().overworld();
    private static ChunkAccess getChunkAccess(String id) {
        return overworld.getChunk(ChunkUtil.getPos(id).x, ChunkUtil.getPos(id).z);
    }

    private static class ChunkGenerationOrderHandler
    {

        public static final int[] UP = new int[] {0, 1};
        public static final int[] RIGHT = new int[] {1, 0};
        public static final int[] DOWN = new int[] {0, -1};
        public static final int[] LEFT = new int[] {-1, 0};
        public static final int[][] DIRECTIONS = new int[][] {UP, RIGHT, DOWN, LEFT};

        public ChunkPos currentPos;
        public HashSet<ChunkPos> sequence;
        public int count;
        public int dirCount;
        public int[] dir;

        public ChunkGenerationOrderHandler(ChunkPos start)
        {
        if( start == null)
            this.currentPos = new ChunkPos(0, 0);
        else
            this.currentPos = start;
            sequence = new HashSet<>();
            count = 0;
        }

        public ChunkPos getNextSpiralChunk()
        {
            if( count == 0 ) {
                count++;
                return currentPos;
            }
            else {
                currentPos = ChunkUtil.posAdd(currentPos, dir);
                sequence.add(currentPos);
                dirCount++;
                if( dirCount == count)
                {
                    dir = getNextDirection();
                    dirCount = 0;
                    if( dir == DOWN )
                        count++;
                }

                return currentPos;
            }
        }

        public int[] getNextDirection()
        {
            int index = Arrays.stream(DIRECTIONS).toList().indexOf(dir);
            return DIRECTIONS[(index + 1) % DIRECTIONS.length];
        }

    }


}
