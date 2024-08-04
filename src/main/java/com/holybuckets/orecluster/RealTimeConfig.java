package com.holybuckets.orecluster;

//MC Imports

//Forge Imports
import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.orecluster.config.COreClusters;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fml.common.Mod;

//Java Imports
    import java.util.*;
import java.util.function.Function;

//Project imports
import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import com.holybuckets.orecluster.config.AllConfigs;


/*
    * Class: RealTimeConfig
    *
    * Description: The majority of fundamental mod config is in the config package.
    * This class will manifest that data behind simple methods and efficient data structures.

 */
@Mod.EventBusSubscriber(modid = OreClustersAndRegenMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RealTimeConfig
{
    /**
     *  Base User configured data: defaultConfig and oreConfigs for particular ores
     */

    OreClusterConfigModel defaultConfig = null;
    Map<String, OreClusterConfigModel> oreConfigs = null;

    /** We will batch checks for which chunks have clusters by the next CHUNK_NORMALIZATION_TOTAL chunks at a time
     thus the spawnrate is normalized to 256 chunks */
    public static final Integer CHUNK_NORMALIZATION_TOTAL = COreClusters.DEF_ORE_CLUSTER_SPAWNRATE_AREA;
    public static final Function<Integer, Double> CHUNK_DISTRIBUTION_STDV_FUNC = (mean ) -> {
        if( mean < 8 )
            return mean / 2.0;
         else
            return mean / (Math.log(mean) * 3);
        };

    /** AS the player explores the world, we will batch new cluster spawns in
     * sizes of 1024. Each chunk will determine the clusters it owns extenting spirally from worldspawn.
     * This is not efficient, but ensures consistently between world seeds.
     *
     * Once a player loads a chunk more than 256 chunks from the worldspawn,
     * this method becomes inefficient and we will load chunks spirally considering this
     * new chunk as the center.
     */
    public static final Integer ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL = 1024;
    public static final Integer ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE = 256;

    /** World Data **/
    public static Minecraft mc = Minecraft.getInstance();
    public static LevelAccessor LEVEL;
    public static Long WORLD_SEED;
    public static Vec3i WORLD_SPAWN;



    //Using minecraft world seed as default
    public static Long CLUSTER_SEED = null;

        //Constructor initializes the defaultConfigs and oreConfigs from forge properties
        public RealTimeConfig()
        {

            COreClusters clusterConfig = AllConfigs.server().cOreClusters;
            defaultConfig = new OreClusterConfigModel(clusterConfig);

            //Create new oreConfig for each element in cOreClusters list
            oreConfigs = new HashMap<>();
            List<String> jsonOreConfigs = AllConfigs.server().cOreClusters.oreClusters.get();

            //Default configs will be used for all valid ore clusters unless overwritten
            for( String validOreCluster : defaultConfig.validOreClusterOreBlocks.stream().toList() )
            {
                OreClusterConfigModel oreConfig = new OreClusterConfigModel(defaultConfig.serialize());
                oreConfig.setOreClusterType(validOreCluster);
                oreConfigs.put(validOreCluster, oreConfig );
            }

            //Particular configs will overwrite the default data
            for (String oreConfig : jsonOreConfigs)
            {
                OreClusterConfigModel cluster = new OreClusterConfigModel(oreConfig);
                oreConfigs.put(cluster.oreClusterType, cluster);
            }

            //Mathematically validate the defaultConfig minSpacingBetweenClusters
            // is acceptable considering the provided spawnrate of each specific ore cluster
            int totalSpawnRatePerAreaSquared = 0; //256 chunks squared -> 16x16
            for( OreClusterConfigModel oreConfig : oreConfigs.values() ) {
                totalSpawnRatePerAreaSquared += oreConfig.oreClusterSpawnRate;
            }
            int reservedBlocksSquaredPerCluster = (int) Math.pow(defaultConfig.minChunksBetweenOreClusters, 2);
            int maxClustersPerAreaSquared = CHUNK_NORMALIZATION_TOTAL / reservedBlocksSquaredPerCluster;
            if( totalSpawnRatePerAreaSquared > maxClustersPerAreaSquared )
            {
                int newMinChunks = (int) Math.sqrt( CHUNK_NORMALIZATION_TOTAL / totalSpawnRatePerAreaSquared );
                StringBuilder warn = new StringBuilder();
                warn.append("The net ore cluster spawn rate exceeds the expected maximum number of clusters in a ");
                warn.append(CHUNK_NORMALIZATION_TOTAL);
                warn.append(" square chunk area: ");
                warn.append(maxClustersPerAreaSquared);
                warn.append(" square chunks alloted by ");
                warn.append(defaultConfig.minChunksBetweenOreClusters);
                warn.append(" chunks between clusters. While ");
                warn.append(totalSpawnRatePerAreaSquared);
                warn.append(" clusters are would be observed on average. minClustersBetweenChunks reduced to ");
                defaultConfig.minChunksBetweenOreClusters = (int) Math.sqrt( newMinChunks ) - 1;
                warn.append(defaultConfig.minChunksBetweenOreClusters);

            }


            if( defaultConfig.subSeed != null ) {
                CLUSTER_SEED = defaultConfig.subSeed;
            } else {
                CLUSTER_SEED = WORLD_SEED;
            }


        }


        /**
        * REAL TIME SERVER CONFIG FROM EVENTS
        *
         */

        public static void initWorldConfigs( LevelAccessor level )
        {
            // Capture the world seed
            LoggerBase.logInfo("**** WORLD LOAD EVENT ****");
            LEVEL = level;
            MinecraftServer server = level.getServer();
            WORLD_SEED = server.overworld().getSeed();
            WORLD_SPAWN = server.overworld().getSharedSpawnPos();
            //LoggerBase.logInfo("World Seed: " + WORLD_SEED);
            //LoggerBase.logInfo("World Spawn: " + WORLD_SPAWN);
        }
}
