package com.holybuckets.foundation;


import com.holybuckets.foundation.database.DatabaseManager;
import com.holybuckets.foundation.exception.InvalidId;
import com.holybuckets.foundation.modelInterface.IStringSerializable;
import com.holybuckets.orecluster.LoggerProject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Class: HolyBucketsUtility
*
* Description: This class will contain utility methods amd objects that I find myself using frequently
*
 */
public class HBUtil {

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


        /**
         * Serialize a map of blocks and their positions to a string
         * @param blocks
         * @return
         */
        public static String serializeBlockPairs(Map<Block,List<BlockPos>> blocks)
        {
            StringBuilder blockStateUpdates = new StringBuilder();
            for(Block block : blocks.keySet())
            {
                blockStateUpdates.append("{");
                String blockName = HBUtil.BlockUtil.blockToString(block);
                blockStateUpdates.append(blockName);
                blockStateUpdates.append("=");

                List<BlockPos> positions = blocks.get(block);
                if( positions.isEmpty() )
                {
                    blockStateUpdates.append("[]}, ");
                    continue;
                }

                for(BlockPos pos : positions)
                {
                    HBUtil.TripleInt vec = new HBUtil.TripleInt(pos);
                    blockStateUpdates.append("[" + vec.x + "," + vec.y + "," + vec.z + "]");
                    blockStateUpdates.append("&");
                }
                blockStateUpdates.deleteCharAt(blockStateUpdates.length() - 1);
                blockStateUpdates.append("}, ");
            }
            //remove trailing comma and space
            blockStateUpdates.deleteCharAt(blockStateUpdates.length() - 1);
            blockStateUpdates.deleteCharAt(blockStateUpdates.length() - 1);

            return blockStateUpdates.toString();
        }

        /**
         * Deserialize a string to a map of blocks and all their positions
         * @param data
         * @return
         */
        public static Map<Block,List<BlockPos>> deserializeBlockPairs(String data)
        {
            Map<Block,List<BlockPos>> blockPairs = new HashMap<>();
            String[] blocks = data.split(", ");

            for (String pairs : blocks)
            {
                //remove curly braces
                pairs = pairs.substring(1, pairs.length() - 1);

                String[] parts = pairs.split("=");
                Block blockType = BlockUtil.blockNameToBlock(parts[0]);

                blockPairs.put(blockType, new ArrayList<>());
                String[] positions = parts[1].split("&");

                if( positions[0].contains("[]") )
                    continue;

                for( String pos : positions )
                {
                    String[] vec = pos.replace("[", "").replace("]","").split(",");
                    int x = Integer.parseInt(vec[0]);
                    int y = Integer.parseInt(vec[1]);
                    int z = Integer.parseInt(vec[2]);
                    BlockPos blockPos = new BlockPos(x, y, z);
                    blockPairs.get(blockType).add(blockPos);
                }

            }

            return blockPairs;
        }


    }
    //END BLOCK UTIL

    public static class LevelUtil {

        public static LevelAccessor toLevel(String id) throws InvalidId {
            return GeneralConfig.getInstance().getLevel(id);
        }

        public static String toId(LevelAccessor level) {
            return level.dimensionType().effectsLocation().toString();
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

        public static Long getChunkRandom(String id) {
            return getChunkRandom(getPos(id));
        }

        public static Long getChunkRandom( ChunkPos pos ) {
            //map the chunks x and z position to a random number
            final Long WIDTH = 10_000_000l;
            return (pos.x * WIDTH) + pos.z;
        }


        public static String readNBT(ChunkAccess chunk, String property) {
            return "";
        }

        public static String writeNBT(ChunkAccess chunk, String property, String value) {
            return "";
        }

        /**
         * Loads the targeted chunk, forceloading if necessary. Force loading may
         * cause a circular dependency during worldLoad, first call is getChunkNow call
         * to protect against this.
         * @param level
         * @param x
         * @param z
         * @return
         */
        public static LevelChunk getLevelChunk(LevelAccessor level, int x, int z, boolean forceLoad)
        {
            LevelChunk c = level.getChunkSource().getChunkNow(x, z);
            if( c == null && forceLoad)
                c = level.getChunkSource().getChunk( x, z, true);
            return c;
        }



    }

    public static class ShapeUtil {

        /**
         * Returns a 3D array of all vertices within a circle of radius r.
         * The y value is always 0. If the provided radius is even, it will be incremented by 1.
         * @param radius
         * @return null if radius is <= 0. returns 3x3 square if radius is 2 or 3. Otherwise
         * returns all integer solutions where x^2 + z^2 <= r^2
         */
        public static Fast3DArray getCircle(int radius)
        {
            if( radius <= 0 )
                return null;

            Fast3DArray circle = new Fast3DArray(4 * radius * radius);

            circle.add(0, 0, 0);
            if( radius <= 1 ) {
                return circle;
            }

            if( radius % 2 == 0) {
                radius++;
            }

            //if radius is 3, we just return 3x3 square
            if( radius == 3)
            {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        circle.add(x, 0, z);
                    }
                }
                return circle;
            }

            int radiusSquared = radius * radius;
            for (int x = -radius; x <= radius; x++)
            {
                // Calculate y^2 for the current x
                int zSquared = radiusSquared - x * x;

                //Instead, we want all integer solutions where x^2 + y^2 <= r^2
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radiusSquared) {
                        circle.add(x, 0, z);
                    }
                }
            }

            return circle;
        }


        /**
         * Returns a 3D array of all vertices within a 2D square of provided length and width
         * The y parameter is always 0.
         * @param length
         * @param width
         * @return
         */
        public static Fast3DArray getSquare(int length, int width)
        {
            if( length <= 0 || width <= 0)
                return null;

            Fast3DArray square = new Fast3DArray(length * width + 1);

           int modL = ( length % 2 == 0) ? 1 : 0;
           int modW = ( width % 2 == 0) ? 1 : 0;

            int xStart = -length / 2 + modL;
            int zStart = -width / 2 + modW;

    //consider edge cases for l/w = 1 or 2
            if( length == 1 && width == 1)
            {
                square.add(0, 0, 0);
                return square;
            }

            if( length == 1)
            {
                for( int z = zStart; z <= width / 2; z++)
                {
                    square.add(0, 0, z);
                }
                return square;
            }

            if( width == 1)
            {
                for( int x = xStart; x <= length / 2; x++)
                {
                    square.add(x, 0, 0);
                }
                return square;
            }

            if( length == 2 && width == 2)
            {
                square.add(-1, 0, -1);
                square.add(-1, 0, 1);
                square.add(1, 0, -1);
                square.add(1, 0, 1);
                return square;
            }

            if( length == 2)
            {
                for( int z = zStart; z <= width / 2; z++)
                {
                    square.add(-1, 0, z);
                    square.add(1, 0, z);
                }
                return square;
            }

            if( width == 2)
            {
                for( int x = xStart; x <= length / 2; x++)
                {
                    square.add(x, 0, -1);
                    square.add(x, 0, 1);
                }
                return square;
            }

            for( int x = xStart; x <= length / 2; x++)
            {
                for( int z = zStart; z <= width / 2; z++)
                {
                    square.add(x, 0, z);
                }
            }
            //if l==3, xStart = -1, l/2 = 1, that works
            //if l==4, xStart = -2, l/2 = 2, thats 5 verticies, we need 4



            return square;
        }


        public static Fast3DArray getCube(int length, int width, int height)
        {
            if( height <= 0 || length <= 0 || width <= 0)
                return null;

            Fast3DArray cube = new Fast3DArray(height * length * width);

            for (int y = 0; y < height; y++)
            {
                Fast3DArray plane = getSquare(length, width);
                for( int i = 0; i < plane.size; i++)
                {
                    cube.add(plane.X[i], y, plane.Z[i]);
                }
            }

            return cube;
        }


    }
    //END SHAPEUTIL

    public static class FileIO {

        /**
         * - Attempts to load the HBOreClustersAndRegenConfigs.json file from the config directory
         * - Provided string may be a relative path or a full path from the root directory.
         * - First checks if a config file exists in the <serverDirectory>/<fileName>
         * - returns string that can be Parsed into a json object
         * @param userConfigFile
         * @param defaultConfigFile
         * @param defaultData
         * @return String
         */
        public static String loadJsonConfigs(File userConfigFile, File defaultConfigFile, IStringSerializable defaultData)
        {
            //Use gson to serialize the default values and write to the file
            File configFile = userConfigFile;
            final String DEFAULT_DATA = defaultData.serialize();

            if( !configFile.exists() )  //User set file
            {
                final StringBuilder warnNoUserFile = new StringBuilder();
                warnNoUserFile.append("Could not find the provided ore cluster config file at path: ");
                warnNoUserFile.append(configFile.getAbsolutePath());
                warnNoUserFile.append(". Provided file name from serverConfig/hbs_ore_clusters_and_regen-server.toml: ");
                warnNoUserFile.append(configFile.getPath());
                warnNoUserFile.append(". Attempting to load the default file at default location: ");
                warnNoUserFile.append(configFile.getAbsolutePath());
                LoggerProject.logWarning("000001",  warnNoUserFile.toString() );

                configFile = defaultConfigFile;
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

                    serializeJsonConfigs(configFile, DEFAULT_DATA);
                }

            }

            /**
             * At this point, configFile exists in some capacity, lets check
             * if its valid JSON or not by reading it in.
             */
            String json = "";
            try {
                //Read line by line into a single string
                json = Files.readString(Paths.get(configFile.getAbsolutePath()));
            } catch (IOException e) {
                final StringBuilder error = new StringBuilder();
                error.append("Could not read the ore cluster JSON config file at path: ");
                error.append(configFile.getAbsolutePath());
                error.append(" due to an unknown exception. The game will still run using default values from memory.");
                LoggerProject.logError("000004", error.toString());

                return DEFAULT_DATA;
            }

            return json;

        }
        //END loadJsonOreConfigs

        public static boolean serializeJsonConfigs(File configFile, String jsonData)
        {
            try {
                Files.write(Paths.get(configFile.getAbsolutePath()), jsonData.getBytes());
            } catch (IOException e) {
                final StringBuilder error = new StringBuilder();
                error.append("Could not write JSON data to config file at path: ");
                error.append(configFile.getAbsolutePath());
                error.append(" due to an unknown exception." );
                error.append("  You can try running the game as an administrator or check the file permissions.");
                LoggerProject.logError("000004", error.toString());
                return false;
            }

            return true;
        }

    }
    //END FILE IO
    public static class TripleInt {
        public int x;
        public int y;
        public int z;

        public TripleInt(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public TripleInt(Vec3i vec) {
            this.x = vec.getX();
            this.y = vec.getY();
            this.z = vec.getZ();
        }

        public static TripleInt of(int x, int y, int z) {
            return new TripleInt(x, y, z);
        }

    }


    public static class WorldPos {
        public static BlockPos blockPos;
        public static TripleInt sectionIndicies;
        public static int sectionIndex;
        public static final int SECTION_SZ = 16;

        public WorldPos(BlockPos pos, LevelChunk chunk) {
            blockPos = pos;
            setWorldPos(pos, chunk);
        }

        public WorldPos(TripleInt indices, int section, LevelChunk chunk) {
            sectionIndicies = indices;
            sectionIndex = section;
            setWorldPos(indices, section, chunk);
        }

        /** Setters **/

        public void setWorldPos(BlockPos pos, LevelChunk chunk)
        {
            blockPos = pos;

            //Convert worldPos to sectionIndicies
            final BlockPos chunkPos = chunk.getPos().getWorldPosition();
            final Integer Y_MIN = chunk.getMinBuildHeight();
            final Integer Y_MAX = chunk.getMaxBuildHeight();

            int x = pos.getX() - chunkPos.getX();
            int z = pos.getZ() - chunkPos.getZ();
            this.sectionIndex = (pos.getY() - Y_MIN) / SECTION_SZ;
            int y = (pos.getY() - Y_MIN) % SECTION_SZ;

            this.sectionIndicies = new TripleInt(x, y, z);

        }


        public void setWorldPos(TripleInt indices, int section, LevelChunk chunk)
        {
            final BlockPos chunkPos = chunk.getPos().getWorldPosition();
            final Integer Y_MIN = chunk.getMinBuildHeight();
            final Integer Y_MAX = chunk.getMaxBuildHeight();

             int x = chunkPos.getX() + indices.x;
             int y = Y_MIN + (SECTION_SZ * section);
             int z = chunkPos.getZ() + indices.z;

             this.blockPos = new BlockPos(x, y, z);
        }

        /** Getters **/

        public BlockPos getWorldPos() {
            return blockPos;
        }

        public TripleInt getIndices() {
            return sectionIndicies;
        }

        public Integer getSectionIndex() {
            return sectionIndex;
        }

        public int getX() {
            return blockPos.getX();
        }

        public int getY() {
            return blockPos.getY();
        }

        public int getZ() {
            return blockPos.getZ();
        }
    }
    //END WORLD POS




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

        public void addAll(Fast3DArray other)
        {
            for( int i = 0; i < other.size; i++)
            {
                add(other.X[i], other.Y[i], other.Z[i]);
            }
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Count: " + size + " ");
            String delimiter = "";
            for( int i = 0; i < size; i++)
            {
                sb.append(delimiter);
                sb.append("[" + X[i] + ", " + Y[i] + ", " + Z[i] + "]");
                delimiter = ", ";
            }
            return sb.toString();
        }
    }
}
