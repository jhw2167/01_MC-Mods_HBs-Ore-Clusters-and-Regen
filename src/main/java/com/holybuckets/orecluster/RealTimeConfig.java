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

        //We will batch checks for which chunks have clusters by the next CHUNK_NORMALIZATION_TOTAL chunks at a time
        // thus the spawnrate is normalized to 64 chunks
        public static final Integer CHUNK_NORMALIZATION_TOTAL = COreClusters.DEF_ORE_CLUSTER_SPAWNRATE_AREA;
        public static final Function<Integer, Double> CHUNK_DISTRIBUTION_STDV_FUNC = (mean ) -> {
            if( mean < 4 )
                return mean / 4.0;
             else
                return mean / (Math.log(mean) * 2);
            };
        public static final Integer CLUSTER_BATCH_TOTAL = 1000;

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

            for( String validOreCluster : defaultConfig.validOreClusterOreBlocks.stream().toList() ) {
                OreClusterConfigModel defaultConfiguredCluster = new OreClusterConfigModel(defaultConfig.serialize());
                defaultConfiguredCluster.setOreClusterType(validOreCluster);
                oreConfigs.put(validOreCluster, defaultConfiguredCluster );
            }

            //Particular configs will overwrite the default data
            for (String oreConfig : jsonOreConfigs) {
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
