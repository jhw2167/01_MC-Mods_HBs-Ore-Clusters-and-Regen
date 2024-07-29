package com.holybuckets.orecluster.config;

import com.holybuckets.foundation.ConfigBase;
import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class COreClusters extends ConfigBase {

    //Put the defaults into public static final fields
    public static String SUB_SEED = "";
    public static final String DEF_VALID_ORE_CLUSTER_ORE_BLOCKS = "minecraft:iron_ore,minecraft:diamond_ore, minecraft:gold_ore, minecraft:coal_ore";
    public static final int DEF_ORE_CLUSTER_SPAWN_RATE = 4;
    public static final String DEF_ORE_CLUSTER_VOLUME = "32x32x32";
    public static final float DEF_ORE_CLUSTER_DENSITY = 0.60f;
    public static final String DEF_ORE_CLUSTER_SHAPE = "any";
    public static final int ORE_CLUSTER_MAX_Y_LEVEL_SPAWN = 256;
    public static final int MIN_CHUNKS_BETWEEN_ORE_CLUSTERS = 0;
    public static final int MAX_CHUNKS_BETWEEN_ORE_CLUSTERS = 9;
    public static final float DEF_ORE_VEIN_MODIFIER = 1f;
    public static final String ORE_CLUSTER_NONREPLACEABLE_BLOCKS = "minecraft:end_portal_frame,minecraft:bedrock";
    public static final String ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK = "minecraft:air";

    public static final boolean REGENERATE_ORE_CLUSTERS = true;
    public static final String REGENERATE_ORE_CLUSTER_PERIOD_LENGTHS = "28, 24, 16, 8";
    public static final String REGENERATE_ORE_CLUSTER_UPGRDADE_ITEMS = "default, minecraft:blaze_powder," +
     "minecraft:dragon_egg, minecraft:nether_star";

     //Ranges
     public static final String MIN_ORE_CLUSTER_VOLUME = "1x1x1";
     public static final String MAX_ORE_CLUSTER_VOLUME = "96x96x96";
     public static final HashSet<String> ORE_CLUSTER_VALID_SHAPES = new HashSet<>(Arrays.asList("bowl", "anvil", "shale", "any"));

    public final ConfigString subSeed = s(SUB_SEED, "subSeed", Comments.DEF_SUB_SEED);
    public final ConfigString validOreClusterOreBlocks = s(DEF_VALID_ORE_CLUSTER_ORE_BLOCKS, "validOreClusterOreBlocks", Comments.DEF_VALID_ORE_CLUSTER_ORE_BLOCKS);
    public final ConfigInt defaultOreClusterSpawnRate = i(DEF_ORE_CLUSTER_SPAWN_RATE, 0, 96, "defaultOreClusterSpawnRate", Comments.DEF_ORE_CLUSTER_SPAWN_RATE);
    public final ConfigString defaultOreClusterVolume = s(DEF_ORE_CLUSTER_VOLUME, "defaultOreClusterVolume", Comments.DEF_ORE_CLUSTER_VOLUME);
    public final ConfigFloat defaultOreClusterDensity = f(DEF_ORE_CLUSTER_DENSITY, 0, 1, "defaultOreClusterDensity", Comments.DEF_ORE_CLUSTER_DENSITY);
    public final ConfigString defaultOreClusterShape = s(DEF_ORE_CLUSTER_SHAPE, "defaultOreClusterShape", Comments.DEF_ORE_CLUSTER_SHAPE);
    public final ConfigInt oreClusterMaxYLevelSpawn = i(ORE_CLUSTER_MAX_Y_LEVEL_SPAWN, -64, 1024, "oreClusterMaxYLevelSpawn", Comments.ORE_CLUSTER_MAX_Y_LEVEL_SPAWN);
    public final ConfigInt minChunksBetweenOreClusters = i(MIN_CHUNKS_BETWEEN_ORE_CLUSTERS, 0, 96, "minChunksBetweenOreClusters", Comments.MIN_CHUNKS_BETWEEN_ORE_CLUSTERS);
    public final ConfigInt maxChunksBetweenOreClusters = i(MAX_CHUNKS_BETWEEN_ORE_CLUSTERS, 9, 96, "maxChunksBetweenOreClusters", Comments.MAX_CHUNKS_BETWEEN_ORE_CLUSTERS);
    public final ConfigFloat defaultOreVeinModifier = f(DEF_ORE_VEIN_MODIFIER, 0, 1, "defaultOreVeinModifier", Comments.DEF_ORE_VEIN_MODIFIER);
    public final ConfigString defaultOreClusterNonReplaceableBlocks = s(ORE_CLUSTER_NONREPLACEABLE_BLOCKS, "defaultOreClusterReplaceableBlocks", Comments.ORE_CLUSTER_NONREPLACEABLE_BLOCKS);
    public final ConfigString defaultOreClusterReplaceableEmptyBlock = s(ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK, "defaultOreClusterReplaceableEmptyBlock", Comments.ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK);
    public final ConfigBool regenerateOreClusters = b(REGENERATE_ORE_CLUSTERS, "regenerateOreClusters", Comments.REGENERATE_ORE_CLUSTERS);
    public final ConfigString regenerateOreClusterPeriodLengths = s(REGENERATE_ORE_CLUSTER_PERIOD_LENGTHS, "regenerateOreClusterPeriodLengths", Comments.REGENERATE_ORE_CLUSTER_PERIOD_LENGTHS);
    public final ConfigString regenerateOreClusterUpgradeItems = s(REGENERATE_ORE_CLUSTER_UPGRDADE_ITEMS, "regenerateOreClusterUpgradeItems", Comments.REGENERATE_ORE_CLUSTER_UPGRADE_ITEMS);

    //Define some static OreClusterConfigs for

        //minecraft:iron_ore

            //public final OreClusterConfig ironOre = new OreClusterConfig("iron_ore");
            public final OreClusterConfigModel ironOre = new OreClusterConfigModel(Blocks.IRON_ORE );
            public final OreClusterConfigModel diamondOre = new OreClusterConfigModel(Blocks.DIAMOND_ORE);

        public final ConfigList oreClusters = list(new ArrayList<String>( Arrays.asList(ironOre.serialize(), diamondOre.serialize())),
                "oreClusters", Comments.ORE_CLUSTERS);


    @Override
    public String getName() {
        return "OreClusters";
    }

    public static class Comments {
        public static final String DEF_SUB_SEED = "Sub-seed used to generate random numbers for ore cluster generation - by default," +
         " Ore cluster generation uses the world seed to determine which chunks have ore clusters and their shape and size. By assigning a " +
          "sub-seed, you can adjust this randomness for the same world";
        public static final String DEF_VALID_ORE_CLUSTER_ORE_BLOCKS = "List of valid ore blocks that can be used in ore clusters.  Clusters spawned from these " +
         "ores will take default ore cluster parameters unless overridden in 'oreClusers' below";
        public static final String DEF_ORE_CLUSTER_SPAWN_RATE = "Defines the default frequency of ore clusters. Takes an integer as the number of expected ore clusters per 96 chunks";
        public static final String DEF_ORE_CLUSTER_VOLUME = "Specifies the dimensions of the ore cluster. <X>x<Y>x<Z>. The true cluster will always be smaller than this box because it will choose a shape that roughly fits inside it";
        public static final String DEF_ORE_CLUSTER_DENSITY = "Determines the density of ore within a cluster";
        public static final String DEF_ORE_CLUSTER_SHAPE = "Defines the shape of the ore cluster. Options are 'bowl', 'anvil', 'shale' or 'any'. Defaults to any, which takes a random shape";
        public static final String ORE_CLUSTER_MAX_Y_LEVEL_SPAWN = "Maximum Y-level at which ore clusters can spawn";
        public static final String MIN_CHUNKS_BETWEEN_ORE_CLUSTERS = "Minimum number of chunks between ore clusters";
        public static final String MAX_CHUNKS_BETWEEN_ORE_CLUSTERS = "Maximum number of chunks between ore clusters";
        public static final String DEF_ORE_VEIN_MODIFIER = "Scales the presence of normal (small) ore veins between 0 and 1. This mod" +
         "replaces existing ore vein blocks in real time with the specified 'ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK' block so can only" +
          " reduce the frequency of ore veins, not increase it";
        public static final String ORE_CLUSTER_NONREPLACEABLE_BLOCKS = "List of blocks that should not be replaced by the specified ore during cluster generation. " +
         "For example, if you don't want ore clusters to replace bedrock - which is very reasonable - you would add 'minecraft:bedrock' to this list";
        public static final String ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK = "Block used to fill in the ore cluster shape when we want the ore to be more sparse" +
         "this field can take multiple comma seperated blocks; only the first block will be used to replace ore veins if ORE_VEIN_MODIFIER is below 1";
        public static final String REGENERATE_ORE_CLUSTERS = "Flag indicating if ore clusters should regenerate by default";
        public static final String ORE_CLUSTERS = "Definition of various cluster parameters per specific ore block ";

        public static final String REGENERATE_ORE_CLUSTER_PERIOD_LENGTHS = "Comma separated list of integer days it takes for clusters to regenerate their ores." +
         " All clusters will regenerate on the same schedule. After sacrificing the specified item in the array below, the period length is reduced";
        public static final String REGENERATE_ORE_CLUSTER_UPGRADE_ITEMS = "Comma separated list of items that will be used to reduce the number of days" +
         " between cluster regeneration. If the first value is NOT 'default', then clusters will not regenerate until the specified item has been sacrificed." +
          "In game, use the 'sacrificial altar' to sacrifice the specified item to trigger the next period length";
    }

    public static class OreClusterConfig extends ConfigBase {

        static final String ORE_COMMENT = "Fully qualified name of the block/ore we are configuring";
        static final String VEIN_SPAWNING_SPAWN_RATE_MODIFIER_COMMENT = "Modifier for the spawn rate of veins";
        static final String VEIN_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT = "Block used to fill empty spaces in veins";
        static final String CLUSTER_SPAWNING_SPAWN_RATE_COMMENT = "Specifies the percentage chance of an ore being part of a cluster";
        static final String CLUSTER_SPAWNING_VOLUME_COMMENT = "Dimensions of the ore cluster";
        static final String CLUSTER_SPAWNING_DENSITY_COMMENT = "Density of ore within the cluster";
        static final String CLUSTER_SPAWNING_SHAPE_COMMENT = "Shape of the ore cluster";
        static final String CLUSTER_SPAWNING_MAX_Y_LEVEL_SPAWN_COMMENT = "Maximum Y-level at which ore clusters can propagate";
        static final String CLUSTER_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT = "Block used to fill empty spaces in clusters";

        static final String oreName = "iron_ore";
        public final ConfigGroup base = group(0, oreName, "Configuring specific cluster and vein parameters for ore");
        public final ConfigString ore = s(oreName, "ore", ORE_COMMENT);
        public final ConfigFloat veinSpawningSpawnRateModifier = f(0.5f, 0, 1, "veinSpawningSpawnRateModifier", VEIN_SPAWNING_SPAWN_RATE_MODIFIER_COMMENT);
        public final ConfigString veinSpawningReplaceableEmptyBlock = s("stone", "veinSpawningReplaceableEmptyBlock", VEIN_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT);
        public final ConfigFloat clusterSpawningSpawnRate = f(0.5f, 0, 1, "clusterSpawningSpawnRate", CLUSTER_SPAWNING_SPAWN_RATE_COMMENT);
        public final ConfigString clusterSpawningVolume = s("32x32x32", "clusterSpawningVolume", CLUSTER_SPAWNING_VOLUME_COMMENT);
        public final ConfigString clusterSpawningDensity = s("60%", "clusterSpawningDensity", CLUSTER_SPAWNING_DENSITY_COMMENT);
        public final ConfigString clusterSpawningShape = s("bowl", "clusterSpawningShape", CLUSTER_SPAWNING_SHAPE_COMMENT);
        public final ConfigInt clusterSpawningMaxYLevelSpawn = i(64, -64, 1024, "clusterSpawningMaxYLevelSpawn", CLUSTER_SPAWNING_MAX_Y_LEVEL_SPAWN_COMMENT);
        public final ConfigString clusterSpawningReplaceableEmptyBlock = s("stone", "clusterSpawningReplaceableEmptyBlock", CLUSTER_SPAWNING_REPLACEABLE_EMPTY_BLOCK_COMMENT);


        //implement getName
        @Override
        public String getName() {
            return ore.get();
        }

    }

}
