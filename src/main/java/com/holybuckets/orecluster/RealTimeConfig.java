package com.holybuckets.orecluster;

//Forge Imports
import com.holybuckets.orecluster.config.COreClusters;
import net.minecraftforge.fml.common.Mod;

//Java Imports
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        OreClusterConfigModel defaultConfig;
        Map<String, OreClusterConfigModel> oreConfigs;

        //Constructor initializes the defaultConfigs and oreConfigs from forge properties
        public RealTimeConfig()
        {
            COreClusters clusterConfig = AllConfigs.server().cOreClusters;
            defaultConfig = new OreClusterConfigModel(clusterConfig);

            //Create new oreConfig for each element in cOreClusters list
            oreConfigs = new HashMap<>();
            List<String> jsonOreConfigs = AllConfigs.server().cOreClusters.oreClusters.get();
            for (String oreConfig : jsonOreConfigs) {
                OreClusterConfigModel cluster = new OreClusterConfigModel(oreConfig);
                oreConfigs.put(cluster.oreClusterType, cluster);
            }

        }

}
