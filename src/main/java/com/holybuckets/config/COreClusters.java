package com.holybuckets.config;

import com.holybuckets.foundation.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class COreClusters extends ConfigBase {

    public final ConfigInt baseOreClusterSpawnRate;
    public final ConfigString baseOreClusterVolume;
    public final ConfigFloat baseOreClusterDensity;
    public final ConfigString baseOreClusterShape;
    public final ConfigInt oreClusterMaxYLevelSpawn;
    public final ConfigInt minChunksBetweenOreClusters;
    public final ConfigInt maxChunksBetweenOreClusters;
    public final ConfigFloat baseOreVeinModifier;
    public final ConfigString oreClusterReplaceableBlocks;
    public final ConfigString oreClusterReplaceableEmptyBlock;
    public final ConfigBool regenerateOreClusters;
    public final ConfigList<OreClusterConfig> oreClusters;

    public COreClusters() {
        baseOreClusterSpawnRate = i(1, 0, 96, "baseOreClusterSpawnRate", Comments.BASE_ORE_CLUSTER_SPAWN_RATE);
        baseOreClusterVolume = s("32x32x32", "baseOreClusterVolume", Comments.BASE_ORE_CLUSTER_VOLUME);
        baseOreClusterDensity = f(0.60f, 0, 1, "baseOreClusterDensity", Comments.BASE_ORE_CLUSTER_DENSITY);
        baseOreClusterShape = s("bowl", "baseOreClusterShape", Comments.BASE_ORE_CLUSTER_SHAPE);
        oreClusterMaxYLevelSpawn = i(64, -64, 1024, "oreClusterMaxYLevelSpawn", Comments.ORE_CLUSTER_MAX_Y_LEVEL_SPAWN);
        minChunksBetweenOreClusters = i(0, 0, 96, "minChunksBetweenOreClusters", Comments.MIN_CHUNKS_BETWEEN_ORE_CLUSTERS);
        maxChunksBetweenOreClusters = i(9, 9, 96, "maxChunksBetweenOreClusters", Comments.MAX_CHUNKS_BETWEEN_ORE_CLUSTERS);
        baseOreVeinModifier = f(1f, 0, 10, "baseOreVeinModifier", Comments.BASE_ORE_VEIN_MODIFIER);
        oreClusterReplaceableBlocks = s("stone,cobblestone,endStone,woodenPlanks,andesite", "oreClusterReplaceableBlocks", Comments.ORE_CLUSTER_REPLACEABLE_BLOCKS);
        oreClusterReplaceableEmptyBlock = s("air", "oreClusterReplaceableEmptyBlock", Comments.ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK);
        regenerateOreClusters = b(true, "regenerateOreClusters", Comments.REGENERATE_ORE_CLUSTERS);
        oreClusters = l(new ArrayList<>(), OreClusterConfig.class, "oreClusters", Comments.ORE_CLUSTERS);
    }

    @Override
    public String getName() {
        return "OreClusters";
    }

    public static class Comments {
        public static final String[] BASE_ORE_CLUSTER_SPAWN_RATE = {"Defines the initial spawn rate of ore clusters. The number of expected ore clusters per 96 chunks"};
        public static final String[] BASE_ORE_CLUSTER_VOLUME = {"Specifies the dimensions of the ore cluster. <X>x<Y>x<Z>. The true cluster will always be smaller than this box because it will choose a shape that roughly fits inside it"};
        public static final String[] BASE_ORE_CLUSTER_DENSITY = {"Determines the density of ore within a cluster"};
        public static final String[] BASE_ORE_CLUSTER_SHAPE = {"Defines the shape of the ore cluster. Defaults to none, which takes a random shape"};
        public static final String[] ORE_CLUSTER_MAX_Y_LEVEL_SPAWN = {"Maximum Y-level at which ore clusters can propagate"};
        public static final String[] MIN_CHUNKS_BETWEEN_ORE_CLUSTERS = {"Minimum number of chunks between ore clusters"};
        public static final String[] MAX_CHUNKS_BETWEEN_ORE_CLUSTERS = {"Maximum number of chunks between ore clusters"};
        public static final String[] BASE_ORE_VEIN_MODIFIER = {"Scales the presence of regular ore veins"};
        public static final String[] ORE_CLUSTER_REPLACEABLE_BLOCKS = {"List of blocks that can be replaced by the specified ore of the cluster during cluster generation"};
        public static final String[] ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK = {"Block used to fill in the ore cluster shape when we want the ore to be more sparse"};
        public static final String[] REGENERATE_ORE_CLUSTERS = {"Flag indicating if ore clusters should regenerate by default"};
    }

    public static class OreClusterConfig {
        public ConfigString ore;
        public ConfigFloat veinSpawningSpawnRateModifier;
        public ConfigString veinSpawningReplaceableEmptyBlock;
        public ConfigFloat clusterSpawningSpawnRate;
        public ConfigString clusterSpawningVolume;
        public ConfigString clusterSpawningDensity;
        public ConfigString clusterSpawningShape;
        public ConfigInt clusterSpawningMaxYLevelSpawn;
        public ConfigString clusterSpawningReplaceableEmptyBlock;

        public OreClusterConfig(COreClusters parent, String oreName) {
            ore = parent.s(oreName, "ore", "Specifies the type of ore in the cluster");
            veinSpawningSpawnRateModifier = parent.f(0.5f, 0, 1, "veinSpawningSpawnRateModifier", "Modifier for the spawn rate of veins");
            veinSpawningReplaceableEmptyBlock = parent.s("stone", "veinSpawningReplaceableEmptyBlock", "Block used to fill empty spaces in veins");
            clusterSpawningSpawnRate = parent.f(0.5f, 0, 1, "clusterSpawningSpawnRate", "Specifies the percentage chance of an ore being part of a cluster");
            clusterSpawningVolume = parent.s("32x32x32", "clusterSpawningVolume", "Dimensions of the ore cluster");
            clusterSpawningDensity = parent.s("60%", "clusterSpawningDensity", "Density of ore within the cluster");
            clusterSpawningShape = parent.s("bowl", "clusterSpawningShape", "Shape of the ore cluster");
            clusterSpawningMaxYLevelSpawn = parent.i(64, -64, 1024, "clusterSpawningMaxYLevelSpawn", "Maximum Y-level at which ore clusters can propagate");
            clusterSpawningReplaceableEmptyBlock = parent.s("stone", "clusterSpawningReplaceableEmptyBlock", "Block used to fill empty spaces in clusters");
        }
    }

    public void addOreCluster(String oreType) {
        oreClusters.add(new OreClusterConfig(this, oreType));
    }
}
