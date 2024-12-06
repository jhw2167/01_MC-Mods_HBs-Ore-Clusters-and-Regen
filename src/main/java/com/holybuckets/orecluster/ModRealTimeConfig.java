package com.holybuckets.orecluster;

//MC Imports

//Forge Imports
import com.google.gson.Gson;
import com.holybuckets.foundation.GeneralRealTimeConfig;
import com.holybuckets.orecluster.config.COreClusters;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.Mod;

//Java Imports
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class ModRealTimeConfig
{
    public static final String CLASS_ID = "000";

    /**
     *  Base User configured data: defaultConfig and oreConfigs for particular ores
     */

    private OreClusterConfigModel defaultConfig;
    private Map<Block, OreClusterConfigModel> oreConfigs;

    /** We will batch checks for which chunks have clusters by the next CHUNK_NORMALIZATION_TOTAL chunks at a time
     thus the spawnrate is normalized to 256 chunks */
    public static final Integer CHUNK_NORMALIZATION_TOTAL = COreClusters.DEF_ORE_CLUSTER_SPAWNRATE_AREA;
    public static final Function<Integer, Double> CHUNK_DISTRIBUTION_STDV_FUNC = (mean ) -> {
        if( mean < 8 )
            return mean / 2.0;
         else
            return mean / (Math.log(mean) * 3);
        };

    /** As the player explores the world, we will batch new cluster spawns in
     * sizes of 1024. Each chunk will determine the clusters it owns extenting spirally from worldspawn.
     * This is not efficient, but ensures consistently between world seeds.
     *
     * Once a player loads a chunk more than 256 chunks from the worldspawn,
     * this method becomes inefficient and we will load chunks spirally considering this
     * new chunk as the center.
     */
    public static final Integer ORE_CLUSTER_DTRM_BATCH_SIZE_TOTAL = 256; //chunks per batch
    public static final Integer ORE_CLUSTER_DTRM_RADIUS_STRATEGY_CHANGE = 256;  //square chunks


    //Using minecraft world seed as default
    public static Long CLUSTER_SEED = null;

        //Constructor initializes the defaultConfigs and oreConfigs from forge properties
        public ModRealTimeConfig(LevelAccessor level)
        {

            COreClusters clusterConfig = AllConfigs.server().cOreClusters;
            defaultConfig = new OreClusterConfigModel(clusterConfig);

            //Create new oreConfig for each element in cOreClusters list
            oreConfigs = new HashMap<Block, OreClusterConfigModel>();
            List<String> jsonOreConfigs = loadJsonOreConfigs( level, clusterConfig );

            //Default configs will be used for all valid ore clusters unless overwritten
            for( Block validOreClusterBlock : defaultConfig.validOreClusterOreBlocks.stream().toList() )
            {
                defaultConfig.setOreClusterType(validOreClusterBlock);
                OreClusterConfigModel oreConfig = new OreClusterConfigModel(defaultConfig.serialize());
                oreConfigs.put(validOreClusterBlock, oreConfig );
            }
            defaultConfig.setOreClusterType((Block) null);

            //Particular configs will overwrite the default data
            for (String oreConfig : jsonOreConfigs)
            {
                OreClusterConfigModel cluster = new OreClusterConfigModel(oreConfig);
                oreConfigs.put(cluster.oreClusterType, cluster);
            }

            //Validate the defaultConfig minSpacingBetweenClusters
            validateClusterSpacingAndMinBlocks();


            if( !defaultConfig.subSeed.equals(0L) ) {
                CLUSTER_SEED = defaultConfig.subSeed;
            } else {
                CLUSTER_SEED = GeneralRealTimeConfig.getInstance().getWORLD_SEED();
            }

            LoggerProject.logInit("000000", this.getClass().getName());
        }

        /**
         *  Getters
         */

        public Map<Block, OreClusterConfigModel> getOreConfigs() {
            return oreConfigs;
        }

        public OreClusterConfigModel getDefaultConfig() {
            return defaultConfig;
        }

        /**
         *  Setters
         */

        public void setDefaultConfig(OreClusterConfigModel defaultConfig) {
            this.defaultConfig = defaultConfig;
        }

        public void setOreConfigs(Map<Block, OreClusterConfigModel> oreConfigs) {
            this.oreConfigs = oreConfigs;
        }

    /**
     * Helper methods
     */

     private void validateClusterSpacingAndMinBlocks()
     {
         //Mathematically validate the defaultConfig minSpacingBetweenClusters
         // is acceptable considering the provided spawnrate of each specific ore cluster
         int totalSpawnRatePerAreaSquared = 0; //256 chunks squared -> 16x16
         for( OreClusterConfigModel oreConfig : this.oreConfigs.values() ) {
             totalSpawnRatePerAreaSquared += oreConfig.oreClusterSpawnRate;
         }
         int reservedBlocksSquaredPerCluster = (int) Math.pow(defaultConfig.minChunksBetweenOreClusters, 2);

         //Avoid divide by zero adjustment
         if( reservedBlocksSquaredPerCluster == 0 || totalSpawnRatePerAreaSquared == 0 )
             return;


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
     }


    /**
     * - Attempts to load the HBOreClustersAndRegenConfigs.json file from the config directory
     * - First checks if a config file exists in the <serverDirectory>/config
     * - Provided string may be a relative path or a full path from the root directory.
     * -
     * @param level
     * @param jsonConfigFile
     * @return
     */
    private List<String> loadJsonOreConfigs(LevelAccessor level, COreClusters configs)
    {
        final String providedFileName = configs.oreClusters.get();
        final String defaultFileName = configs.oreClusters.getDefault();
        File serverDirectory = level.getServer().getServerDirectory();

        File configFile = new File(serverDirectory, providedFileName);

        if( !configFile.exists() )  //User set file
        {
            final StringBuilder warnNoUserFile = new StringBuilder();
            warnNoUserFile.append("Could not find the provided ore cluster config file at path: ");
            warnNoUserFile.append(configFile.getAbsolutePath());
            warnNoUserFile.append(". Provided file name from serverConfig/hbs_ore_clusters_and_regen-server.toml: ");
            warnNoUserFile.append(providedFileName);
            warnNoUserFile.append(". Attempting to load the default file at: ");
            warnNoUserFile.append(defaultFileName);
            LoggerProject.logWarning("000001",  warnNoUserFile.toString() );

            configFile = new File(serverDirectory, defaultFileName);
            if( !configFile.exists() )  //default file
            {
                final StringBuilder warnNoDefaultFile = new StringBuilder();
                warnNoDefaultFile.append("Could not find the default ore cluster JSON config file at path: ");
                warnNoDefaultFile.append(configFile.getAbsolutePath());
                warnNoDefaultFile.append(". A default file will be created for future reference.");
                LoggerProject.logError("000002", warnNoDefaultFile.toString());

                try {
                    configFile.createNewFile();
                }
                catch (Exception e)
                {
                    final StringBuilder error = new StringBuilder();
                    error.append("Could not create the default ore cluster JSON config file at path: ");
                    error.append(configFile.getAbsolutePath());
                    error.append(" due to an unknown exception. The game will still run using default values from memory.");
                    error.append("  You can try running the game as an administrator or update the file permissions to fix this issue.");
                    LoggerProject.logError("000003", error.toString());

                    return DEFAULT_ORE_CONFIG_JSON;
                }

                writeDefaultJsonOreConfigsToFile(configFile);
            }
        }
        /**
         * At this point, configFile exists in some capacity, lets check
         * if its valid JSON or not by reading it in.
         */
        String jsonOreConfigs = "";
        try {
            //Read line by line into a single string
            jsonOreConfigs = Files.readString(Paths.get(configFile.getAbsolutePath()));
        } catch (IOException e) {
            final StringBuilder error = new StringBuilder();
            error.append("Could not read the ore cluster JSON config file at path: ");
            error.append(configFile.getAbsolutePath());
            error.append(" due to an unknown exception. The game will still run using default values from memory.");
            LoggerProject.logError("000004", error.toString());

            return DEFAULT_ORE_CONFIG_JSON;
        }

        /**
         * Now, use gson to convert the string to a LIST of strings
         */
        Gson gson = new Gson();

        try {
            return gson.fromJson(jsonOreConfigs, List.class);
        } catch (Exception e) {
            final StringBuilder error = new StringBuilder();
            error.append("Could not parse the ore cluster JSON config file at path: ");
            error.append(configFile.getAbsolutePath());
            error.append(" due to an unknown exception. The game will still run using default values from memory.");
            error.append(" And it will write those configs to the default config file for reference");
            LoggerProject.logError("000005", error.toString());

            return DEFAULT_ORE_CONFIG_JSON;
        }

    }
    //END loadJsonOreConfigs

    private boolean writeDefaultJsonOreConfigsToFile(File configFile)
    {
        //Use gson to serialize the default values and write to the file
        Gson gson = new Gson();
        String json = gson.toJson(DEFAULT_ORE_CONFIG_JSON);

        try {
            Files.write(Paths.get(configFile.getAbsolutePath()), json.getBytes());
        } catch (IOException e) {
            final StringBuilder error = new StringBuilder();
            error.append("Could not write the default ore cluster JSON config file at path: ");
            error.append(configFile.getAbsolutePath());
            error.append(" due to an unknown exception. The game will still run using default values from memory.");
            error.append("  You can try running the game as an administrator or check the file permissions.");
            LoggerProject.logError("000004", error.toString());
        }
    }


    private final List<String> DEFAULT_ORE_CONFIG_JSON = Arrays.asList("{'oreClusterType':'minecraft:iron_ore','oreClusterSpawnRate':16,'oreClusterVolume':'16x16x16','oreClusterDensity':0.8,'oreClusterShape':'any','oreClusterMaxYLevelSpawn':256,'minChunksBetweenOreClusters':0,'oreVeinModifier':0.5,'oreClusterNonReplaceableBlocks':'minecraft:bedrock, minecraft:end_portal_frame','oreClusterReplaceableEmptyBlocks':'minecraft:stone,minecraft:air','oreClusterDoesRegenerate':true}",
     "{'oreClusterType':'minecraft:deepslate_diamond_ore','oreVeinModifier':0.2,'oreClusterNonReplaceableBlocks':'minecraft:bedrock, minecraft:end_portal_frame','oreClusterReplaceableEmptyBlocks':'minecraft:deepslate'");

}
//END CLASS