package com.holybuckets.config;

import com.holybuckets.foundation.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
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
    public final ConfigStringList oreClusterReplaceableBlocks;
    public final ConfigString oreClusterReplaceableEmptyBlock;
    public final ConfigBool regenerateOreClusters;

    public final List<OreClusterConfig> oreClusters;

    public COreClusters(ForgeConfigSpec.Builder builder) {
        super(builder);

        baseOreClusterSpawnRate = i(1, 0, 96, "baseOreClusterSpawnRate", "Defines the initial spawn rate of ore clusters. The number of expected ore clusters per 96 chunks");
        baseOreClusterVolume = s("32x32x32", "baseOreClusterVolume", "Specifies the dimensions of the ore cluster. <X>x<Y>x<Z>. The true cluster will always be smaller than this box because it will choose a shape that roughly fits inside it");
        baseOreClusterDensity = f(0.60f, 0, 1, "baseOreClusterDensity", "Determines the density of ore within a cluster");
        baseOreClusterShape = s("bowl", "baseOreClusterShape", "Defines the shape of the ore cluster. Defaults to none, which takes a random shape");
        oreClusterMaxYLevelSpawn = i(64, -64, 1024, "oreClusterMaxYLevelSpawn", "Maximum Y-level at which ore clusters can propagate");
        minChunksBetweenOreClusters = i(0, 0, 96, "minChunksBetweenOreClusters", "Minimum number of chunks between ore clusters");
        maxChunksBetweenOreClusters = i(9, 9, 96, "maxChunksBetweenOreClusters", "Maximum number of chunks between ore clusters");
        baseOreVeinModifier = f(1f, 0, 10, "baseOreVeinModifier", "Scales the presence of regular ore veins");
        oreClusterReplaceableBlocks = sl(Arrays.asList("stone", "cobblestone", "endStone", "woodenPlanks", "andesite"), "oreClusterReplaceableBlocks", "List of blocks that can be replaced by the specified ore of the cluster during cluster generation");
        oreClusterReplaceableEmptyBlock = s("air", "oreClusterReplaceableEmptyBlock", "Block used to fill in the ore cluster shape when we want the ore to be more sparse");
        regenerateOreClusters = b(true, "regenerateOreClusters", "Flag indicating if ore clusters should regenerate by default");

        oreClusters = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "OreClusters";
    }

    public static class OreClusterConfig {
        public final ConfigString ore;
        public final ConfigFloat veinSpawningSpawnRateModifier;
        public final ConfigString veinSpawningReplaceableEmptyBlock;
        public final ConfigFloat clusterSpawningSpawnRate;
        public final ConfigString clusterSpawningVolume;
        public final ConfigString clusterSpawningDensity;
        public final ConfigString clusterSpawningShape;
        public final ConfigInt clusterSpawningMaxYLevelSpawn;
        public final ConfigString clusterSpawningReplaceableEmptyBlock;

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
