package com.holybuckets.orecluster.config;

import com.holybuckets.foundation.ConfigBase;
import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraftforge.registries.ForgeRegistries.BLOCKS;

public class COreClusters extends ConfigBase {

    //Put the defaults into public static final fields
    public static final int BASE_ORE_CLUSTER_SPAWN_RATE = 1;
    public static final String BASE_ORE_CLUSTER_VOLUME = "32x32x32";
    public static final float BASE_ORE_CLUSTER_DENSITY = 0.60f;
    public static final String BASE_ORE_CLUSTER_SHAPE = "none";
    public static final int ORE_CLUSTER_MAX_Y_LEVEL_SPAWN = 256;
    public static final int MIN_CHUNKS_BETWEEN_ORE_CLUSTERS = 0;
    public static final int MAX_CHUNKS_BETWEEN_ORE_CLUSTERS = 9;
    public static final float BASE_ORE_VEIN_MODIFIER = 1f;
    public static final String ORE_CLUSTER_REPLACEABLE_BLOCKS = "stone,cobblestone,endStone,woodenPlanks,andesite";
    public static final String ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK = "air";
    public static final boolean REGENERATE_ORE_CLUSTERS = true;

    public final ConfigInt baseOreClusterSpawnRate = i(BASE_ORE_CLUSTER_SPAWN_RATE, 0, 96, "baseOreClusterSpawnRate", Comments.BASE_ORE_CLUSTER_SPAWN_RATE);
    public final ConfigString baseOreClusterVolume = s(BASE_ORE_CLUSTER_VOLUME, "baseOreClusterVolume", Comments.BASE_ORE_CLUSTER_VOLUME);
    public final ConfigFloat baseOreClusterDensity = f(BASE_ORE_CLUSTER_DENSITY, 0, 1, "baseOreClusterDensity", Comments.BASE_ORE_CLUSTER_DENSITY);
    public final ConfigString baseOreClusterShape = s(BASE_ORE_CLUSTER_SHAPE, "baseOreClusterShape", Comments.BASE_ORE_CLUSTER_SHAPE);
    public final ConfigInt oreClusterMaxYLevelSpawn = i(ORE_CLUSTER_MAX_Y_LEVEL_SPAWN, -64, 1024, "oreClusterMaxYLevelSpawn", Comments.ORE_CLUSTER_MAX_Y_LEVEL_SPAWN);
    public final ConfigInt minChunksBetweenOreClusters = i(MIN_CHUNKS_BETWEEN_ORE_CLUSTERS, 0, 96, "minChunksBetweenOreClusters", Comments.MIN_CHUNKS_BETWEEN_ORE_CLUSTERS);
    public final ConfigInt maxChunksBetweenOreClusters = i(MAX_CHUNKS_BETWEEN_ORE_CLUSTERS, 9, 96, "maxChunksBetweenOreClusters", Comments.MAX_CHUNKS_BETWEEN_ORE_CLUSTERS);
    public final ConfigFloat baseOreVeinModifier = f(BASE_ORE_VEIN_MODIFIER, 0, 10, "baseOreVeinModifier", Comments.BASE_ORE_VEIN_MODIFIER);
    public final ConfigString oreClusterReplaceableBlocks = s(ORE_CLUSTER_REPLACEABLE_BLOCKS, "oreClusterReplaceableBlocks", Comments.ORE_CLUSTER_REPLACEABLE_BLOCKS);
    public final ConfigString oreClusterReplaceableEmptyBlock = s(ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK, "oreClusterReplaceableEmptyBlock", Comments.ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK);
    public final ConfigBool regenerateOreClusters = b(REGENERATE_ORE_CLUSTERS, "regenerateOreClusters", Comments.REGENERATE_ORE_CLUSTERS);

    //Define some static OreClusterConfigs for

        //minecraft:iron_ore

            //public final OreClusterConfig ironOre = new OreClusterConfig("iron_ore");
            public final OreClusterConfigModel ironOre = new OreClusterConfigModel(Blocks.IRON_ORE.getName().getString() );
            public final OreClusterConfigModel diamondOre = new OreClusterConfigModel(Blocks.DIAMOND_ORE.toString());

        public final ConfigList oreClusters = list(new ArrayList<String>( Arrays.asList(ironOre.serialize(), diamondOre.serialize())),
                "oreClusters", Comments.ORE_CLUSTERS);


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
