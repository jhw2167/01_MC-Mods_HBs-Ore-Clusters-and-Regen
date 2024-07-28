package com.holybuckets.orecluster.config.model;

import org.antlr.v4.runtime.misc.Triple;

//Java
import java.util.Arrays;
import java.util.HashSet;

import com.holybuckets.orecluster.config.COreClusters;

public class OreClusterConfigModel {

    public String oreClusterType;
    public Integer baseOreClusterSpawnRate = COreClusters.BASE_ORE_CLUSTER_SPAWN_RATE;
    public Triple<Integer, Integer, Integer> baseOreClusterVolume = processVolume(COreClusters.BASE_ORE_CLUSTER_VOLUME);
    public Float baseOreClusterDensity = COreClusters.BASE_ORE_CLUSTER_DENSITY;
    public String baseOreClusterShape = COreClusters.BASE_ORE_CLUSTER_SHAPE;
    public Integer oreClusterMaxYLevelSpawn = COreClusters.ORE_CLUSTER_MAX_Y_LEVEL_SPAWN;
    public Integer minChunksBetweenOreClusters = COreClusters.MIN_CHUNKS_BETWEEN_ORE_CLUSTERS;
    public Integer maxChunksBetweenOreClusters = COreClusters.MAX_CHUNKS_BETWEEN_ORE_CLUSTERS;
    public Float baseOreVeinModifier = COreClusters.BASE_ORE_VEIN_MODIFIER;
    public HashSet<String> oreClusterReplaceableBlocks = processReplaceableBlocks(COreClusters.ORE_CLUSTER_REPLACEABLE_BLOCKS);
    public HashSet<String> oreClusterReplaceableEmptyBlock = processReplaceableBlocks(COreClusters.ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK);
    public Boolean regenerateOreClusters = COreClusters.REGENERATE_ORE_CLUSTERS;

    public OreClusterConfigModel(String oreClusterType) {
        this.oreClusterType = oreClusterType;
    }

    //Setup static methods to process oreClusterReplaceableBlocks and oreClusterReplaceableEmptyBlock
    public static HashSet<String> processReplaceableBlocks(String replaceableBlocks) {
        return new HashSet<>(Arrays.asList(replaceableBlocks.split(",")));
    }

    public static Triple<Integer, Integer, Integer> processVolume(String volume) {
        String[] volumeArray = volume.toLowerCase().split("x");
        if(volumeArray.length != 3) {
            volumeArray = COreClusters.BASE_ORE_CLUSTER_VOLUME.split("x");
        }
        return new Triple<>(Integer.parseInt(volumeArray[0]),
                Integer.parseInt(volumeArray[1]),
                Integer.parseInt(volumeArray[2]));
    }


    //use GSON library to serialize into JSON
    public String serialize() {
        return " 'jsonProperty' : '" + this.oreClusterType + "'";
    }

    //use GSON library to deserialize from JSON
    public void deserialize( String json) {

    }


}

