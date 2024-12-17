package com.holybuckets.foundation.datastore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.level.LevelAccessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.holybuckets.foundation.datastore.LevelSaveData.convertLevelId;

/**
 * Holds arbitrary data the library user would like to asociate with one or more world save files
 * For OreClustersAndRegen, this class is used to store world data on loaded chunks per World Save, per Level
 */
public class WorldSaveData {

    String worldId;
    final Map<String, JsonElement> properties;
    final Map<String, LevelSaveData> levelData;

    /** STATICS **/


    /** Constructor **/
    WorldSaveData(String worldId)
    {
        super();
        if(worldId == null)
            throw new IllegalArgumentException("World ID cannot be null");

        this.worldId = worldId;
        this.properties = new ConcurrentHashMap<>();
        this.levelData = new ConcurrentHashMap<>();

    }

    WorldSaveData(JsonObject json) {

        super();
        this.properties = new ConcurrentHashMap<>();
        this.levelData = new ConcurrentHashMap<>();
        this.fromJson(json);
    }

    public String getWorldId() {
        return worldId;
    }


    /**
     * Creates or gets the LevelSaveData for the given level
     * @param level
     * @return LevelSaveData object
     */
    public LevelSaveData getOrCreateLevelSaveData(LevelAccessor level) {
        String id = convertLevelId(level);
        LevelSaveData data = levelData.getOrDefault(id, new LevelSaveData(level));
        levelData.put(id, data);
        return data;
    }

    public void removeLevelSaveData(LevelAccessor level) {
        levelData.remove(convertLevelId(level));
    }

    public void createLevelSaveData(JsonElement json) {
        LevelSaveData data = new LevelSaveData(json.getAsJsonObject());
        levelData.put(data.levelId, data);
    }

    public void addProperty(String key, JsonElement data) {
        properties.put(key, data);
    }




    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("worldId", worldId);

        JsonArray levelDataArray = new JsonArray();
        levelData.forEach((levelId, levelSaveData) -> {
            levelDataArray.add(levelSaveData.toJson());
        });
        json.add("levelData", levelDataArray);

        this.properties.forEach(json::add);

        return json;
    }

    void fromJson(JsonObject json)
    {
        this.worldId = json.get("worldId").getAsString();
        json.remove("worldId");

        JsonArray levelData = json.getAsJsonArray("levelData");
        levelData.forEach(this::createLevelSaveData);
        json.remove("levelData");

        Map<String, JsonElement> map = json.asMap();
        map.forEach(this::addProperty);

    }

}
