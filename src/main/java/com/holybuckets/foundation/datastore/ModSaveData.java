package com.holybuckets.foundation.datastore;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Holds arbitrary data the library user would like to asociate with one or more world save files
 */
public class ModSaveData {

    private static final String CLASS_ID = "008";
    private String MOD_ID;
    private Map<String, JsonObject> WORLD_SAVE_DATA;

    private String comment;

    public ModSaveData(String modId) {
        super();
        MOD_ID = modId;
        WORLD_SAVE_DATA = new HashMap<>();
    }

    public ModSaveData(String modId, Map<String, JsonObject> worldSaveData) {
        this(modId);
        this.WORLD_SAVE_DATA.putAll(worldSaveData);
    }

    public ModSaveData(JsonObject worldSaveData) {
        this(worldSaveData.get("modId").getAsString());
        this.fromJson(worldSaveData);
    }

   /** GETTERS & SETTERS **/

    public String getModId() {
        return MOD_ID;
    }

    public void getWorldSaveData(String worldSaveName) {
        WORLD_SAVE_DATA.get(worldSaveName);
    }
    public void putWorldSaveData(String worldSaveName, JsonObject data) {
        WORLD_SAVE_DATA.put(worldSaveName, data);
    }

    public void clearWorldSaveData() {
        WORLD_SAVE_DATA.clear();
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.addProperty("modId", MOD_ID);
        if(comment != null)
            json.addProperty("comment", comment);
        JsonArray worldSaveDataArray = new JsonArray();
        WORLD_SAVE_DATA.forEach((k, v) -> {
            v.addProperty("worldName", k);
            worldSaveDataArray.add(v);
        });
        json.add("worldSaves", worldSaveDataArray);
        return json;
    }

    public void fromJson(JsonObject json)
    {
        this.MOD_ID = json.get("modId").getAsString();
        this.comment = json.get("comment").getAsString();
        Map<String, JsonObject> worldSaveData = new HashMap<>();
        json.getAsJsonArray("worldSaves").forEach((v) -> {
            JsonObject worldSave = v.getAsJsonObject();
            String worldName = worldSave.get("worldName").getAsString();
            worldSave.remove("worldName");
            worldSaveData.put(worldName, worldSave);
        });

    }

    public void setComment(String s) {
        this.comment = s;
    }
}
