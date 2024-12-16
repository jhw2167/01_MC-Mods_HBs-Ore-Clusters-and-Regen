package com.holybuckets.foundation.datastore;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.holybuckets.foundation.GeneralConfig;
import net.minecraft.world.level.LevelAccessor;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores a levelID and any data associated with a level that we want to persist
 */
public class LevelSaveData {
    String levelId;
    LevelAccessor level;
    final Map<String, JsonElement> properties;

    /** STATICS **/

    static String convertLevelId(LevelAccessor level) {
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


    public void addProperty(String key, JsonElement data) {
        properties.put(key, data);
    }

    /** Serializers */

    JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("levelId", levelId);
        this.properties.forEach((key, value) -> {
            json.add(key, value);
        });

        JsonArray array = new JsonArray();
        json.add("properties", array);
        return json;
    }

    public void fromJson(JsonObject json)
    {
        this.properties.clear();
        this.levelId = json.get("levelId").getAsString();
        json.remove("levelId");
        Map<String, JsonElement> map = json.asMap();
        map.forEach(this::addProperty);
    }


}