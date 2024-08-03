package com.holybuckets.orecluster;

import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkAccess;

import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import com.holybuckets.foundation.HolyBucketsUtility.*;

public class OreClusterCalculator {

    public ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters;


    /**
     * function determineOreClusters(worldSeed, chunkRangeStart):
     *     # Initialize random number generator with world seed and sub-seed
     *     rng = initializeRNG(worldSeed, subSeed)
     *
     *     # Get list of all ore cluster types
     *     oreClusterTypes = getOreClusterTypes()
     *
     *     # Initialize a 2D array to store cluster positions
     *     clusterPositions = [[] for _ in range(96)]
     *
     *     for oreType in oreClusterTypes:
     *         # Normalize spawn rate to 96 chunks
     *         normalizedSpawnRate = normalizeSpawnRate(oreType.oreClusterSpawnRate, 96)
     *
     *         # Generate a random number of clusters for this ore type
     *         numClusters = rng.getRandomInt(max(0, normalizedSpawnRate - 2), min(normalizedSpawnRate + 2, 96))
     *
     *         # Place clusters for this ore type
     *         placedClusters = 0
     *         attempts = 0
     *         while placedClusters < numClusters and attempts < 200:
     *             chunkIndex = rng.getRandomInt(0, 95)
     *
     *             # Check if the chunk is available and respects min/max chunk distance
     *             if isChunkAvailable(clusterPositions, chunkIndex, oreType.minChunksBetweenOreClusters, oreType.maxChunksBetweenOreClusters):
     *                 clusterPositions[chunkIndex].append(oreType)
     *                 placedClusters += 1
     *
     *             attempts += 1
     *
     *     return clusterPositions
     *
     * function isChunkAvailable(clusterPositions, chunkIndex, minDistance, maxDistance):
     *     # Check nearby chunks within minDistance and maxDistance
     *     for i in range(max(0, chunkIndex - maxDistance), min(96, chunkIndex + maxDistance + 1)):
     *         if len(clusterPositions[i]) > 0 and abs(i - chunkIndex) < minDistance:
     *             return false
     *     return true
     *
     * function normalizeSpawnRate(spawnRate, targetChunks):
     *     return (spawnRate * targetChunks) / 96
     *
     * # Main execution
     * worldSeed = getWorldSeed()
     * chunkRangeStart = getChunkRangeStart()
     * oreClusters = determineOreClusters(worldSeed, chunkRangeStart)
     *
     * # Process the results
     * for chunkIndex, clusters in enumerate(oreClusters):
     *     if len(clusters) > 0:
     *         print(f"Chunk {chunkRangeStart + chunkIndex} contains: {', '.join(cluster.oreClusterType for cluster in clusters)}")
     *
     *
     */

     //Constructor
    public OreClusterCalculator( ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters ) {
        this.existingClusters = existingClusters;
    }

    public HashMap<String, HashMap<String, Vec3i>> calculateClusterLocations(List<ChunkAccess> chunks, Random rng)
    {
        final RealTimeConfig C = OreClusterManager.config;
        final int MAX_CLUSTERS = RealTimeConfig.CHUNK_NORMALIZATION_TOTAL;

        // Get list of all ore cluster types
        Map<String, OreClusterConfigModel> clusters = C.oreConfigs;
        List<String> oreClusterTypes = new ArrayList<>(clusters.keySet());

        //Randomize the order of the oreClusterTypes
        Collections.shuffle(oreClusterTypes, rng);

        HashMap<String, Integer> clusterCounts = new HashMap<>();

        //Determine the expected total for each cluster type for this MAX_CLUSTERS batch
        // Use a normal distribution to determine the number of clusters for each type
        for (String oreType : oreClusterTypes)
        {
            int normalizedSpawnRate = clusters.get(oreType).oreClusterSpawnRate;
            double sigma = RealTimeConfig.CHUNK_DISTRIBUTION_STDV_FUNC.apply(normalizedSpawnRate);
            int numClusters = (int) Math.round(rng.nextGaussian() * sigma + normalizedSpawnRate);

            clusterCounts.put(oreType, numClusters);
        }

        /** Add all clusters, distributing one cluster type at a time
        *
         *  Summarize the below implementation
         *  - Create a hashmap to store the cluster positions
         *  - Create a hashmap to store the unused chunks by ore type - we want it to be O(1) to see which chunks have no clusters
         *
         *  - Iterate through the oreClusterTypes setting one cluster at a time 1 Diamond, 1 Gold, 1 Iron... etc
         *  - For each oreType, get a random chunk from the unusedChunksByOreType
         *  - Remove all chunks within minSpacing of this chunk (e.g. we want no other iron clusters to spawn within 3 chunks of this one)
         *  - Add the cluster to the chunk, positions or cluster within chunk defined later
         *
         */

        // Maps <ChunkId, <OreType, Vec3i>>
        HashMap<String, HashMap<String, Vec3i>> clusterPositions = new HashMap<>();
        HashMap<String, LinkedHashSet<Integer>> unusedChunksByOreType = new HashMap<>();
        HashMap<String, Integer> chunkIdToChunkReferenceIndex = new HashMap<>();

        for (String oreType : oreClusterTypes)
        {
            LinkedHashSet<Integer> arr = new LinkedHashSet<>();
            for (int i = 0; i < chunks.size(); i++) {
                arr.add(i);
                chunkIdToChunkReferenceIndex.put(ChunkUtil.getId(chunks.get(i)), i);
            }
            unusedChunksByOreType.put(oreType, arr );
        }

        //Add all clusters, distributing one cluster type at a time
        List<String> oreClusterTypesCopy = new ArrayList<>(oreClusterTypes);
        while( oreClusterTypesCopy.size() > 0 )
        {
            for (String oreType : oreClusterTypes)
            {
                //Decrease cluster count
                clusterCounts.put(oreType, clusterCounts.get(oreType) - 1);

                if (clusterCounts.get(oreType) == -1)
                {
                    oreClusterTypesCopy.remove(oreType);
                    continue;
                }
                else
                {
                   LinkedHashSet<Integer> unusedChunks = unusedChunksByOreType.get(oreType);

                     if( unusedChunks.size() == 0 ) {
                         continue;
                     }

                    //Get a random remaining chunk
                    int chunkIdReferenceIndex = rng.nextInt(unusedChunks.size());
                    Integer chunkIndex = unusedChunks.stream().skip(chunkIdReferenceIndex).findFirst().get();
                    int minSpacing = C.oreConfigs.get(oreType).minChunksBetweenOreClusters;

                    //Remove all chunks within minSpacing of this chunk
                    int j = -1*minSpacing;
                    while (j < C.oreConfigs.get(oreType).minChunksBetweenOreClusters) {
                        unusedChunks.remove(chunkIndex + j);
                        j++;
                    }

                    //Add the cluster to the chunk, positions or cluster within chunk defined later
                    ChunkAccess chunk = chunks.stream().skip(chunkIndex).findFirst().get();
                    HashMap<String, Vec3i> cluster = clusterPositions.get(ChunkUtil.getId(chunk));
                    if( cluster != null )
                    {
                        cluster.put( oreType, null );
                    }
                    else
                    {
                        cluster = new HashMap<>();
                        cluster.put( oreType, null );
                        clusterPositions.put( ChunkUtil.getId(chunk), cluster );
                    }

                }
            }
        }
        //END WHILE


        return clusterPositions;
    }
}
