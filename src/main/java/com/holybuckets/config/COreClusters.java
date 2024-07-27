package com.holybuckets.config;

import com.holybuckets.foundation.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class COreClusters extends ConfigBase {

    public final ConfigInt baseOreClusterSpawnRate = i(1, 0, 96, "baseOreClusterSpawnRate", Comments.BASE_ORE_CLUSTER_SPAWN_RATE);
    public final ConfigString baseOreClusterVolume = s("32x32x32", "baseOreClusterVolume", Comments.BASE_ORE_CLUSTER_VOLUME);
    public final ConfigFloat baseOreClusterDensity = f(0.60f, 0, 1, "baseOreClusterDensity", Comments.BASE_ORE_CLUSTER_DENSITY);
    public final ConfigString baseOreClusterShape = s("bowl", "baseOreClusterShape", Comments.BASE_ORE_CLUSTER_SHAPE);
    public final ConfigInt oreClusterMaxYLevelSpawn = i(64, -64, 1024, "oreClusterMaxYLevelSpawn", Comments.ORE_CLUSTER_MAX_Y_LEVEL_SPAWN);
    public final ConfigInt minChunksBetweenOreClusters = i(0, 0, 96, "minChunksBetweenOreClusters", Comments.MIN_CHUNKS_BETWEEN_ORE_CLUSTERS);
    public final ConfigInt maxChunksBetweenOreClusters = i(9, 9, 96, "maxChunksBetweenOreClusters", Comments.MAX_CHUNKS_BETWEEN_ORE_CLUSTERS);
    public final ConfigFloat baseOreVeinModifier = f(1f, 0, 10, "baseOreVeinModifier", Comments.BASE_ORE_VEIN_MODIFIER);
    public final ConfigString oreClusterReplaceableBlocks = s("stone,cobblestone,endStone,woodenPlanks,andesite", "oreClusterReplaceableBlocks", Comments.ORE_CLUSTER_REPLACEABLE_BLOCKS);
    public final ConfigString oreClusterReplaceableEmptyBlock = s("air", "oreClusterReplaceableEmptyBlock", Comments.ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK);
    public final ConfigBool regenerateOreClusters = b(true, "regenerateOreClusters", Comments.REGENERATE_ORE_CLUSTERS);
    public final ConfigList<OreClusterConfig> oreClusters = l(new ArrayList<>(), OreClusterConfig.class, "oreClusters", Comments.ORE_CLUSTERS);


    @Override
    public String getName() {
        return "OreClusters";
    }

    public static class Comments {
        public static final String BASE_ORE_CLUSTER_SPAWN_RATE = "Defines the initial spawn rate of ore clusters. The number of expected ore clusters per 96 chunks";
        public static final String BASE_ORE_CLUSTER_VOLUME = "Specifies the dimensions of the ore cluster. <X>x<Y>x<Z>. The true cluster will always be smaller than this box because it will choose a shape that roughly fits inside it";
        public static final String BASE_ORE_CLUSTER_DENSITY = "Determines the density of ore within a cluster";
        public static final String BASE_ORE_CLUSTER_SHAPE = "Defines the shape of the ore cluster. Defaults to none, which takes a random shape";
        public static final String ORE_CLUSTER_MAX_Y_LEVEL_SPAWN = "Maximum Y-level at which ore clusters can propagate";
        public static final String MIN_CHUNKS_BETWEEN_ORE_CLUSTERS = "Minimum number of chunks between ore clusters";
        public static final String MAX_CHUNKS_BETWEEN_ORE_CLUSTERS = "Maximum number of chunks between ore clusters";
        public static final String BASE_ORE_VEIN_MODIFIER = "Scales the presence of regular ore veins";
        public static final String ORE_CLUSTER_REPLACEABLE_BLOCKS = "List of blocks that can be replaced by the specified ore of the cluster during cluster generation";
        public static final String ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK = "Block used to fill in the ore cluster shape when we want the ore to be more sparse";
        public static final String REGENERATE_ORE_CLUSTERS = "Flag indicating if ore clusters should regenerate by default";
        public static final String ORE_CLUSTERS = "Definition of various cluster parameters per specific ore block ";
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

        public static final String ORE_COMMENT = "Specifies the type of ore in the cluster";
        public static final String VEIN_SPAWNING_SPAWN_RATE_MODIFIER_COMMENT = "Modifier for the spawn rate of veins";
        public static final String VEIN_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT = "Block used to fill empty spaces in veins";
        public static final String CLUSTER_SPAWNING_SPAWN_RATE_COMMENT = "Specifies the percentage chance of an ore being part of a cluster";
        public static final String CLUSTER_SPAWNING_VOLUME_COMMENT = "Dimensions of the ore cluster";
        public static final String CLUSTER_SPAWNING_DENSITY_COMMENT = "Density of ore within the cluster";
        public static final String CLUSTER_SPAWNING_SHAPE_COMMENT = "Shape of the ore cluster";
        public static final String CLUSTER_SPAWNING_MAX_Y_LEVEL_SPAWN_COMMENT = "Maximum Y-level at which ore clusters can propagate";
        public static final String CLUSTER_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT = "Block used to fill empty spaces in clusters";

        public OreClusterConfig(COreClusters parent, String oreName) {
            ore = parent.s(oreName, "ore", ORE_COMMENT);
            veinSpawningSpawnRateModifier = parent.f(0.5f, 0, 1, "veinSpawningSpawnRateModifier", VEIN_SPAWNING_SPAWN_RATE_MODIFIER_COMMENT);
            veinSpawningReplaceableEmptyBlock = parent.s("stone", "veinSpawningReplaceableEmptyBlock", VEIN_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT);
            clusterSpawningSpawnRate = parent.f(0.5f, 0, 1, "clusterSpawningSpawnRate", CLUSTER_SPAWNING_SPAWN_RATE_COMMENT);
            clusterSpawningVolume = parent.s("32x32x32", "clusterSpawningVolume", CLUSTER_SPAWNING_VOLUME_COMMENT);
            clusterSpawningDensity = parent.s("60%", "clusterSpawningDensity", CLUSTER_SPAWNING_DENSITY_COMMENT);
            clusterSpawningShape = parent.s("bowl", "clusterSpawningShape", CLUSTER_SPAWNING_SHAPE_COMMENT);
            clusterSpawningMaxYLevelSpawn = parent.i(64, -64, 1024, "clusterSpawningMaxYLevelSpawn", CLUSTER_SPAWNING_MAX_Y_LEVEL_SPAWN_COMMENT);
            clusterSpawningReplaceableEmptyBlock = parent.s("stone", "clusterSpawningReplaceableEmptyBlock", CLUSTER_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT);
        }
    }

}
