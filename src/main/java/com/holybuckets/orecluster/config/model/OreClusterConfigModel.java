package com.holybuckets.orecluster.config.model;

import org.antlr.v4.runtime.misc.Triple;

//Java
import java.util.Arrays;
import java.util.HashSet;

import com.holybuckets.orecluster.config.COreClusters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.antlr.v4.runtime.misc.Triple;

public class OreClusterConfigModel {

    public String oreClusterType;
    public Integer baseOreClusterSpawnRate = COreClusters.BASE_ORE_CLUSTER_SPAWN_RATE;
    public Triple<Integer, Integer, Integer> baseOreClusterVolume = processVolume(COreClusters.BASE_ORE_CLUSTER_VOLUME);
    public Float baseOreClusterDensity = COreClusters.BASE_ORE_CLUSTER_DENSITY;
    public String baseOreClusterShape = COreClusters.BASE_ORE_CLUSTER_SHAPE;
    public Integer oreClusterMaxYLevelSpawn = COreClusters.ORE_CLUSTER_MAX_Y_LEVEL_SPAWN;
    public Integer minChunksBetweenOreClusters = COreClusters.MIN_CHUNKS_BETWEEN_ORE_CLUSTERS;
    public Integer maxChunksBetweenOreClusters = COreClusters.MAX_CHUNKS_BETWEEN_ORE_CLUSTERS;
    public String oreClusterConfigModelConstructionErrors;

    private static final Gson gson = new GsonBuilder().create();

    public String serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("oreClusterType", oreClusterType);
        jsonObject.addProperty("baseOreClusterSpawnRate", baseOreClusterSpawnRate);
        jsonObject.addProperty("baseOreClusterVolume", baseOreClusterVolume.a
                + "x" + baseOreClusterVolume.b
                + "x" + baseOreClusterVolume.c
        );
        jsonObject.addProperty("baseOreClusterDensity", baseOreClusterDensity);
        jsonObject.addProperty("baseOreClusterShape", baseOreClusterShape);
        jsonObject.addProperty("oreClusterMaxYLevelSpawn", oreClusterMaxYLevelSpawn);
        jsonObject.addProperty("minChunksBetweenOreClusters", minChunksBetweenOreClusters);
        jsonObject.addProperty("maxChunksBetweenOreClusters", maxChunksBetweenOreClusters);
        System.err.println("jsonObject: " + jsonObject);
        return gson.toJson(jsonObject).
                //replace("\",", "\"," + System.getProperty("line.separator") ).
                replace('"', "'".toCharArray()[0]);
    }

    public void deserialize(String jsonString) {
        StringBuilder errorBuilder = new StringBuilder();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        try {
            oreClusterType = jsonObject.get("oreClusterType").getAsString();
        } catch (Exception e) {
            errorBuilder.append("Error parsing oreClusterType: ").append(e.getMessage()).append("\n");
        }

        try {
            baseOreClusterSpawnRate = jsonObject.get("baseOreClusterSpawnRate").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing baseOreClusterSpawnRate: ").append(e.getMessage()).append("\n");
        }

        try {
            baseOreClusterVolume = processVolume(jsonObject.get("baseOreClusterVolume").getAsString());
        } catch (Exception e) {
            errorBuilder.append("Error parsing baseOreClusterVolume: ").append(e.getMessage()).append("\n");
        }

        try {
            baseOreClusterDensity = jsonObject.get("baseOreClusterDensity").getAsFloat();
        } catch (Exception e) {
            errorBuilder.append("Error parsing baseOreClusterDensity: ").append(e.getMessage()).append("\n");
        }

        try {
            baseOreClusterShape = jsonObject.get("baseOreClusterShape").getAsString();
        } catch (Exception e) {
            errorBuilder.append("Error parsing baseOreClusterShape: ").append(e.getMessage()).append("\n");
        }

        try {
            oreClusterMaxYLevelSpawn = jsonObject.get("oreClusterMaxYLevelSpawn").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing oreClusterMaxYLevelSpawn: ").append(e.getMessage()).append("\n");
        }

        try {
            minChunksBetweenOreClusters = jsonObject.get("minChunksBetweenOreClusters").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing minChunksBetweenOreClusters: ").append(e.getMessage()).append("\n");
        }

        try {
            maxChunksBetweenOreClusters = jsonObject.get("maxChunksBetweenOreClusters").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing maxChunksBetweenOreClusters: ").append(e.getMessage()).append("\n");
        }

        oreClusterConfigModelConstructionErrors = errorBuilder.toString();
    }
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


}

