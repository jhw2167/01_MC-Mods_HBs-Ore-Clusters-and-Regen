package com.holybuckets.orecluster.core;

import com.holybuckets.foundation.HolyBucketsUtility.*;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.RealTimeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import org.apache.commons.lang3.tuple.Pair;

//Java Imports

import java.util.*;
import java.util.concurrent.*;

/**
 * Class: OreClusterManager
 *
 * Description: This class will manage all ore clusters that exist in the instance
 *  - Determines which chunks clusters will appear in
 *  - Determines the type of cluster that will appear
 *  - All variables and methods are static
 *
 *  #Variables - list all variables and a brief description
 *  config - (private) RealTimeConfig object contains statically defined and configurable variables
 *  randSeqClusterPositionGen - (private) Random object for generating cluster positions
 *
 *  newlyLoadedChunks - (private) LinkedBlockingQueue of chunkIds that have been loaded and not yet processed
 *  chunksPendingDeterminations - (private) LinkedBlockingQueue of chunkIds that are pending cluster determination
 *  chunksPendingGeneration - (private) LinkedBlockingQueue of chunkIds that are pending cluster generation
 *
 *  existingClusters - (private) ConcurrentHashMap of <chunkId, <oreType, Vec3i>> containing all existing clusters
 *      in the world, each String chunkId maps to a HashMap of each chunk's cluster type(s) and origin
 *  existingClustersByType - (private) ConcurrentHashMap of <oreType, <chunkId>> containing all existing clusters
 *      allows to check quickly if any newly generated chunk has a nearby cluster of its type
 *  chunksPendingClusterGen - (private) ConcurrentLinkedQueue of chunkIds that are pending cluster generation in the main gamethread
 *
 *  exploredChunks - (private) LinkedHashSet of chunkIds that have been explored
 *  mainSpiral - (private) ChunkGenerationOrderHandler object that generates a spiral of chunkIds
 *
 *  oreClusterCalculator - (private) Handles calculations for cluster determination and generation
 *  managerRunning - (private) boolean flag for toggling internal threads on and off
 *
 *  threadPoolLoadedChunks - (private) ExecutorService for handling newly loaded chunks, 1 thread
 *  threadPoolClusterDetermination - (private) ExecutorService for handling cluster determinations, 1 thread
 *  threadPoolClusterGeneration - (private) ExecutorService for handling cluster generation, 3 threads
 *
 *  #Methods - list all methods and a brief description
 *
 **/


public class OreClusterManager {

    /** Variables **/
    private final RealTimeConfig config;
    private Random randSeqClusterPositionGen;
    //private Random randSeqClusterShapeGen;


    private final LinkedBlockingQueue<String> newlyLoadedChunks;
    private final LinkedBlockingQueue<String> chunksPendingDeterminations;
    private final LinkedBlockingQueue<String> chunksPendingGeneration;
    private final ConcurrentHashMap<String, List<Pair<String, Vec3i>>> chunksPendingManifestation;

    //<chunkId, <oreType, Vec3i>>
    private final ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters;
    //<oreType, <chunkIds>>
    private final ConcurrentHashMap<String, HashSet<String>> existingClustersByType;

    private final LinkedHashSet<String> exploredChunks;
    private final ChunkGenerationOrderHandler mainSpiral;

    private OreClusterCalculator oreClusterCalculator;

    //Threads
    private boolean managerRunning = true;
    private final ExecutorService threadPoolLoadedChunks;
    private final ExecutorService threadPoolClusterDetermination;
    private final ExecutorService threadPoolClusterGeneration;


    /** Constructor **/
    public OreClusterManager(RealTimeConfig config)
    {
        this.config = config;

        this.existingClusters = new ConcurrentHashMap<>();
        this.existingClustersByType = new ConcurrentHashMap<>();
        this.exploredChunks = new LinkedHashSet<>();
        this.chunksPendingDeterminations = new LinkedBlockingQueue<>();
        this.chunksPendingGeneration = new LinkedBlockingQueue<>();
        this.newlyLoadedChunks = new LinkedBlockingQueue<>();
        this.chunksPendingManifestation = new ConcurrentHashMap<>();

        this.mainSpiral = new ChunkGenerationOrderHandler(null);
        //Thread pool needs to have one thread max, use Synchronous queue and discard policy
        this.threadPoolLoadedChunks = new ThreadPoolExecutor(1, 1, 1L,
         TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
        //Thread pool
        this.threadPoolClusterDetermination = new ThreadPoolExecutor(1, 1,
         30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
        //Thread pool with unlimited buffer, 3 threads max
        this.threadPoolClusterGeneration = Executors.newFixedThreadPool(3);

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




    /** Behavior **/
    public void init()
    {

        if (RealTimeConfig.CLUSTER_SEED == null)
            RealTimeConfig.CLUSTER_SEED = this.config.WORLD_SEED;
        this.randSeqClusterPositionGen = new Random(RealTimeConfig.CLUSTER_SEED);

        this.oreClusterCalculator = new OreClusterCalculator( this );

        config.getOreConfigs().forEach((oreType, oreConfig) ->
            existingClustersByType.put(oreType, new HashSet<>()));

    }

    /**
     * Determines how to handle the newly loaded chunk upon loading
     * @param chunkEvent chunk from the onChunkLoad event
     */
    public void onChunkLoad(ChunkEvent.Load chunkEvent)
    {
        String chunkId = ChunkUtil.getId(chunkEvent.getChunk());
        newlyLoadedChunks.add(chunkId);
        threadPoolLoadedChunks.submit(this::onNewlyAddedChunk);
        LoggerBase.logInfo("Chunk " + chunkId + " added to queue size " + newlyLoadedChunks.size());

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
                long start = System.nanoTime();
                handleClustersForChunk(chunkId);
                long end = System.nanoTime();
                LoggerBase.logDebug("Chunk " + chunkId + " processed. Queue size: " + newlyLoadedChunks.size());
                //LoggerBase.logDebug("Full process for Chunk " + chunkId + "  took " + LoggerBase.getTime(start, end) + " ms");

                //Remove duplicates
                newlyLoadedChunks.remove(chunkId);

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

            chunksPendingDeterminations.add(chunkId);
            this.threadPoolClusterDetermination.submit(this::workerThreadDetermineClusters);
        }
    }

    private void workerThreadDetermineClusters()
    {
        Throwable thrown = null;
        try
        {
            while( managerRunning )
            {
                String chunkId = chunksPendingDeterminations.poll();
                if( chunkId == null )
                    break;

                while (!exploredChunks.contains(chunkId)) {
                    handlePrepareNewChunksForClusters(RealTimeConfig.ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL, chunkId);
                }
            }

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerBase.threadExited(this, thrown);
        }
    }

    private  void workerThreadGenerateClusters()
    {
        Throwable thrown = null;
        try
        {
            while( managerRunning )
            {
                String chunkId = chunksPendingGeneration.poll();
                if( chunkId == null )
                    break;

                ChunkAccess chunk = getChunkAccess(chunkId);
                handleClusterGeneration( chunk );

            }

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerBase.threadExited(this, thrown);
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
    private void handleClusterGeneration(ChunkAccess chunk)
    {
        String chunkId = ChunkUtil.getId(chunk);
        LoggerBase.logDebug("Generating clusters for chunk: " + chunkId);

        chunk.getBlockEntity(new BlockPos(0, 0, 0));

        HashMap<String, Vec3i> clusters = existingClusters.get(chunkId);
        List<Pair<String, Vec3i>> cluster = chunksPendingManifestation.getOrDefault(chunkId, new LinkedList<>());
        clusters.forEach((oreType, position) -> {
            position = oreClusterCalculator.determineSourcePosition(oreType, chunk);
            cluster.addAll( oreClusterCalculator.generateCluster(oreType, position, chunk) );
        });

        chunksPendingManifestation.put(chunkId, cluster);

    }

    private void mainThreadGenerateClusters( ChunkAccess chunk )
    {
        //1. Get clusters for chunk
        String chunkId = ChunkUtil.getId(chunk);


        //2. Generate clusters in world

        //3. Write data to chunk NBT data

        //4. Release hold on resource
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
        threadPoolLoadedChunks.shutdown();
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
