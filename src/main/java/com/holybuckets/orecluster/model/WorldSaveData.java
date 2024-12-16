package com.holybuckets.orecluster.model;

import net.minecraft.world.level.LevelAccessor;

import javax.json.JsonObject;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds arbitrary data the library user would like to asociate with one or more world save files
 * For OreClustersAndRegen, this class is used to store world data on loaded chunks per World Save, per Level
 */
public class WorldSaveData {

    final String worldId;
    final Map<String, LevelSaveData> levelData;

    /** STATICS **/

    final static Map<String, WorldSaveData> WORLDS = new ConcurrentHashMap<>();

    public static WorldSaveData getWorldSaveData(String worldId) {
        return WORLDS.get(worldId);
    }

    public static void deleteWorldSaveData(String worldId) {
        WORLDS.remove(worldId);
    }


    /** Constructor **/
    public WorldSaveData(String worldId)
    {
        super();
        if(worldId == null)
            throw new IllegalArgumentException("World ID cannot be null");

        this.worldId = worldId;
        this.levelData = new ConcurrentHashMap<>();
        WORLDS.put(worldId, this);
    }


    /**
     * Stores a levelID and any data associated with a level that we want to persist
     */
    private class LevelSaveData {
        final String levelId;
        final LevelAccessor level;
        final Set<String> chunkIds;

        /** STATICS **/
        static final Map<String, LevelSaveData> LEVELS = new ConcurrentHashMap<>();

        public static LevelSaveData getLevelSaveData(LevelAccessor level) {
            return LEVELS.get(convertLevelId(level));        }


        private static String convertLevelId(LevelAccessor level) {
            return level.dimensionType().toString();
        }


        /** ######### **/


        /** Constructors **/

        public LevelSaveData(LevelAccessor level)
        {
            super();
            if(level == null)
                throw new IllegalArgumentException("Level cannot be null");
            this.level = level;
            this.levelId = convertLevelId(level);
            this.chunkIds = new LinkedHashSet<>();

            LEVELS.put(levelId, this);
        }

        public void addChunkId(String chunkId) {
            chunkIds.add(chunkId);
        }

        /**
         * Returns the original reference to the chunkIds set
         * @return
         */
        public Set<String> getChunkIds() {
            return chunkIds;
        }


    }

}
