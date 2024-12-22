package com.holybuckets.foundation.datastore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Holds arbitrary data the library user would like to asociate with one or more world save files
 */
public class ModSaveData {

    private static final String CLASS_ID = "008";
    String MOD_ID;
    Map<String, JsonElement> properties;
    Map<String, WorldSaveData> worldSaveData;

    String comment;

    ModSaveData(String modId) {
        super();
        MOD_ID = modId;
        properties = new ConcurrentHashMap<>();
        worldSaveData = new ConcurrentHashMap<>();
    }


    ModSaveData(JsonObject worldSaveData) {
        this(worldSaveData.get("modId").getAsString());
        this.fromJson(worldSaveData);
    }

    public void createWorldSaveData(JsonElement json) {
        WorldSaveData w = new WorldSaveData(json.getAsJsonObject());
        this.worldSaveData.put(w.getWorldId(), w);
    }

   /** GETTERS & SETTERS **/

    public String getModId() {
        return MOD_ID;
    }

    public WorldSaveData getOrCreateWorldSaveData(String worldId) {
        WorldSaveData data = worldSaveData.getOrDefault(worldId, new WorldSaveData(worldId));
        worldSaveData.put(worldId, data);
        return data;
    }

    public void addProperty(String key, JsonElement data) {
        properties.put(key, data);
    }

    public void clearWorldSaveData() {
        worldSaveData.clear();
    }

    JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("modId", MOD_ID);
        if(comment != null)
            json.addProperty("comment", comment);

        this.properties.forEach(json::add);

        JsonArray worldSaveDataArray = new JsonArray();
        worldSaveData.forEach((key, value) -> {
            worldSaveDataArray.add(value.toJson());
        });
        json.add("worldSaves", worldSaveDataArray);


        return json;
    }

    private void fromJson(JsonObject json)
    {
        this.properties.clear();


        this.MOD_ID = json.get("modId").getAsString();
        json.remove("modId");

        JsonElement comment = json.get("comment");
        if(comment != null)
        {
            this.comment = json.get("comment").getAsString();
            json.remove("comment");
        }


        JsonArray worldSaves = json.getAsJsonArray("worldSaves");
        worldSaves.forEach(this::createWorldSaveData);
        json.remove("worldSaves");

        this.properties.putAll(json.asMap());

    }

    public void setComment(String s) {
        this.comment = s;
    }


}
