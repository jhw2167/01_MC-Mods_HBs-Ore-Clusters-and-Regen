package com.holybuckets.orecluster;

//MC Imports

//Forge Imports
import com.holybuckets.orecluster.config.COreClusters;
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


            if( defaultConfig.subSeed != null ) {
                CLUSTER_SEED = defaultConfig.subSeed;
            } else {
                CLUSTER_SEED = AllConfigs.WORLD_SEED;
            }


        }

}
