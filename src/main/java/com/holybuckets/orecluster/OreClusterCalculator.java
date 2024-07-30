package com.holybuckets.orecluster;

import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import net.minecraft.core.Vec3i;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class OreClusterCalculator {

    public HashMap<String, Pair<String, Vec3i>> existingClusters;


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

    public void calculateClusterLocations(List<String> chunks)
    {
        final RealTimeConfig C = OreClusterManager.config;
        final int MAX_CLUSTERS = RealTimeConfig.CHUNK_NORMALIZATION_TOTAL;
        final int BATCH_SIZE = RealTimeConfig.CLUSTER_BATCH_TOTAL;

        // Initialize random number generator with world seed and sub-seed
        Random rng = new Random( RealTimeConfig.CLUSTER_SEED );

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

        // Initialize a 2D array to store cluster positions
        List<Pair<String, Vec3i>> clusterPositions = new ArrayList<>();




    }
}
