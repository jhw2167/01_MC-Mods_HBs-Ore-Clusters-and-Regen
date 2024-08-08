package com.holybuckets.orecluster.core;

import com.holybuckets.foundation.HolyBucketsUtility.*;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.RealTimeConfig;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;

//Java Imports

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private final RealTimeConfig config;
    private Random randSeqClusterPositionGen;
    //private Random randSeqClusterShapeGen;


    private final LinkedBlockingQueue<String> newlyLoadedChunks;
    //<chunkId, <oreType, Vec3i>>
    private final ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters;
    //<oreType, <chunkId>>
    private final ConcurrentHashMap<String, HashSet<String>> existingClustersByType;
    private final ConcurrentLinkedQueue<String> chunksPendingClusterGen;

    private final LinkedHashSet<String> exploredChunks;
    private final ChunkGenerationOrderHandler mainSpiral;

    private OreClusterCalculator oreClusterCalculator;

    //Threads
    private boolean managerRunning = true;
    private final ExecutorService threadPoolClusterDetermination;


    /** Constructor **/
    public OreClusterManager(RealTimeConfig config)
    {
        this.config = config;

        this.existingClusters = new ConcurrentHashMap<>();
        this.existingClustersByType = new ConcurrentHashMap<>();
        this.exploredChunks = new LinkedHashSet<>();
        this.newlyLoadedChunks = new LinkedBlockingQueue<>();
        this.chunksPendingClusterGen = new ConcurrentLinkedQueue<>();

        this.mainSpiral = new ChunkGenerationOrderHandler(null);
        //Thread pool needs to have one thread max, use Synchronous queue and discard policy
        this.threadPoolClusterDetermination = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
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

        config.getOreConfigs().forEach((oreType, oreConfig) ->
            existingClustersByType.put(oreType, new HashSet<>()));
        
        //threadPool.execute(this::onNewlyAddedChunk);
    }

    /**
     * Determines how to handle the newly loaded chunk upon loading
     * @param chunkEvent chunk from the onChunkLoad event
     */
    public void onChunkLoad(ChunkEvent.Load chunkEvent) {
        String chunkId = ChunkUtil.getId(chunkEvent.getChunk());
        if( !exploredChunks.contains(chunkId)  )
        {
            newlyLoadedChunks.add(chunkId);
            threadPoolClusterDetermination.submit(this::onNewlyAddedChunk);
            LoggerBase.logInfo("Chunk " + chunkId + " added to queue size " + newlyLoadedChunks.size());
        }

    }

    /**
     * Newly loaded chunks are polled in a queue awaiting batch handling
     * If the chunk has already been processed it is skipped
     */
    private void onNewlyAddedChunk()
    {

        try
        {
            while( !newlyLoadedChunks.isEmpty() )
            {
                String chunkId = newlyLoadedChunks.poll(1, TimeUnit.SECONDS);
                if (!exploredChunks.contains(chunkId))
                {
                    long start = System.nanoTime();
                    handleClustersForChunk(chunkId);
                    long end = System.nanoTime();
                    LoggerBase.logDebug("Chunk " + chunkId + " processed. Queue size: " + newlyLoadedChunks.size());
                    LoggerBase.logDebug("Full process for Chunk " + chunkId + "  took " +
                        LoggerBase.getTime(start, end) + " ms");
                }

            }

        }
        catch (InterruptedException e)
        {
            LoggerBase.logError("OreClusterManager::onNewlyAddedChunk() thread interrupted: "
                 + e.getMessage());
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
            //LoggerBase.logInfo("Chunk " + chunkId + " has not been explored");

            while (!exploredChunks.contains(chunkId))
            {
                handlePrepareNewChunksForClusters(RealTimeConfig.ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL, chunkId);
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
        long startTime = System.nanoTime();
        ChunkAccess start = getChunkAccess(chunkId);
        LinkedHashSet<String> chunkIds = getBatchedChunkList(batchSize, start);
        long step1Time = System.nanoTime();
        LoggerBase.logDebug("Queued " + chunkIds.size() + " chunks for cluster determination");

        //LoggerBase.logDebug("handlePrepareNewCluster #1  " + LoggerBase.getTime(startTime, step1Time) + " ms");

        //List<ChunkAccess> chunkIds = chunkIds.stream().map(this::getChunkAccess)
                //.collect(Collectors.toList());
        LoggerBase.logDebug("Produced " + chunkIds.size() + " chunkAccess objects: ");
        long step2Time = System.nanoTime();
        //LoggerBase.logDebug("handlePrepareNewCluster #2  " + LoggerBase.getTime(step1Time, step2Time) + " ms");


        HashMap<String, HashMap<String, Vec3i>> clusters;
        clusters = oreClusterCalculator.calculateClusterLocations(chunkIds.stream().toList() , randSeqClusterPositionGen);
        long step3Time = System.nanoTime();
        LoggerBase.logDebug("Determined " + clusters.size() + " clusters in " + chunkIds.size() + " chunks");
        //LoggerBase.logDebug("handlePrepareNewCluster #3  " + LoggerBase.getTime(step2Time, step3Time) + " ms");


        clusters.forEach((id, clusterMap) -> {

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
        long step4Time = System.nanoTime();
        //LoggerBase.logDebug("handlePrepareNewCluster #4  " + LoggerBase.getTime(step3Time, step4Time) + " ms");

        exploredChunks.addAll(chunkIds);
        long endTime = System.nanoTime();
        //LoggerBase.logDebug("handlePrepareNewCluster #5  " + LoggerBase.getTime(step4Time, endTime) + " ms");
    }

    /**
     * Batch process that determines the location of clusters in the next n chunks
     * Chunk cluster determinations are made spirally from the 'start' chunk, up, right, down, left
     */
    private LinkedHashSet<String> getBatchedChunkList(int batchSize, ChunkAccess start) {
        LinkedHashSet<String> chunkIds = new LinkedHashSet<>();
        ChunkGenerationOrderHandler chunkIdGeneratorHandler = mainSpiral;
        if (exploredChunks.size() > Math.pow(RealTimeConfig.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
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

            LoggerBase.logDebug("Generating clusters for chunk: " + chunkId);
            HashMap<String, Vec3i> clusters = existingClusters.get(chunkId);
            clusters.forEach((oreType, position) -> {
                //Set source of cluster
                //Generate cluster
            });
        }
    }

    public void onChunkUnload(ChunkEvent.Unload chunk) {
        // Implementation for chunk unload
        newlyLoadedChunks.remove(ChunkUtil.getId(chunk.getChunk()));
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
        if (exploredChunks.size() < Math.pow(RealTimeConfig.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
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


    public void shutdown() {
        managerRunning = false;
        threadPoolClusterDetermination.shutdown();
    }
    


    private class ChunkGenerationOrderHandler
    {
        private static final int[] UP = new int[]{0, 1};
        private static final int[] RIGHT = new int[]{1, 0};
        private static final int[] DOWN = new int[]{0, -1};
        private static final int[] LEFT = new int[]{-1, 0};
        private static final int[][] DIRECTIONS = new int[][]{UP, RIGHT, DOWN, LEFT};

        private ChunkPos currentPos;
        private final LinkedHashSet<ChunkPos> sequence;
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
