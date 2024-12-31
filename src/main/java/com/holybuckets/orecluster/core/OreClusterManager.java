package com.holybuckets.orecluster.core;

import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.HBUtil.*;
import com.holybuckets.foundation.model.ManagedChunk;
import com.holybuckets.orecluster.LoggerProject;
import com.holybuckets.orecluster.ModRealTimeConfig;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import org.apache.commons.lang3.tuple.Pair;
import oshi.annotation.concurrent.ThreadSafe;

//Java Imports

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
    public static final GeneralConfig GENERAL_CONFIG = GeneralConfig.getInstance();

    /** Variables **/
    private final LevelAccessor level;
    private final ModRealTimeConfig config;
    private Random randSeqClusterPositionGen;
    private Random randSeqClusterBuildGen;
    //private Random randSeqClusterShapeGen;


    private final LinkedBlockingQueue<String> chunksPendingHandling;
    private final LinkedBlockingQueue<String> chunksPendingDeterminations;
    private final ConcurrentHashMap<String, ManagedOreClusterChunk> chunksPendingCleaning;
    private final ConcurrentHashMap<String, ManagedOreClusterChunk> chunksPendingGeneration;
    //private final ConcurrentHashMap<String, ManagedOreClusterChunk> chunksPendingManifestation;

    //<chunkId, <oreType, Vec3i>>


    private final ConcurrentHashMap<String, ManagedOreClusterChunk> loadedOreClusterChunks;

    private final ConcurrentHashMap<Block, HashSet<String>> existingClustersByType;
    private final ChunkGenerationOrderHandler mainSpiral;
    private OreClusterCalculator oreClusterCalculator;

    //Threads
    private boolean managerRunning = true;
    private final ExecutorService threadPoolLoadedChunks;
    private final ExecutorService threadPoolClusterDetermination;
    private final ThreadPoolExecutor threadPoolClusterCleaning;
    private final ThreadPoolExecutor threadPoolClusterGenerating;
    private final ThreadPoolExecutor threadPoolChunkEditing;
    //private final ThreadPoolExecutor threadPoolChunkProcessing;



    /** Constructor **/
    public OreClusterManager(LevelAccessor level, ModRealTimeConfig config)
    {
        super();
        this.level = level;
        this.config = config;

        this.existingClustersByType = new ConcurrentHashMap<Block, HashSet<String>>();
        this.loadedOreClusterChunks = new ConcurrentHashMap<>();

        this.chunksPendingHandling = new LinkedBlockingQueue<>();
        this.chunksPendingDeterminations = new LinkedBlockingQueue<>();
        this.chunksPendingCleaning = new ConcurrentHashMap<>();
        this.chunksPendingGeneration = new ConcurrentHashMap<>();

        //this.chunksPendingManifestation = new ConcurrentHashMap<>();

        this.mainSpiral = new ChunkGenerationOrderHandler(null);
        //Thread pool needs to have one thread max, use Synchronous queue and discard policy
        this.threadPoolLoadedChunks = new ThreadPoolExecutor(1, 1, 1L,
         TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
        //Thread pool
        this.threadPoolClusterDetermination = new ThreadPoolExecutor(1, 1,
         30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());

        this.threadPoolClusterCleaning = new ThreadPoolExecutor(1, 1,
            30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());

        this.threadPoolClusterGenerating = new ThreadPoolExecutor(1, 1,
            30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());

        /*
        this.threadPoolChunkProcessing = new ThreadPoolExecutor(1, 1,
            300L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());
         */

        this.threadPoolChunkEditing = new ThreadPoolExecutor(1, 1,
            300L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.DiscardPolicy());


        init(level);
        LoggerProject.logInit("002000", this.getClass().getName());
    }

    /** Get Methods **/
    public ModRealTimeConfig getConfig() {
        return config;
    }

    public Map<String, ManagedOreClusterChunk> getLoadedOreClusterChunks() {
        return loadedOreClusterChunks;
    }

    public ConcurrentHashMap<Block, HashSet<String>> getExistingClustersByType() {
        return existingClustersByType;
    }





    /** Behavior **/
    public void init(LevelAccessor level)
    {

        if (ModRealTimeConfig.CLUSTER_SEED == null)
            ModRealTimeConfig.CLUSTER_SEED = GENERAL_CONFIG.getWORLD_SEED();
        long seed = ModRealTimeConfig.CLUSTER_SEED * level.hashCode();
        this.randSeqClusterPositionGen = new Random(seed);
        //this.randSeqClusterBuildGen = new Random(seed);

        this.oreClusterCalculator = new OreClusterCalculator( this );

        config.getOreConfigs().forEach((oreType, oreConfig) ->
            existingClustersByType.put(oreType, new HashSet<>())
        );

    }

    /**
     * Description: Handles newly loaded chunks
     * @param chunk
     */
    public void handleChunkLoaded(ChunkAccess chunk, ManagedOreClusterChunk managedChunk)
    {
        loadedOreClusterChunks.put(managedChunk.getId(), managedChunk);
        chunksPendingHandling.add(managedChunk.getId());
        threadPoolLoadedChunks.submit(this::workerThreadLoadedChunk);
        threadPoolChunkEditing.submit(this::workerThreadEditChunk);
        //LoggerProject.logInfo("002001", "Chunk " + chunkId + " added to queue size " + chunksPendingHandling.size());

    }

    /**
     * Description: Handles newly unloaded chunks
     * @param chunk
     */
    public void handleChunkUnloaded(ChunkAccess chunk)
    {
        String chunkId = ChunkUtil.getId(chunk);
        loadedOreClusterChunks.remove(chunkId);
    }

    /**
     * Newly loaded chunks are polled in a queue awaiting batch handling
     * If the chunk has already been processed it is skipped
     */
    private void workerThreadLoadedChunk()
    {

        try
        {
            while( !chunksPendingHandling.isEmpty() )
            {
                String chunkId = chunksPendingHandling.poll(1, TimeUnit.SECONDS);

                if( chunkId == null )
                    continue;

                long start = System.nanoTime();
                handleClustersForChunk(chunkId);
                long end = System.nanoTime();
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
        ManagedOreClusterChunk chunk = loadedOreClusterChunks.get(chunkId);
        if( chunk == null || chunk.getStatus() == ManagedOreClusterChunk.ClusterStatus.NONE )
        {
            /** Determine Chunk **/
            //LoggerProject.logDebug("002006","Chunk " + chunkId + " has not been explored");
            chunksPendingDeterminations.add(chunkId);
            this.threadPoolClusterDetermination.submit(this::workerThreadDetermineClusters);
        }
        else if( ManagedOreClusterChunk.isDetermined(chunk) ) {
            chunksPendingCleaning.put(chunkId, chunk);
            this.threadPoolClusterCleaning.submit(this::workerThreadCleanClusters);
        }
        else if( ManagedOreClusterChunk.isCleaned(chunk) )
        {
            //LoggerProject.logDebug("002007","Chunk " + chunkId + " has been cleaned");
            if(chunk.hasClusters() )
            {
                chunksPendingGeneration.put(chunkId, chunk);
                this.threadPoolClusterGenerating.submit(this::workerThreadGenerateClusters);
            }

        }
        else if( ManagedOreClusterChunk.isGenerated(chunk) )
        {
            //LoggerProject.logDebug("002008","Chunk " + chunkId + " has been generated");
            //chunksPendingManifestation.add(chunkId);
        }
        else
        {
            //LoggerProject.logDebug("002009","Chunk " + chunkId + " has already been explored");
        }

    }

    private void workerThreadDetermineClusters()
    {
        Throwable thrown = null;
        try
        {
            while( managerRunning )
            {

                if( chunksPendingDeterminations.size() == 0 ) {
                    sleep(10);
                    continue;
                }
                String chunkId = chunksPendingDeterminations.poll();

                //LoggerProject.logDebug("002017", "workerThreadDetermineClusters for chunkId: " + chunkId);
                while (loadedOreClusterChunks.get(chunkId).getStatus() != ManagedOreClusterChunk.ClusterStatus.DETERMINED)
                {
                    handleClusterDetermination(ModRealTimeConfig.ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL, chunkId);
                    this.threadPoolClusterCleaning.submit(this::workerThreadCleanClusters);
                }


                //MAX
                LoggerProject.logDebug("002020", "workerThreadDetermineClusters, after handleClusterDetermination for chunkId: " + chunkId);
            }

        }
        catch (Exception e) {
            thrown = e;
            LoggerProject.logError("002011.1","Error in workerThreadDetermineClusters: " + e.getMessage());
        }
        finally {
            LoggerProject.threadExited("002011",this, thrown);
        }
    }

    /**
     * Description: Polls determinedChunks attempts to clean any chunk and
     * adds any cluster chunk to the chunksPendingGeneration queue. If chunk
     * is not loaded at the time it is polled, it is skipped and re-added to the queue.
     *
     * 0. Get iterable list of all determined chunks, filter by status == Determined
     * 1. Get next determined chunkId
     * 2. Determine cluster is loaded
     *
     * 3. Thread the chunk cleaning process, low priority, same executor as cluster generation
     * 4. handleChunkCleaning will add the chunk to chunksPendingGeneration once finished
     */
    private void workerThreadCleanClusters()
    {
        Throwable thrown = null;
        try
        {
            while( managerRunning )
            {

                ManagedOreClusterChunk.ClusterStatus DETERMINED = ManagedOreClusterChunk.ClusterStatus.DETERMINED;
                Queue<ManagedOreClusterChunk> chunksToClean = chunksPendingCleaning.values().stream()
                    .filter(chunk -> chunk.getStatus() == DETERMINED && chunk.hasChunk())
                    .collect(Collectors.toCollection(LinkedList::new));

            if( chunksToClean.size() == 0 ) {
                sleep(10);
                continue;
            }
                LoggerProject.logDebug("002026", "workerThreadCleanClusters cleaning chunks: " + chunksToClean.size());

                for (ManagedOreClusterChunk chunk : chunksToClean)
                {
                        try {
                            editManagedChunk(chunk, this::handleChunkCleaning);
                            chunksPendingCleaning.remove(chunk.getId());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            LoggerProject.logError("002030","Error cleaning chunk: " + chunk.getId() + " message: "  );
                        }

                }

            }
            //END WHILE MANAGER RUNNING

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerProject.threadExited("002028",this, thrown);
        }
    }


    /**
     * Description: Polls prepared chunks from chunksPendingGenerationQueue
     */
    private void workerThreadGenerateClusters()
    {
        Throwable thrown = null;

        final Predicate<ManagedOreClusterChunk> IS_ADJACENT_CHUNKS_LOADED = chunk -> {
            ChunkPos pos = HBUtil.ChunkUtil.getPos(chunk.getId());
            //give me a nested for loop over x, z coordinates from -1 to 1
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    ChunkPos adjPos = new ChunkPos(pos.x + x, pos.z + z);
                    if (!loadedOreClusterChunks.containsKey(ChunkUtil.getId(adjPos)))
                        return false;
                }
            }
            return true;
        };

        try
        {

            while( managerRunning )
            {
                Queue<ManagedOreClusterChunk> chunksToGenerate = chunksPendingGeneration.values().stream()
                    .filter(ManagedOreClusterChunk::isCleaned)
                    .filter(chunk -> chunk.hasClusters())
                    .filter(IS_ADJACENT_CHUNKS_LOADED)
                    .collect(Collectors.toCollection(LinkedList::new));

                if( chunksToGenerate.size() == 0 ) {
                    sleep(10);
                    continue;
                }

                for (ManagedOreClusterChunk chunk : chunksToGenerate)
                {
                    editManagedChunk(chunk, this::handleClusterGeneration);
                    if( ManagedOreClusterChunk.isPregenerated(chunk) )
                        chunksPendingGeneration.remove(chunk.getId());
                }


            }

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerProject.threadExited("002007",this, thrown);
        }
    }
    //END workerThreadGenerateClusters

    private void workerThreadEditChunk()
    {
        Throwable thrown = null;

        try
        {
            if( true ) {
                //sleep(10000);
                //return;
            }

            while( managerRunning )
            {
                //sleep(1000);
                //Sleep if loaded chunks is empty, else iterate over them
                if( loadedOreClusterChunks.isEmpty() )
                {
                    sleep(10);
                    continue;
                }

                handleChunkReadiness();

                List<ManagedOreClusterChunk> readyChunks = loadedOreClusterChunks.values().stream()
                    .filter(c -> c.isReady())
                    .toList();

                for( ManagedOreClusterChunk chunk : readyChunks )
                {
                    Queue<Pair<Block, BlockPos>> blockUpdates = chunk.getBlockStateUpdates();
                    if( !chunk.isReady() || blockUpdates == null || blockUpdates.size() == 0 )
                        continue;

                    //LoggerProject.logDebug("002029.1","Editing chunk: " + chunk.getId() + " with " + blockUpdates.size() + " updates");
                    editManagedChunk(chunk, c -> {
                        LevelChunk levelChunk = c.getChunk(false);
                        boolean isSuccessful = ManagedChunk.updateChunkBlocks(levelChunk, c.getBlockStateUpdates());
                        if( isSuccessful ) {
                            c.setStatus(ManagedOreClusterChunk.ClusterStatus.GENERATED);
                            c.setReady(false);
                        }
                    });

                }

                //sleep(10000);   //10 seconds
            }

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerProject.threadExited("002031",this, thrown);
        }
    }


    private void handleChunkReadiness()
    {
        Throwable thrown = null;
        try
        {
            if( true ) {
                //sleep(10000);
                //return;
            }
            //sleep(1000);

            //while( managerRunning )
            {


                List<ManagedOreClusterChunk> availableChunks = loadedOreClusterChunks.values().stream()
                    .filter(c -> c.hasChunk() && c.getChunk(false).getStatus() == ChunkStatus.FULL)
                    .filter(c -> !c.isReady())
                    .toList();

                //Cleaned chunks that have not been harvested yet
                List<ManagedOreClusterChunk> cleanedChunks = availableChunks.stream()
                    .filter(c -> ManagedOreClusterChunk.isCleaned(c) )
                    .filter(c -> !c.checkClusterHarvested())    //Checks if cluster has been interacted with by player
                    .filter(c -> c.hasChunk())                 //must have loaded chunk
                    .toList();

                 //Pre-generated chunks that have not been harvested yet
                List<ManagedOreClusterChunk> preGeneratedChunks = availableChunks.stream()
                    .filter(c -> ManagedOreClusterChunk.isPregenerated(c) )
                    .filter(c -> !c.isReady())
                    .filter(c -> !c.checkClusterHarvested())    //Checks if cluster has been interacted with by player
                    .filter(c -> c.hasChunk())                 //must have loaded chunk
                    .toList();

                //Any adjacentChunks to the player that are not harvested
                /*
                final List<ChunkAccess> PLAYER_CHUNKS = GeneralConfig.getPlayers();
                //Get location of each player
                //Get chunk at each location
                //Determine adjacent chunks
                //Put adjacent chunks into an array
                //Filter all chunks that have been harvested
                List<ManagedOreClusterChunk> adjacentChunks = loadedOreClusterChunks.values().stream()
                    .filter(c -> ManagedOreClusterChunk.isCleaned(c) )
                    .filter(c -> !c.isReady())
                    .filter(c -> !c.checkClusterHarvested())    //Checks if cluster has been interacted with by player
                    .filter(c -> c.hasChunk())                 //must have loaded chunk
                    .toList();

                */

                //Join Lists and mark as ready
                List<ManagedOreClusterChunk> readyChunks = new ArrayList<>();
                readyChunks.addAll(cleanedChunks);
                readyChunks.addAll(preGeneratedChunks);
                //readyChunks.addAll(adjacentChunks);

                readyChunks.forEach(c -> editManagedChunk(c, ch -> ch.setReady(true)) );
            }

        }
        catch (Exception e) {
            thrown = e;
        }
        finally {
            LoggerProject.threadExited("002031",this, thrown);
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
        ManagedOreClusterChunk managedChunk = loadedOreClusterChunks.get(chunkId);
        if( managedChunk == null || managedChunk.getChunk(false) == null )
            return;

        LevelChunk start = managedChunk.getChunk(false);
        LinkedHashSet<String> chunkIds = getBatchedChunkList(batchSize, start);
        long step1Time = System.nanoTime();
        //LoggerProject.logDebug("002008", "Queued " + chunkIds.size() + " chunks for cluster determination");


        //LoggerProject.logDebug("handlePrepareNewCluster #1  " + LoggerProject.getTime(startTime, step1Time) + " ms");


        Map<String, Map<Block, BlockPos>> clusters;
        clusters = oreClusterCalculator.calculateClusterLocations(chunkIds.stream().toList() , randSeqClusterPositionGen);
        long step2Time = System.nanoTime();
        //LoggerProject.logDebug("002009","Determined " + clusters.size() + " clusters in " + chunkIds.size() + " chunks");
        //LoggerProject.logDebug("handlePrepareNewCluster #3  " + LoggerProject.getTime(step1Time, step2Time) + " ms");


        // #3. Add clusters to determinedClusters
        for( String id: chunkIds)
        {
        //Create clusters for chunks that aren't loaded yet
            ManagedOreClusterChunk chunk = loadedOreClusterChunks.getOrDefault(id, ManagedOreClusterChunk.getInstance(level, id) );
            this.loadedOreClusterChunks.put(id, chunk);

            chunk.addClusterTypes(clusters.get(id));
            chunk.setStatus(ManagedOreClusterChunk.ClusterStatus.DETERMINED);
            this.chunksPendingCleaning.put(id, chunk);

        }
        LoggerProject.logDebug("002010","Added " + clusters.size() + " clusters to determinedChunks");

        long step3Time = System.nanoTime();
        //        //LoggerProject.logDebug("handlePrepareNewCluster #3  " + LoggerProject.getTime(step2Time, step3Time) + " ms");

        for( Map.Entry<String, Map<Block, BlockPos>> cluster : clusters.entrySet())
        {
            //Add chunkId to existingClustersByType Map
            LinkedHashSet<Block> oreTypesInThisCluster = new LinkedHashSet<>(cluster.getValue().keySet());
            existingClustersByType.forEach((type, set) -> {
                if (oreTypesInThisCluster.remove(type))
                    set.add(cluster.getKey());
            });

           // LoggerProject.logDebug("002012","determined oreTypesInThisCluster: " + oreTypesInThisCluster.size());


            if(oreTypesInThisCluster.size() == 0)
            {
                //LoggerProject.logDebug("002013","No new ore types in this cluster");
                continue;
            }


            //Ore types not yet added to the Aggregate List
            oreTypesInThisCluster.forEach(type -> {
                HashSet<String> set = new HashSet<>();
                set.add(cluster.getKey());
                existingClustersByType.put(type, set);
            });

           //LoggerProject.logDebug("002014","Added net new oreTypes to grand list, new size " + existingClustersByType.size());
        }


        long endTime = System.nanoTime();
        //LoggerProject.logDebug("handlePrepareNewCluster #4  " + LoggerProject.getTime(step3Time, endTime) + " ms");
    }

    /**
     * Step 2. Cleans the chunk by performing 3 distinct operations
     * 1. Scan the chunk for all cleanable ores
     * 2. Determine the cluster position for each ore in the managed chunk
     * 3. Determine which Ores need to be cleaned based on Ore Config data
     *
     * @param chunk
     */
    private void handleChunkCleaning(ManagedOreClusterChunk chunk)
    {

        if( chunk == null|| chunk.getChunk(false) == null )
            return;

        if( chunk.getChunk(false).getStatus() != ChunkStatus.FULL )
            return;

        //LoggerProject.logDebug("002025", "Cleaning chunk: " + chunk.getId());

        try {


            final Map<Block, OreClusterConfigModel> ORE_CONFIGS = config.getOreConfigs();

            final Set<Block> COUNTABLE_ORES = ORE_CONFIGS.keySet().stream().collect(Collectors.toSet());
            final Set<Block> CLEANABLE_ORES = ORE_CONFIGS.keySet().stream().filter(oreName -> {
                return ORE_CONFIGS.get(oreName).oreVeinModifier < 1.0f;
            }).collect(Collectors.toSet());


            //1. Scan chunk for all cleanable ores, testing each block
            oreClusterCalculator.cleanChunkFindAllOres(chunk, COUNTABLE_ORES);

            //2. Determine the cluster position for each ore in the managed chunk
            if( chunk.hasClusters() )
            {
                oreClusterCalculator.cleanChunkSelectClusterPosition(chunk);
                this.chunksPendingGeneration.put(chunk.getId(), chunk);
                this.threadPoolClusterGenerating.submit(this::workerThreadGenerateClusters);
            }

            //3. Determine which Ore Vertices need to be cleaned
            oreClusterCalculator.cleanChunkDetermineBlockPosToClean(chunk, CLEANABLE_ORES);

            //4. Set the originalOres array to null to free up memory
            chunk.setOriginalOres(null);

            //5. Set the chunk status to CLEANED
            chunk.setStatus(ManagedOreClusterChunk.ClusterStatus.CLEANED);

            //LoggerProject.logError("002027", "Cleaning chunk: " + chunk.getId() + " complete");

    }
    catch(Exception e) {
        StringBuilder error = new StringBuilder();
        error.append("Error cleaning chunk: ");
        error.append(chunk.getId());
        error.append(" name | message: ");
        error.append(e.getClass());
        error.append(" | ");
        error.append(e.getMessage());
        error.append(" stacktrace: \n");
        error.append(Arrays.stream(e.getStackTrace()).toList().toString());
        LoggerProject.logError("002027.1", error.toString());
    }

    }
    //END handleCleanClusters


    /**
     * Takes a ManagedOreClusterChunk and generates a sequence of positions
     * that will become the ore cluster in the world. These positions are
     * added to ManagedOreClusterChunk::blockStateUpdates
     *
     * @param chunk
     */
    private void handleClusterGeneration(ManagedOreClusterChunk chunk)
    {
        if( chunk == null || chunk.getChunk(false) == null )
            return;

        if(chunk.getClusterTypes() == null || chunk.getClusterTypes().size() == 0)
            return;



        LoggerProject.logDebug("002015","Generating clusters for chunk: " + chunk.getId());

        String SKIPPED = null;
        for( Block oreType : chunk.getClusterTypes().keySet() )
        {
            //1. Get the cluster config

            BlockPos sourcePos = chunk.getClusterTypes().get(oreType);
            if( sourcePos == null ) {
                LoggerProject.logDebug("002016","No source position for oreType: " + oreType);
                SKIPPED = BlockUtil.blockToString(oreType);
                continue;
            }


            List<BlockPos> clusterPos = oreClusterCalculator.generateCluster(Pair.of(oreType, sourcePos));
            clusterPos.forEach( pos -> chunk.addBlockStateUpdate(oreType, pos) );
        }

        if( SKIPPED == null )
            chunk.setStatus(ManagedOreClusterChunk.ClusterStatus.PREGENERATED);
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


    /**
     * Batch process that determines the location of clusters in the next n chunks
     * Chunk cluster determinations are made spirally from the 'start' chunk, up, right, down, left
     */
    private LinkedHashSet<String> getBatchedChunkList(int batchSize, ChunkAccess start)
    {
        LinkedHashSet<String> chunkIds = new LinkedHashSet<>();
        ChunkGenerationOrderHandler chunkIdGeneratorHandler = mainSpiral;
        if (chunksPendingCleaning.size() > Math.pow(ModRealTimeConfig.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
            chunkIdGeneratorHandler = new ChunkGenerationOrderHandler(start.getPos());
        }

        for (int i = 0; i < batchSize; i++) {
            ChunkPos next = chunkIdGeneratorHandler.getNextSpiralChunk();
            chunkIds.add(ChunkUtil.getId(next));
        }

        return chunkIds;
    }

    /** GETTERS AND SETTERS **/

        /** GETTERS **/

        public ManagedOreClusterChunk getLoadedChunk(String chunkId) {
            return loadedOreClusterChunks.get(chunkId);
        }

        /** SETTERS **/



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
        if (chunksPendingCleaning.size() < Math.pow(ModRealTimeConfig.ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE, 2)) {
            return chunksPendingCleaning.values().stream().map(ManagedOreClusterChunk::getId).
            collect(Collectors.toCollection(LinkedHashSet::new));
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
     * Returns an empty optional if the chunk is locked or null is passed
     * @param chunk
     * @param consumer
     * @return
     */
    @ThreadSafe
    private synchronized Optional<ManagedOreClusterChunk> editManagedChunk(ManagedOreClusterChunk chunk, Consumer<ManagedOreClusterChunk> consumer)
    {
        if (chunk == null)
            return Optional.empty();

        if( chunk.getLock().isLocked() )
            return Optional.empty();

        synchronized (chunk)
        {
            chunk.getLock().lock();
            consumer.accept(chunk);
            chunk.getLock().unlock();
        }

        return Optional.ofNullable(chunk);
    }



    public void shutdown()
    {
        managerRunning = false;
        threadPoolLoadedChunks.shutdown();
        threadPoolClusterDetermination.shutdown();
        threadPoolClusterCleaning.shutdown();
        threadPoolClusterGenerating.shutdown();
        threadPoolChunkEditing.shutdown();
    }

    /** STATIC METHODS **/

    public static void onChunkLoad(ChunkEvent.Load event, ManagedOreClusterChunk managedChunk)
    {
        LevelAccessor level = event.getLevel();
        if( level !=null && level.isClientSide() ) {
            //Client side
        }
        else {
            OreClusterManager manager = OreClustersAndRegenMain.ORE_CLUSTER_MANAGER_BY_LEVEL.get( level );
            if( manager != null ) {
                manager.handleChunkLoaded(event.getChunk(), managedChunk);
            }
        }
    }
    //END onChunkLoad

    public static void onChunkUnload(ChunkEvent.Unload event)
    {
        LevelAccessor level = event.getLevel();
        if( level !=null && level.isClientSide() ) {
            //Client side
        }
        else {
            OreClusterManager manager = OreClustersAndRegenMain.ORE_CLUSTER_MANAGER_BY_LEVEL.get( level );
            if( manager != null ) {
                manager.handleChunkUnloaded(event.getChunk());
            }
        }
    }


    /** ############### **/


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
