package com.holybuckets.foundation;


import com.holybuckets.foundation.database.DatabaseManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
* Class: HolyBucketsUtility
*
* Description: This class will contain utility methods amd objects that I find myself using frequently
*
 */
public class HolyBucketsUtility {

    public static DatabaseManager databaseManager = null;

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

    }

}
