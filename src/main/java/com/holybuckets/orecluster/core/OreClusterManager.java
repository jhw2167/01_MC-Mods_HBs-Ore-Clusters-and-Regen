package com.holybuckets.orecluster.core;

import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.foundation.HolyBucketsUtility.*;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.model.ManagedChunkCapabilityProvider;
import com.holybuckets.orecluster.LoggerProject;
import com.holybuckets.orecluster.ModRealTimeConfig;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import org.apache.commons.lang3.tuple.Pair;
import oshi.annotation.concurrent.ThreadSafe;

//Java Imports

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

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

    public static final String CLASS_ID = "002";    //value used in logs
    public static final GeneralRealTimeConfig GENERAL_CONFIG = GeneralRealTimeConfig.getInstance();
    public static Map<LevelAccessor, OreClusterManager> oreClusterManagers = new HashMap<>();

    /** Variables **/
    private final LevelAccessor level;
    private final ModRealTimeConfig config;
    private Random randSeqClusterPositionGen;
    //private Random randSeqClusterShapeGen;


    private final LinkedBlockingQueue<String> chunksPendingHandling;
    private final LinkedBlockingQueue<String> chunksPendingDeterminations;
    private final LinkedBlockingQueue<String> chunksPendingGeneration;
    private final ConcurrentHashMap<String, ManagedOreClusterChunk> chunksPendingManifestation;

    //<chunkId, <oreType, Vec3i>>

    private final ConcurrentHashMap<String, ManagedOreClusterChunk> determinedChunks;
    private final ConcurrentHashMap<String, ManagedOreClusterChunk> loadedChunks;
    //<oreType, <chunkIds>>
    private final ConcurrentHashMap<String, HashSet<String>> existingClustersByType;

    private final ChunkGenerationOrderHandler mainSpiral;

    private OreClusterCalculator oreClusterCalculator;

    //Threads
    private boolean managerRunning = true;
    private final ExecutorService threadPoolLoadedChunks;
    private final ExecutorService threadPoolClusterDetermination;
    private final ExecutorService threadPoolClusterGeneration;


    /** Constructor **/
    public OreClusterManager(LevelAccessor level, ModRealTimeConfig config)
    {
        super();
        oreClusterManagers.put(level, this);
        this.level = level;
        this.config = config;

        this.determinedChunks = new ConcurrentHashMap<>();
        this.existingClustersByType = new ConcurrentHashMap<>();
        this.loadedChunks = new ConcurrentHashMap<>();

        this.chunksPendingDeterminations = new LinkedBlockingQueue<>();
        this.chunksPendingGeneration = new LinkedBlockingQueue<>();
        this.chunksPendingHandling = new LinkedBlockingQueue<>();
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
        LoggerProject.logInit("002000", this.getClass().getName());
    }

    /** Get Methods **/
    public ModRealTimeConfig getConfig() {
        return config;
    }

    public ConcurrentHashMap<String, ManagedOreClusterChunk> getDeterminedChunks() {
        return determinedChunks;
    }

    public ConcurrentHashMap<String, HashSet<String>> getExistingClustersByType() {
        return existingClustersByType;
    }





    /** Behavior **/
    public void init()
    {

        if (ModRealTimeConfig.CLUSTER_SEED == null)
            ModRealTimeConfig.CLUSTER_SEED = GENERAL_CONFIG.getWORLD_SEED();
        this.randSeqClusterPositionGen = new Random(ModRealTimeConfig.CLUSTER_SEED);

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
        ChunkAccess chunk = chunkEvent.getChunk();

        loadedChunks.put(chunkId, new ManagedOreClusterChunk(level, chunk));
        chunksPendingHandling.add(chunkId);
        threadPoolLoadedChunks.submit(this::onNewlyAddedChunk);
        //LoggerBase.logInfo("002001", "Chunk " + chunkId + " added to queue size " + chunksPendingHandling.size());

    }

    public void onChunkUnload(ChunkEvent.Unload event)
    {
        // Implementation for chunk unload
        ChunkAccess chunk = event.getChunk();
        String chunkId = ChunkUtil.getId(event.getChunk());
        //LevelChunk levelChunk = level.getChunkSource().getChunk(chunk.getPos().x, chunk.getPos().z, true);
        //editManagedChunk(chunkId, c -> c.setChunk(null));

    }

    /**
     * Newly loaded chunks are polled in a queue awaiting batch handling
     * If the chunk has already been processed it is skipped
     */
    private void onNewlyAddedChunk()
    {

        try
        {
            while( !chunksPendingHandling.isEmpty() )
            {
                String chunkId = chunksPendingHandling.poll(1, TimeUnit.SECONDS);
                long start = System.nanoTime();
                handleClustersForChunk(chunkId);
                long end = System.nanoTime();
                //LoggerBase.logDebug("002002","Chunk " + chunkId + " processed. Queue size: " + chunksPendingHandling.size());
                //LoggerBase.logDebug("Full process for Chunk " + chunkId + "  took " + LoggerBase.getTime(start, end) + " ms");

                //Remove duplicates
                chunksPendingHandling.remove(chunkId);
            }
        }
        catch (InterruptedException e)
        {
            LoggerProject.logError("002003","OreClusterManager::onNewlyAddedChunk() thread interrupted: "
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
        if (determinedChunks.get(chunkId) != null && determinedChunks.get(chunkId).hasClusters() ) {
            LoggerProject.logDebug("002004","Chunk " + chunkId + " contains a cluster");
            //Check regen
        } else if (determinedChunks.containsKey(chunkId)) {
            //LoggerProject.logDebug("002005","Chunk " + chunkId + " has already been explored");
        } else {
            //LoggerProject.logDebug("002006","Chunk " + chunkId + " has not been explored");
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

                //LoggerBase.logDebug("002017", "workerThreadDetermineClusters for chunkId: " + chunkId);
                while (!determinedChunks.containsKey(chunkId))
                {
                    LoggerProject.logDebug("002018", "handleClusterDetermination, starting batch for: " + chunkId);
                    handleClusterDetermination(ModRealTimeConfig.ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL, chunkId);
                   //LoggerBase.logDebug("002019", "handleClusterDetermination, finished batch for: " + chunkId);

                }

                //MAX
                //LoggerBase.logDebug("002020", "workerThreadDetermineClusters, after handleClusterDetermination for chunkId: " + chunkId);
            }

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerProject.threadExited("002011",this, thrown);
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

                Optional<ManagedOreClusterChunk> chunk = editManagedChunk(chunkId, this::handleClusterGeneration);

            }

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerProject.threadExited("002007",this, thrown);
        }
    }


    /**
     * Batch process that determines the location of clusters in the next n chunks
     * @param batchSize
     * @param chunkId
     */
    private void handleClusterDetermination(int batchSize, String chunkId)
    {
        long startTime = System.nanoTime();
        ChunkAccess start = getChunkAccess(chunkId);
        LinkedHashSet<String> chunkIds = getBatchedChunkList(batchSize, start);
        long step1Time = System.nanoTime();
        //LoggerBase.logDebug("002008", "Queued " + chunkIds.size() + " chunks for cluster determination");


        //LoggerBase.logDebug("handlePrepareNewCluster #1  " + LoggerBase.getTime(startTime, step1Time) + " ms");


        HashMap<String, HashMap<String, Vec3i>> clusters;
        clusters = oreClusterCalculator.calculateClusterLocations(chunkIds.stream().toList() , randSeqClusterPositionGen);
        long step2Time = System.nanoTime();
        //LoggerBase.logDebug("002009","Determined " + clusters.size() + " clusters in " + chunkIds.size() + " chunks");
        //LoggerBase.logDebug("handlePrepareNewCluster #3  " + LoggerBase.getTime(step1Time, step2Time) + " ms");



        // #3. Add clusters to determinedClusters
        for( String id: chunkIds)
        {
            ManagedOreClusterChunk chunk = loadedChunks.getOrDefault(id, new ManagedOreClusterChunk(level, id));
            chunk.addClusterTypes(clusters.get(id));
            determinedChunks.put(id, chunk);
        }
        //LoggerBase.logDebug("002010","Added " + clusters.size() + " clusters to determinedChunks");

        long step3Time = System.nanoTime();
        //        //LoggerBase.logDebug("handlePrepareNewCluster #3  " + LoggerBase.getTime(step2Time, step3Time) + " ms");

        for( Map.Entry<String, HashMap<String, Vec3i>> cluster : clusters.entrySet())
        {
            //Add chunkId to existingClustersByType Map
            LinkedHashSet<String> oreTypesInThisCluster = new LinkedHashSet<>(cluster.getValue().keySet());
            existingClustersByType.forEach((type, set) -> {
                if (oreTypesInThisCluster.remove(type))
                    set.add(cluster.getKey());
            });

           // LoggerBase.logDebug("002012","determined oreTypesInThisCluster: " + oreTypesInThisCluster.size());


            if(oreTypesInThisCluster.size() == 0)
            {
                //LoggerBase.logDebug("002013","No new ore types in this cluster");
                continue;
            }


            //Ore types not yet added to the Aggregate List
            oreTypesInThisCluster.forEach(type -> {
                HashSet<String> set = new HashSet<>();
                set.add(cluster.getKey());
                existingClustersByType.put(type, set);
            });

           //LoggerBase.logDebug("002014","Added net new oreTypes to grand list, new size " + existingClustersByType.size());
        }


        long endTime = System.nanoTime();
        //LoggerBase.logDebug("handlePrepareNewCluster #4  " + LoggerBase.getTime(step3Time, endTime) + " ms");
    }

    private void handleClusterCleaning( ManagedOreClusterChunk chunk )
    {
        //1. Get clusters for chunk
        //2. Clean clusters in world
        //3. Write data to chunk NBT data
        //4. Release hold on resource
    }


    /**
     * Handles creation of each type of ore cluster within each chunk
     */
    private void handleClusterGeneration(ManagedOreClusterChunk chunk)
    {
        LoggerProject.logDebug("002015","Generating clusters for chunk: " + chunk.getId());


        HashMap<String, Vec3i> clusters = chunk.getClusterTypes();
        List<Pair<String, Vec3i>> cluster = chunk.getClusters();
        clusters.forEach((oreType, position) -> {
            position = oreClusterCalculator.determineSourcePosition(oreType, chunk.getChunk());
            cluster.addAll( oreClusterCalculator.generateCluster(oreType, position, chunk.getChunk()) );
        });

        chunk.setStatus(ManagedOreClusterChunk.ClusterStatus.GENERATED);

    }

    /**
     * Alters the chunk to place blocks in the world as necessary to build clusters or reduce
     * @param chunk
     */
    private void handleClusterManifestation(ManagedOreClusterChunk chunk)
    {
        //1. Get clusters for chunk


        //2. Generate clusters in world

        //3. Write data to chunk NBT data

        //4. Release hold on resource
    }


    /**
     *              UTILITY SECTION
     */

    private ChunkAccess getChunkAccess(String id) {
        return loadedChunks.get(id).getChunk();
    }

    /**
     * Batch process that determines the location of clusters in the next n chunks
     * Chunk cluster determinations are made spirally from the 'start' chunk, up, right, down, left
     */
    private LinkedHashSet<String> getBatchedChunkList(int batchSize, ChunkAccess start)
    {
        LinkedHashSet<String> chunkIds = new LinkedHashSet<>();
        ChunkGenerationOrderHandler chunkIdGeneratorHandler = mainSpiral;
        if (determinedChunks.size() > Math.pow(ModRealTimeConfig.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
            chunkIdGeneratorHandler = new ChunkGenerationOrderHandler(start.getPos());
        }

        for (int i = 0; i < batchSize; i++) {
            ChunkPos next = chunkIdGeneratorHandler.getNextSpiralChunk();
            chunkIds.add(ChunkUtil.getId(next));
        }

        return chunkIds;
    }

    /**
     * If the main spiral is still being explored (within 256x256 chunks of worldspawn)
     * then we return all explored chunks, otherwise we generate a new spiral with the requested area
     * at the requested chunk
     * @param start
     * @param spiralArea
     * @return LinkedHashSet of chunkIds that were recently explored
     */
    public LinkedHashSet<String> getRecentChunkIds(ChunkPos start, int spiralArea)
    {
        if (determinedChunks.size() < Math.pow(ModRealTimeConfig.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
            return determinedChunks.keySet().stream().collect(
                Collectors.toCollection(LinkedHashSet::new));
        }
        else
        {
            LinkedHashSet<String> chunkIds = new LinkedHashSet<>();
            ChunkGenerationOrderHandler spiralHandler = new ChunkGenerationOrderHandler(start);

            try
            {
                for (int i = 0; i < spiralArea; i++) {
                    ChunkPos next = spiralHandler.getNextSpiralChunk();
                    chunkIds.add( ChunkUtil.getId(next) );
                }

            } catch (Exception e) {
                LoggerProject.logError("002016","Error generating spiral chunk ids at startPos: " + start.toString() + " message " + e.getMessage());
            }

            return chunkIds;
        }
    }

    /**
     * Edits a ManagedChunk object from determinedChunks with a consumer, ensuring each object is edited atomically
     * @param chunkId
     * @param consumer
     * @return
     */
    @ThreadSafe
    private synchronized Optional<ManagedOreClusterChunk> editManagedChunk(String chunkId, Consumer<ManagedOreClusterChunk> consumer)
    {
        ManagedOreClusterChunk chunk = determinedChunks.get(chunkId);
        if (chunk != null) {
            synchronized (chunk) {
                consumer.accept(chunk);
            }

        }

        return Optional.ofNullable(chunk);
    }



    public void shutdown() {
        managerRunning = false;
        threadPoolLoadedChunks.shutdown();
    }

    public ConcurrentHashMap<String, ManagedOreClusterChunk> getLoadedChunks() {
        return loadedChunks;
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

        public ChunkGenerationOrderHandler(ChunkPos start)
        {
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
