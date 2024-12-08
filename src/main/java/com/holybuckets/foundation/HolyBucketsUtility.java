package com.holybuckets.foundation;


import com.holybuckets.foundation.database.DatabaseManager;
import com.holybuckets.foundation.modelInterface.IStringSerializable;
import com.holybuckets.orecluster.LoggerProject;
import com.holybuckets.orecluster.model.ManagedOreClusterChunk;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

/**
* Class: HolyBucketsUtility
*
* Description: This class will contain utility methods amd objects that I find myself using frequently
*
 */
public class HolyBucketsUtility {

    public static final String CLASS_ID = "004";

    /*
    * General variables
     */
    public static final String NAME = "HBs Utility";

    public static final String RESOURCE_NAMESPACE = "hb";

    public static DatabaseManager databaseManager = null;

    public static class BlockUtil {

        /**
         * Convert a block to its string name for formatting
         * @param blockType
         * @return
         */
        public static String blockToString(Block blockType)
        {
            if( blockType == null)
            {
                LoggerProject.logError("004000", "Error parsing blockType to string, blockType is null");
                return null;
            }

            return blockType.toString().replace("Block{", "").replace("}", "");
        }

        /**
         * Convert a block name as a string to a Minecraft Block Object: eg. "minecraft:iron_ore" to Blocks.IRON_ORE
         * @param blockStringName
         * @return
         */
        public static Block blockNameToBlock(String blockStringName)
        {
            if( blockStringName == null || blockStringName.isEmpty() )
            {
                LoggerProject.logError("004001", "Error parsing block name as string into a Minecraft Block type, " +
                    "type provided was null or provided as empty string");
                return null;
            }

            String formattedBlockString = "Block{";
            blockStringName = blockStringName.trim();
            if( blockStringName.contains(":"))
                formattedBlockString += blockStringName + "}";
            else
                formattedBlockString += "minecraft:" + blockStringName + "}";

            final String formattedBlockStringFinal = formattedBlockString;
            Block b = ForgeRegistries.BLOCKS.getValues().stream()
                .filter(block -> block.toString().equals(formattedBlockStringFinal))
                .findFirst()
                .orElse(null);

            if( b == null )
            {
                LoggerProject.logError("004002", "Error parsing block name as string into a Minecraft Block type, " +
                    "block name provided was not found in Minecraft/Forge registry: " + formattedBlockStringFinal);
            }

            return b;
        }
    }

    public static class ChunkUtil {

        public static String getId(ChunkAccess chunk) {
            return chunk.getPos().x + "," + chunk.getPos().z;
        }

        public static String getId(int x, int z) {
            return x + "," + z;
        }

        public static String getId( ChunkPos pos ) {
            return pos.x + "," + pos.z;
        }

        public static ChunkPos getPos(String id) {
            String[] parts = id.split(",");
            return new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        /** Check if chunk is within [-x, x] and [-z, z] **/
        public static boolean checkInBounds(ChunkAccess chunk, int x, int z) {
            return chunk.getPos().x >= -x && chunk.getPos().x <= x && chunk.getPos().z >= -z && chunk.getPos().z <= z;
        }

        public static int chunkDistSquared(ChunkPos p1, ChunkPos p2) {
            return (p1.x - p2.x) * (p1.x - p2.x) + (p1.z - p2.z) * (p1.z - p2.z);
        }

        public static float chunkDist(ChunkPos p1, ChunkPos p2) {
            return (float) Math.sqrt(chunkDistSquared(p1, p2));
        }

        //override with string Id args
        public static float chunkDist(String id1, String id2) {
            return chunkDist(getPos(id1), getPos(id2));
        }

        public static ChunkPos posAdd(ChunkPos p, int x, int z) {
            return new ChunkPos(p.x + x, p.z + z);
        }

        public static ChunkPos posAdd(ChunkPos p1, ChunkPos p2) {
            return new ChunkPos(p1.x + p2.x, p1.z + p2.z);
        }

        public static ChunkPos posAdd(ChunkPos p1,  int[] dir ) {
            return new ChunkPos(p1.x + dir[0], p1.z + dir[1]);
        }


        public static String readNBT(ChunkAccess chunk, String property) {
            return "";
        }

        public static String writeNBT(ChunkAccess chunk, String property, String value) {
            return "";
        }

        /**
         * Returns a levelChunk from the level object, never forceloaded
         */
        public static LevelChunk getLevelChunk(LevelAccessor level, ManagedOreClusterChunk chunk) {
            return getLevelChunk(level, chunk.getId());
        }

        //override with chunkAccess object
        public static LevelChunk getLevelChunk(LevelAccessor level, ChunkAccess chunk) {
            return getLevelChunk(level, getId(chunk));
        }

        //override with string Id args
        public static LevelChunk getLevelChunk(LevelAccessor level, String id) {
          ChunkPos pos =  getPos(id);
            return level.getChunkSource().getChunkNow(pos.x, pos.z);
        }
        //override with x and y coordinates
        public static LevelChunk getLevelChunk(LevelAccessor level, int x, int z) {
            return level.getChunkSource().getChunkNow(x, z);
        }

    }

    public static class SerializeUtil {

        /**
         * Serialize iterable object to a string
         */
        public static String serialize( Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object o : iterable) {
                sb.append(o.toString());
                sb.append(",");
            }
            return sb.toString();
        }

        /**
         * Deserialize a string to an iterable object
          */

        public static Iterable<String> deserialize(CompoundTag tag, String property) {
            return null;
        }


    }

    public class FileIO {

        /**
         * - Attempts to load the HBOreClustersAndRegenConfigs.json file from the config directory
         * - First checks if a config file exists in the <serverDirectory>/config
         * - Provided string may be a relative path or a full path from the root directory.
         * -
         * @param level
         * @param jsonFilePathConfig
         * @param defaultData
         * @return String
         */
        public static String loadJsonConfig(LevelAccessor level, ConfigBase.ConfigString jsonFilePathConfig,  IStringSerializable defaultData)
        {
            final String providedFileName = jsonFilePathConfig.get();
            final String defaultFileName = jsonFilePathConfig.getDefault();
            final File serverDirectory = level.getServer().getServerDirectory();

            File configFile = new File(serverDirectory, providedFileName);

            //Use gson to serialize the default values and write to the file
            final String DEFAULT_DATA = defaultData.serialize();

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

                        return DEFAULT_DATA;
                    }

                    writeDefaultJsonOreConfigsToFile(configFile, DEFAULT_DATA);
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

                return DEFAULT_DATA;
            }

            return jsonOreConfigs;

        }
        //END loadJsonOreConfigs

        private static boolean writeDefaultJsonOreConfigsToFile(File configFile, String jsonData)
        {
            try {
                Files.write(Paths.get(configFile.getAbsolutePath()), jsonData.getBytes());
            } catch (IOException e) {
                final StringBuilder error = new StringBuilder();
                error.append("Could not write the default ore cluster JSON config file at path: ");
                error.append(configFile.getAbsolutePath());
                error.append(" due to an unknown exception. The game will still run using default values from memory.");
                error.append("  You can try running the game as an administrator or check the file permissions.");
                LoggerProject.logError("000004", error.toString());
                return false;
            }

            return true;
        }

    }



    /**
    * Class: Fast3DArray
    * Description: A 3D array that is optimized for fast access by avoiding new calls
     */
    public static class Fast3DArray {

        private int[] X;
        private int[] Y;
        private int[] Z;

        public int size;
        public final int MAX_SIZE;

        public Fast3DArray(int size) {
            this.size = 0;
            this.MAX_SIZE = size;
            X = new int[size];
            Y = new int[size];
            Z = new int[size];
        }

        public void add(int x, int y, int z)
        {
            if( size >= MAX_SIZE)
                return;

            X[size] = x;
            Y[size] = y;
            Z[size] = z;
            size++;
        }

        /**
         * Returns a new 3D array of all
         * @param index
         * @return null if index is out of bounds
         */
        public int[][] get(int index) {

            if( index >= MAX_SIZE || index < 0)
                return null;
            return new int[][] { { X[index], Y[index], Z[index] } };
        }

        public int getX(int index) {
            return X[index];
        }

        public int getY(int index) {
            return Y[index];
        }

        public int getZ(int index) {
            return Z[index];
        }

    }
}
