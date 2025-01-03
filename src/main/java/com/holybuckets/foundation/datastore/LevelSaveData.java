package com.holybuckets.foundation.datastore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.level.LevelAccessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.holybuckets.foundation.HBUtil.LevelUtil;

/**
 * Stores a levelID and any data associated with a level that we want to persist
 */
public class LevelSaveData {
    String levelId;
    LevelAccessor level;
    final Map<String, JsonElement> properties;

    /** STATICS **/


    /** ######### **/


    /** Constructors **/

    public LevelSaveData(LevelAccessor level)
    {
        super();
        if(level == null)
            throw new IllegalArgumentException("Level cannot be null");
        this.level = level;
        this.levelId = LevelUtil.toId(level);
        this.properties = new ConcurrentHashMap<>();

    }

    public LevelSaveData(JsonObject json)
    {
        super();
        this.levelId = json.get("levelId").getAsString();
        this.level = null;
        this.properties = new ConcurrentHashMap<>();
        this.fromJson(json);
    }

    public LevelAccessor getLevel() {
        return level;
    }

    /**
     * Adds a property to the level data, replaces existing property if key already exists
     * @param key
     * @param data
     */
    public void addProperty(String key, JsonElement data) {
        properties.put(key, data);
    }

    /**
     * Gets a property from the level data
     * @param jsonProperty
     * @return null if property does not exist
     */
    public JsonElement get(String jsonProperty) {
        return properties.get(jsonProperty);
    }

    /** Serializers */

    JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("levelId", levelId);
        this.properties.forEach((key, value) -> {
            json.add(key, value);
        });

        return json;
    }

    public void fromJson(JsonObject json)
    {
        this.properties.clear();
        this.levelId = json.get("levelId").getAsString();
        json.remove("levelId");

        this.properties.putAll(json.asMap());
    }



}