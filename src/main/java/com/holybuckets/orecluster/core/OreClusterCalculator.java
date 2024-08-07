package com.holybuckets.orecluster.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.stream.Collectors;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import com.holybuckets.foundation.HolyBucketsUtility.*;
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.RealTimeConfig;
import com.holybuckets.orecluster.RealTimeConfig;

public class OreClusterCalculator {

    private OreClusterManager manager;
    private RealTimeConfig C;
    private LinkedHashSet exploredChunks;
    private ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters;
    private ConcurrentHashMap<String, HashSet<String>> existingClustersByType;



     //Constructor
    public OreClusterCalculator( final OreClusterManager manager)
    {

        this.manager = manager;
        this.C = manager.getConfig();
        this.exploredChunks = manager.getExploredChunks();
        this.existingClusters = manager.getExistingClusters();
        this.existingClustersByType = manager.getExistingClustersByType();

    }

    public HashMap<String, HashMap<String, Vec3i>> calculateClusterLocations(List<ChunkAccess> chunks, Random rng)
    {

        // Get list of all ore cluster types
        Map<String, OreClusterConfigModel> clusterConfigs = C.getOreConfigs();
        List<String> oreClusterTypes = new ArrayList<>(clusterConfigs.keySet());

        HashMap<String, Integer> clusterCounts = new HashMap<>();
        LoggerBase.logDebug("1. Obtained cluster configs for ores: ");
        LoggerBase.logDebug(clusterConfigs.toString());

        //Determine the expected total for each cluster type for this MAX_CLUSTERS batch
        // Use a normal distribution to determine the number of clusters for each type
        for (String oreType : oreClusterTypes)
        {
            int normalizedSpawnRate = clusterConfigs.get(oreType).oreClusterSpawnRate;
            double sigma = RealTimeConfig.CHUNK_DISTRIBUTION_STDV_FUNC.apply(normalizedSpawnRate);
            int numClusters = (int) Math.round(rng.nextGaussian() * sigma + normalizedSpawnRate);

            clusterCounts.put(oreType, numClusters);
        }
        LoggerBase.logDebug("2. Determined cluster counts for each ore type: ");
        LoggerBase.logDebug(clusterCounts.toString());

        /** Add all clusters, distributing one cluster type at a time
        *
         *  Summarize the below implementation
         *  1. Get list of ids for recently (previously) loaded chunks, we need to check which have clusters so we don't place new ones too close!
         *  2. Build 2D Array of chunk positions that serves as a map identifying where current and previous clusters are
         *  3. Determine distribution of clusters as aggregate group over all chunks
         *      - e.g. 47 clusters over 256 chunks ~ 256/47 = 5.4 chunks per each cluster
         *      - Gaussian distribute the clusters between prevClusterPosition + ~N(5.4, minSpacing/3)
         *
         *   4. Using the Map of aggregate clusters, pick chunks for each cluster type
         */

         //1. Get recently loaded chunks
         String startChunk = ChunkUtil.getId( chunks.get(0) );
         int minSpacing = C.getDefaultConfig().minChunksBetweenOreClusters;

        /** If the spacing is large, there will be fewer cluster chunks, so we can check all against
        *   all existing clusters instead of calculating the area around each chunk
        *   If the spacing is small, we will have many cluster chunks, better to check the radius
         */
         final int MIN_SPACING_VALIDATOR_CUTOFF_RADIUS = Math.min( existingClusters.size(), (int) Math.pow(minSpacing, 2) );
         LinkedHashSet<String> chunksInRadiusOfStart = getChunkIdsInRadius(startChunk,
          Math.min( minSpacing, MIN_SPACING_VALIDATOR_CUTOFF_RADIUS ));
         String closestToCenter = chunksInRadiusOfStart.stream().min(Comparator.comparingInt( c ->
             Math.round( ChunkUtil.chunkDist( c, "0,0" ) )
         )).get();


        //2. Determine area needed for spiral generation of recent chunks
         int batchDimensions = (int) Math.ceil( Math.sqrt( chunks.size() ) );
         int spiralRadius = batchDimensions + MIN_SPACING_VALIDATOR_CUTOFF_RADIUS;
         int spiralArea = (int) Math.pow( spiralRadius, 2 );
         LinkedHashSet<String> recentlyLoadedChunks =
            manager.getRecentChunkIds( chunks.get(0).getPos(), spiralArea );
        LinkedHashSet<String> localExistingClusters = existingClusters.keySet().stream().
            collect(Collectors.toCollection(LinkedHashSet::new));

         if( !existingClusters.isEmpty() )
         {

             int minX = recentlyLoadedChunks.stream().mapToInt( c -> ChunkUtil.getPos(c).x ).min().getAsInt();
             int minZ = recentlyLoadedChunks.stream().mapToInt( c -> ChunkUtil.getPos(c).z ).min().getAsInt();
             int maxX = recentlyLoadedChunks.stream().mapToInt( c -> ChunkUtil.getPos(c).x ).max().getAsInt();
             int maxZ = recentlyLoadedChunks.stream().mapToInt( c -> ChunkUtil.getPos(c).z ).max().getAsInt();

             //Filter existing clusters by only clusters within radius of minX, minZ, maxX, maxZ
             localExistingClusters.removeIf( c ->
                 ChunkUtil.getPos(c).x < minX ||
                     ChunkUtil.getPos(c).x > maxX ||
                     ChunkUtil.getPos(c).z < minZ ||
                     ChunkUtil.getPos(c).z > maxZ );
         }

        //3. Determine distribution of clusters as aggregate group over all chunks
        float totalClusters = clusterCounts.values().stream().mapToInt( i -> i ).sum();
        float chunksPerCluster = chunks.size() / totalClusters;
        float stdDev = Math.max( (chunksPerCluster - minSpacing) / 3, 0);

        List<String> chunksToBePopulated = new LinkedList<>();  //may contain duplicates


        int chunkIndex = 0;
        while( chunkIndex < chunks.size() )
        {
            chunkIndex = (int) Math.round(rng.nextGaussian() * stdDev + chunksPerCluster) + chunkIndex;
            boolean openSpaceForCluster = false;
            while ( !openSpaceForCluster && chunkIndex < chunks.size() )
            {
                openSpaceForCluster = true;

                if( minSpacing == 0)
                    continue;

                /**
                 * In later stages of cluster generation, there will be overlap between
                 * the area we are attempting to generate and already populated clusters
                 * we tacitly "accept" a cluster in any chunk that already exists;
                 * these clusters, once assigned a particular ore type, will be discarded later
                 */

                String chunkId = ChunkUtil.getId(chunks.get(chunkIndex++));
                if( exploredChunks.contains(chunkId) )
                    continue;

                /**
                 * If the radius within we are placing clusters is reasonably small, we can
                 * check each space within the radius to see if it is occupied by a cluster
                 *  ELSE
                 * The radius is too large and we will check all against all existing clusters instead
                 */

                if( minSpacing < MIN_SPACING_VALIDATOR_CUTOFF_RADIUS )
                {

                    //Now we found a chunk where we randomly want to place a cluster, check 2D array to check validity
                    LinkedHashSet<String> nearbyChunks = getChunkIdsInRadius(chunkId, minSpacing);
                    for( String nearbyChunk : nearbyChunks ) {
                        if( localExistingClusters.contains(nearbyChunk) ) {
                            openSpaceForCluster = false;
                            break;
                        }
                    }
                }
                else
                {
                    if( localExistingClusters.stream().anyMatch( c ->
                        ChunkUtil.chunkDist( c, chunkId ) < minSpacing
                    )) {
                        openSpaceForCluster = false;
                    }

                }

            }
            //FOUND A VALID CHUNK, PLACE A CLUSTER
            if( chunkIndex < chunks.size() )
            {
                String chunkId = ChunkUtil.getId(chunks.get(chunkIndex));
                chunksToBePopulated.add(chunkId);
                localExistingClusters.add(chunkId);
            }

        }
        //END cluster placing algorithm
        LoggerBase.logDebug("Cluster Placement Algorithm Complete");
        LoggerBase.logDebug(chunksToBePopulated.toString());


        //4. Using the Map of aggregate clusters, pick chunks for each cluster type

        // Maps <ChunkId, <OreType, Vec3i>>
        HashMap<String, HashMap<String, Vec3i>> clusterPositions = new HashMap<>();

        //Order OreCluster types by spawnRate ascending
        oreClusterTypes.sort(Comparator.comparingInt( o -> clusterConfigs.get(o).oreClusterSpawnRate ));
        LinkedHashSet<String> selectedChunks = new LinkedHashSet<>();

        /**
         * Iterate over all ore types we want to place clusters for
         * 1. Obtain configs for the ore type
         * 2. Get all existing chunks in the world with the ore cluster type
         * 3. Remove all chunks from this list that are not in the local area
         *  (SKIP THIS STEP IN CASE OF LARGE RANGES)
         * 4. Remove set of available chunks that were selected from previous ore
         * 5. Initialize index variables for traversing "chunksTobePopulated"
         * 6. Calculate the nextIndex over all chunks to be populated
         * 7. If suggested "chunksToBePopulated.get(index)" chunk is already explored, skip it
         *      - We skip by autimatically accepting it, this cluster will be discarded later since this
         *      area was already assessed for chunks.
         * 8. Pop chunks from the list until we find a valid chunk after nextIndex
         *      - Validate no other cluster of the same type is within minSpacing
         * 9. Place the cluster by adding it to clusterPositions and selectedChunks
         *
         */
         try {


             for (String oreType : oreClusterTypes) {
                 OreClusterConfigModel config = clusterConfigs.get(oreType);
                 HashSet<String> allChunksWithClusterType = existingClustersByType.get(oreType);
                 //allChunksWithClusterType.removeIf( c -> !localExistingClusters.contains(c) );
                 final int MIN_SPACING_SPECIFIC_CLUSTER_VALIDATOR_CUTOFF_RADIUS = Math.min(allChunksWithClusterType.size(),
                     (int) Math.pow(config.minChunksBetweenOreClusters, 2));
                 LoggerBase.logDebug("Validating clusters for ore type: " + oreType);
                 LoggerBase.logDebug("Existing clusters for this ore type: " + allChunksWithClusterType.size());
                 LoggerBase.logDebug("Min spacing for this ore type: " + MIN_SPACING_SPECIFIC_CLUSTER_VALIDATOR_CUTOFF_RADIUS);


                 int totalSpecificClusters = clusterCounts.get(oreType);
                 if( totalSpecificClusters == 0 )
                     continue;
                 LoggerBase.logDebug("Total clusters for this ore type: " + totalSpecificClusters);

                 int clustersPlaced = 0;
                 int specificMinSpacing = config.minChunksBetweenOreClusters;

                 //removes all chunks that were selected by previous ores, removes item from hashset so duplicates are left
                 Iterator<String> it = chunksToBePopulated.iterator();
                 while (it.hasNext()) {
                     String chunkId = it.next();
                     if (selectedChunks.remove(chunkId)) {
                         it.remove();
                     }
                 }
                 LinkedList<String> chunksToBePopulatedSpecificCopy = new LinkedList<>(chunksToBePopulated);
                 float incrementByBuckets = chunksToBePopulatedSpecificCopy.size() / totalSpecificClusters;
                 boolean validCluster = false;
                 int nextIndex = 0;
                 int subIndex = 0;    //properly tracks next chunksId to be used
                 int nextIndexBalancer = 0;  //balances the jump of the next index considering how many cluster conflicts we encountered
                 int failedChunksCount = 0;

                 while (clustersPlaced < totalSpecificClusters) {
                     clustersPlaced++;
                     float gaussianIncrementFactor = (float) rng.nextGaussian(incrementByBuckets, incrementByBuckets / 6);
                     nextIndex = (int) (clustersPlaced * gaussianIncrementFactor + nextIndexBalancer);
                     nextIndex = Math.max(subIndex + 1, nextIndex);
                     failedChunksCount = 0;
                     String candidateChunkId = null;
                     validCluster = false;

                     while (!validCluster && !chunksToBePopulatedSpecificCopy.isEmpty())
                     {

                         while (subIndex < nextIndex && !chunksToBePopulatedSpecificCopy.isEmpty()) {
                             candidateChunkId = chunksToBePopulatedSpecificCopy.removeFirst();
                             subIndex++;
                         }

                         if( chunksToBePopulatedSpecificCopy.isEmpty() )
                             break;


                         if (exploredChunks.contains(candidateChunkId)) {
                             continue;
                         }

                         //Check if the chunk is within the radius of a chunk with the same cluster type
                         validCluster = true;
                         if (specificMinSpacing < MIN_SPACING_SPECIFIC_CLUSTER_VALIDATOR_CUTOFF_RADIUS) {
                             LinkedHashSet<String> nearbyChunks = getChunkIdsInRadius(candidateChunkId, specificMinSpacing);
                             for (String nearbyChunk : nearbyChunks) {
                                 if (allChunksWithClusterType.contains(nearbyChunk)) {
                                     validCluster = false;
                                     break;
                                 }
                             }
                         } else {
                             final String id = candidateChunkId;
                             if (allChunksWithClusterType.stream().anyMatch(c ->
                                 ChunkUtil.chunkDist(c, id) < specificMinSpacing)) {
                                 validCluster = false;
                             }
                         }

                         if (!validCluster) {
                             failedChunksCount++;
                             nextIndex++;
                         }

                     }
                     //END WHILE FIND VALID CHUNK FOR GIVEN CLUSTER
                     nextIndexBalancer += failedChunksCount;
                     if (failedChunksCount == 0)
                         nextIndexBalancer = 0; //Cluster was valid on first attempt, reset balancer

                     //PLACE THE CLUSTER
                     if (validCluster && candidateChunkId != null)
                     {
                         selectedChunks.add(candidateChunkId);
                         allChunksWithClusterType.add(candidateChunkId);
                         if (clusterPositions.containsKey(candidateChunkId))
                         {
                             clusterPositions.get(candidateChunkId).put(oreType, null);
                         }
                         else
                         {
                             HashMap<String, Vec3i> clusterMap = new HashMap<>();
                             clusterMap.put(oreType, null);
                             clusterPositions.put(candidateChunkId, clusterMap);
                         }
                     }

                 }
                 //END WHILE PLACE ALL CLUSTERS OF THIS TYPE


             }
             //END FOR EACH ORE TYPE
         }
         catch (Exception e)
         {
            StringBuilder sb = new StringBuilder();
            clusterCounts.entrySet().forEach( ore -> sb.append(ore.getKey() + ": " + ore.getValue() + ", "));
            LoggerBase.logWarning("Unable to place all ore clusters: " + e.getMessage() + "Remaining Counts: " + sb.toString());
             e.printStackTrace();
         }

        //6. Remove all clusters at chunks that were populated in previous batches
        clusterPositions.keySet().removeAll( exploredChunks );

        return clusterPositions;
    }


    /**
     * Get a list of chunk ids within a given radius of a chunk - radius of 0 is itself
      * @param chunkId
     * @param radius
     * @return
     */
    private LinkedHashSet<String> getChunkIdsInRadius( String chunkId, int radius )
    {
        LinkedHashSet<String> chunks = new LinkedHashSet<>();
        ChunkPos center = ChunkUtil.getPos(chunkId);
        for( int x = center.x - radius; x <= center.x + radius; x++ )
        {
            for( int z = center.z - radius; z <= center.z + radius; z++ )
            {
                chunks.add( ChunkUtil.getId(x, z) );
            }
        }
        return chunks;
    }

}
//END CLASS

