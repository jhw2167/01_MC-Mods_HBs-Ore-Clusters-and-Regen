package com.holybuckets.orecluster;

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.config.AllConfigs;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkAccess;

//Java Imports
import net.minecraft.world.level.levelgen.blending.Blender;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
    public static  RealTimeConfig config;
    public static ConcurrentHashMap<String, HashMap<String, Vec3i>> existingClusters = new ConcurrentHashMap<>();
    public static String lastDeterminedClusterChunkId = "";

    /** Constructor **/
    public OreClusterManager() {
        init();
    }

    /** Behavior **/
    public static void init() {

        //1. Starting at worldspawn, read chunk NBT data of all existing chunks to determine if they own a cluster
        //2. If the chunk owns a cluster, add it to the existingClusters map

        Vec3i worldSpawn = AllConfigs.WORLD_SPAWN;

    }

    /**
     * Handle newly loaded chunk
     */
    public static void handleClustersForChunk( ChunkAccess c ) {
        //If else series
    }

    /**
     * Batch process that determines the location of clusters in the next n chunks
     * @param n
     */
    public static void determineClusterLocations(int n) {

    }

    /**
     * Determines how to handle the newly loaded chunk upon loading
     * @param chunk
     */
    public static void onChunkLoad(ChunkAccess chunk)
    {
        LoggerBase.logInfo("Chunk loaded: " + chunk.getPos());
        handleClustersForChunk( chunk );
    }



    public static void onChunkUnload(ChunkAccess chunk) {

    }
}
