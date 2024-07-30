package com.holybuckets.foundation;


import net.minecraft.world.level.chunk.ChunkAccess;

/**
* Class: HolyBucketsUtility
*
* Description: This class will contain utility methods that I find myself using frequently
*
 */
public class HolyBucketsUtility {


    public static class Chunk {

        public static String getId(ChunkAccess chunk) {
            return chunk.getPos().x + "," + chunk.getPos().z;
        }

        public static String getId(int x, int z) {
            return x + "," + z;
        }

        public static String readNBT(ChunkAccess chunk, String property) {
            return "";
        }

        public static String writeNBT(ChunkAccess chunk, String property, String value) {
            return "";
        }

    }

}
