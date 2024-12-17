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

   /** GETTERS & SETTERS **/

    public String getModId() {
        return MOD_ID;
    }

    public WorldSaveData getOrCreateWorldSaveData(String worldId) {
        WorldSaveData data = worldSaveData.getOrDefault(worldId, new WorldSaveData(worldId));
        worldSaveData.put(worldId, data);
        return data;
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

        JsonArray worldSaveDataArray = new JsonArray();
        worldSaveData.forEach((id, data) -> {
            worldSaveDataArray.add(data.toJson());
        });
        json.add("worldSaves", worldSaveDataArray);

        this.properties.forEach(json::add);

        return json;
    }

    private void fromJson(JsonObject json)
    {
        this.MOD_ID = json.get("modId").getAsString();
        json.remove("modId");
        this.comment = json.get("comment").getAsString();
        json.remove("comment");

        json.getAsJsonArray("worldSaves").forEach(worldSave -> {
        WorldSaveData w = new WorldSaveData(worldSave.getAsJsonObject());
            this.worldSaveData.put(w.getWorldId(), w);
        });
        json.remove("worldSaves");

        this.properties.putAll(json.asMap());

    }

    public void setComment(String s) {
        this.comment = s;
    }


}
