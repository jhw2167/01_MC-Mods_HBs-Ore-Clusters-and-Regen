package com.holybuckets.orecluster.core;

import com.holybuckets.foundation.HolyBucketsUtility.*;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.RealTimeConfig;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

//Java Imports

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    /** Variables **/
    private RealTimeConfig config;
    private Random randSeqClusterPositionGen;
    private Random randSeqClusterShapeGen;


    private final ConcurrentLinkedQueue<String> newlyLoadedChunks = new ConcurrentLinkedQueue<>();
    //<chunkId, <oreType, Vec3i>>
    private final ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters = new ConcurrentHashMap<>();
    //<oreType, <chunkId>>
    private final ConcurrentHashMap<String, HashSet<String>> existingClustersByType = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> chunksPendingClusterGen = new ConcurrentLinkedQueue<>();

    private final LinkedHashSet<String> exploredChunks = new LinkedHashSet<>();
    private final ChunkGenerationOrderHandler mainSpiral;

    private OreClusterCalculator oreClusterCalculator;

    //Threads
    private boolean managerRunning = true;
    private final ExecutorService threadPool;


    /** Constructor **/
    public OreClusterManager(RealTimeConfig config) {
        this.config = config;
        this.mainSpiral = new ChunkGenerationOrderHandler(null);
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        init();
        LoggerBase.logInit(this.getClass().getName());
    }

    /** Get Methods **/
    public RealTimeConfig getConfig() {
        return config;
    }

    public ConcurrentHashMap<String, HashMap<String, Vec3i>> getExistingClusters() {
        return existingClusters;
    }

    public ConcurrentHashMap<String, HashSet<String>> getExistingClustersByType() {
        return existingClustersByType;
    }

    public LinkedHashSet<String> getExploredChunks() {
        return exploredChunks;
    }

    public ConcurrentLinkedQueue<String> getChunksPendingClusterGen() {
        return chunksPendingClusterGen;
    }





    /** Behavior **/
    public void init()
    {

        if (RealTimeConfig.CLUSTER_SEED == null)
            RealTimeConfig.CLUSTER_SEED = this.config.WORLD_SEED;
        this.randSeqClusterPositionGen = new Random(RealTimeConfig.CLUSTER_SEED);

        this.oreClusterCalculator = new OreClusterCalculator( this );

        config.getOreConfigs().forEach((oreType, oreConfig) -> {
            existingClustersByType.put(oreType, new HashSet<>());
        });
        
        threadPool.execute(this::onNewlyAddedChunk);
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    /**
     * Determines how to handle the newly loaded chunk upon loading
     * @param chunk
     */
    public void onChunkLoad(ChunkAccess chunk) {
        String chunkId = ChunkUtil.getId(chunk);
        if( !exploredChunks.contains(chunkId) && !newlyLoadedChunks.contains(chunkId) )
            newlyLoadedChunks.add( chunkId );
    }

    /**
     * Newly loaded chunks are polled in a queue awaiting batch handling
     * If the chunk has already been processed it is skipped
     */
    private void onNewlyAddedChunk()
    {
        while (managerRunning)
        {
            while (!newlyLoadedChunks.isEmpty())
            {
                if ( this.config.PLAYER_LOADED )
                {
                    String chunkId = newlyLoadedChunks.poll();
                    if (!exploredChunks.contains(chunkId))
                        handleClustersForChunk(chunkId);
                    LoggerBase.logDebug("Chunk " + chunkId + " processed. Queue size: " + newlyLoadedChunks.size());
                    LoggerBase.logInfo("Chunk " + chunkId + " processed. Queue size: " + newlyLoadedChunks.size());
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                LoggerBase.logError(" onNewlyAddedChunk thread interrupted " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handle newly loaded chunk
     *
     * 1. If this chunkId exists in existingClusters, check regen
     * 2. If the chunkId exists in exploredChunks, ignore
     * 3. If the chunkId does not exist in exploredChunks, queue a batch
     *
     */
    private void handleClustersForChunk(String chunkId)
    {
        if (existingClusters.containsKey(chunkId)) {
            LoggerBase.logDebug("Chunk " + chunkId + " contains a cluster");
            //Check regen
        } else if (exploredChunks.contains(chunkId)) {
            LoggerBase.logDebug("Chunk " + chunkId + " has already been explored");
        } else {
            LoggerBase.logDebug("Chunk " + chunkId + " has not been explored");
            LoggerBase.logInfo("Chunk " + chunkId + " has not been explored");

            while (!exploredChunks.contains(chunkId))
            {
                handlePrepareNewChunksForClusters(config.ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL, chunkId);
            }
        }
    }

    /**
     * Batch process that determines the location of clusters in the next n chunks
     * @param batchSize
     * @param chunkId
     */
    private void handlePrepareNewChunksForClusters(int batchSize, String chunkId)
    {
        ChunkAccess start = getChunkAccess(chunkId);
        LinkedHashSet<String> chunkIds = getBatchedChunkList(batchSize, start);
        LoggerBase.logDebug("Queued " + chunkIds.size() + " chunks for cluster determination");

        List<ChunkAccess> chunks = chunkIds.stream().map(this::getChunkAccess)
                .collect(Collectors.toList());
        LoggerBase.logDebug("Produced " + chunks.size() + " chunkAccess objects: ");
        LoggerBase.logDebug("Chunks: \n" + chunks);

        HashMap<String, HashMap<String, Vec3i>> clusters;
        clusters = oreClusterCalculator.calculateClusterLocations(chunks, randSeqClusterPositionGen);

        LoggerBase.logDebug("Determined " + clusters.size() + " clusters in " + chunkIds.size() + " chunks");
        LoggerBase.logDebug("Clusters: " + clusters);

        clusters.forEach((id, clusterMap) -> {
            if (getChunkAccess(id).getStatus().equals(ChunkStatus.FULL))
                chunksPendingClusterGen.add(id);

            existingClusters.put(id, clusterMap);

            LinkedHashSet<String> oreTypesInThisCluster = new LinkedHashSet<>(clusterMap.keySet());
            existingClustersByType.forEach((type, set) -> {
                if (oreTypesInThisCluster.remove(type))
                    set.add(id);
            });

            oreTypesInThisCluster.forEach(type -> {
                HashSet<String> set = new HashSet<>();
                set.add(id);
                existingClustersByType.put(type, set);
            });
        });

        exploredChunks.addAll(chunkIds);
    }

    /**
     * Batch process that determines the location of clusters in the next n chunks
     * Chunk cluster determinations are made spirally from the 'start' chunk, up, right, down, left
     */
    private LinkedHashSet<String> getBatchedChunkList(int batchSize, ChunkAccess start) {
        LinkedHashSet<String> chunkIds = new LinkedHashSet<>();
        ChunkGenerationOrderHandler chunkIdGeneratorHandler = mainSpiral;
        if (exploredChunks.size() > Math.pow(config.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
            chunkIdGeneratorHandler = new ChunkGenerationOrderHandler(start.getPos());
        }

        for (int i = 0; i < batchSize; i++) {
            ChunkPos next = chunkIdGeneratorHandler.getNextSpiralChunk();
            chunkIds.add(ChunkUtil.getId(next));
        }

        return chunkIds;
    }

    /**
     * Handles creation of each type of ore cluster within each chunk
     */
    private void handleClusterGeneration() {
        while (!chunksPendingClusterGen.isEmpty()) {
            String chunkId = chunksPendingClusterGen.poll();
            ChunkAccess chunk = getChunkAccess(chunkId);
            if (chunk == null)
                continue;
            LoggerBase.logDebug("Generating clusters for chunk: " + chunkId);
            HashMap<String, Vec3i> clusters = existingClusters.get(chunkId);
            clusters.forEach((oreType, position) -> {
                //Set source of cluster
                //Generate cluster
            });
        }
    }

    public void onChunkUnload(ChunkAccess chunk) {
        // Implementation for chunk unload
    }

    /**
     *              UTILITY SECTION
     */

    private ChunkAccess getChunkAccess(String id) {
        return this.config.LEVEL.getServer().overworld()
            .getChunk(ChunkUtil.getPos(id).x, ChunkUtil.getPos(id).z);
    }

    /**
     * If the main spiral is still being explored (within 256x256 chunks of worldspawn)
     * then we return all explored chunks, otherwise we generate a new spiral with the requested area
     * at the requested chunk
     * @param start
     * @param spiralArea
     * @return LinkedHashSet of chunkIds that were recently explored
     */
    public LinkedHashSet<String> getRecentChunkIds(ChunkPos start, int spiralArea) {
        if (exploredChunks.size() < Math.pow(config.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
            return new LinkedHashSet<>(exploredChunks);
        } else {
            LinkedHashSet<String> chunkIds = new LinkedHashSet<>();
            ChunkGenerationOrderHandler spiralHandler = new ChunkGenerationOrderHandler(start);
            for (int i = 0; i < spiralArea; i++) {
                ChunkPos next = spiralHandler.getNextSpiralChunk();
                chunkIds.add(ChunkUtil.getId(next));
            }
            return chunkIds;
        }
    }

    //Create a destructor that calls shutdown
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }

    private class ChunkGenerationOrderHandler
    {
        private static final int[] UP = new int[]{0, 1};
        private static final int[] RIGHT = new int[]{1, 0};
        private static final int[] DOWN = new int[]{0, -1};
        private static final int[] LEFT = new int[]{-1, 0};
        private static final int[][] DIRECTIONS = new int[][]{UP, RIGHT, DOWN, LEFT};

        private ChunkPos currentPos;
        private LinkedHashSet<ChunkPos> sequence;
        private int count;
        private int dirCount;
        private int[] dir;

        public ChunkGenerationOrderHandler(ChunkPos start) {
            this.currentPos = (start == null) ? new ChunkPos(0, 0) : start;
            this.sequence = new LinkedHashSet<>();
            this.count = 1;
            this.dirCount = 0;
            this.dir = UP;
        }

        public ChunkPos getNextSpiralChunk()
        {
            if(sequence.size() == 0)
            {
                sequence.add(currentPos);
                return currentPos;
            }

            //This algorithm skips 1,0, fix it

            if (dirCount == count) {
                dir = getNextDirection();
                dirCount = 0;
                if (dir == UP || dir == DOWN) {
                    count++;
                }
            }

            currentPos = ChunkUtil.posAdd(currentPos, dir);
            sequence.add(currentPos);
            dirCount++;

            return currentPos;
        }

        private int[] getNextDirection() {
            int index = Arrays.asList(DIRECTIONS).indexOf(dir);
            return DIRECTIONS[(index + 1) % DIRECTIONS.length];
        }
    }
}
