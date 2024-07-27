package com.holybuckets.config;

import com.holybuckets.foundation.ConfigBase;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class COreClusters extends ConfigBase {

    public final ConfigInt clusterSize;
    public final ConfigInt clusterCount;
    public final ConfigInt minHeight;
    public final ConfigInt maxHeight;
    public final ConfigFloat discardChanceOnAirExposure;
    public final ConfigFloat replacementChance;
    public final ConfigFloat replacementChanceIncrease;
    public final ConfigFloat replacementChanceDecrease;
    public final ConfigFloat replacementChanceMin;
    public final ConfigFloat replacementChanceMax;
    public final ConfigInt replacementRadius;
    public final ConfigInt replacementRadiusIncrease;
    public final ConfigInt replacementRadiusDecrease;
    public final ConfigInt replacementRadiusMin;
    public final ConfigInt replacementRadiusMax;
    public final ConfigInt replacementMaxCount;

    public final List<OreClusterConfig> oreClusters;

    public COreClusters(ForgeConfigSpec.Builder builder) {
        super(builder);

        clusterSize = i(64, 1, "clusterSize", "The size of each ore cluster");
        clusterCount = i(8, 1, "clusterCount", "The number of clusters per chunk");
        minHeight = i(-64, -64, "minHeight", "The minimum height for ore generation");
        maxHeight = i(320, -64, "maxHeight", "The maximum height for ore generation");
        discardChanceOnAirExposure = f(0.5f, 0, 1, "discardChanceOnAirExposure", "The chance to discard a block placement if it would be exposed to air");
        replacementChance = f(0.05f, 0, 1, "replacementChance", "The chance to replace a block in the cluster");
        replacementChanceIncrease = f(0.05f, 0, 1, "replacementChanceIncrease", "The amount to increase replacement chance by on each iteration");
        replacementChanceDecrease = f(0.05f, 0, 1, "replacementChanceDecrease", "The amount to decrease replacement chance by on each iteration");
        replacementChanceMin = f(0.01f, 0, 1, "replacementChanceMin", "The minimum replacement chance");
        replacementChanceMax = f(0.5f, 0, 1, "replacementChanceMax", "The maximum replacement chance");
        replacementRadius = i(5, 1, "replacementRadius", "The radius to check for replaceable blocks");
        replacementRadiusIncrease = i(1, 0, "replacementRadiusIncrease", "The amount to increase the replacement radius by on each iteration");
        replacementRadiusDecrease = i(1, 0, "replacementRadiusDecrease", "The amount to decrease the replacement radius by on each iteration");
        replacementRadiusMin = i(1, 1, "replacementRadiusMin", "The minimum replacement radius");
        replacementRadiusMax = i(10, 1, "replacementRadiusMax", "The maximum replacement radius");
        replacementMaxCount = i(16, 1, "replacementMaxCount", "The maximum number of blocks to replace in a single iteration");

        oreClusters = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "OreClusters";
    }

    public static class OreClusterConfig {
        public final String oreType;
        public final ConfigInt clusterSize;
        public final ConfigInt clusterCount;
        public final ConfigInt minHeight;
        public final ConfigInt maxHeight;
        public final ConfigFloat discardChanceOnAirExposure;
        public final ConfigFloat replacementChance;
        public final ConfigInt replacementRadius;

        public OreClusterConfig(COreClusters parent, String oreType) {
            this.oreType = oreType;
            this.clusterSize = parent.i(parent.clusterSize.get(), 1, "clusterSize", "The size of each " + oreType + " cluster");
            this.clusterCount = parent.i(parent.clusterCount.get(), 1, "clusterCount", "The number of " + oreType + " clusters per chunk");
            this.minHeight = parent.i(parent.minHeight.get(), -64, "minHeight", "The minimum height for " + oreType + " generation");
            this.maxHeight = parent.i(parent.maxHeight.get(), -64, "maxHeight", "The maximum height for " + oreType + " generation");
            this.discardChanceOnAirExposure = parent.f(parent.discardChanceOnAirExposure.getF(), 0, 1, "discardChanceOnAirExposure", "The chance to discard a " + oreType + " block placement if it would be exposed to air");
            this.replacementChance = parent.f(parent.replacementChance.getF(), 0, 1, "replacementChance", "The chance to replace a block with " + oreType + " in the cluster");
            this.replacementRadius = parent.i(parent.replacementRadius.get(), 1, "replacementRadius", "The radius to check for replaceable blocks for " + oreType);
        }
    }

    public void addOreCluster(String oreType) {
        oreClusters.add(new OreClusterConfig(this, oreType));
    }
}
