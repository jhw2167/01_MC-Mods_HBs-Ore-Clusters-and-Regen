package com.holybuckets.foundation.datastore;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.config.ConfigBase;
import com.holybuckets.foundation.modelInterface.IStringSerializable;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class DataStore - manages a simple persisted json file used as a datastore that holds
 * world and save file level data on the server side. To be read into memory at startup and serialized
 * periodically
 */
public class DataStore implements IStringSerializable {

    private static final String CLASS_ID = "007";

    private static DataStore INSTANCE;

    private final Map<String, ModSaveData> STORE;


    private DataStore()
    {
        super();
        STORE = new HashMap<>();
        File dataStoreFile = new File("hb_datastore.json");
        String json = HBUtil.FileIO.loadJsonConfig(dataStoreFile, dataStoreFile, new DefaultDataStore());
        this.deserialize(json);
    }

    private DataStore(List<ModSaveData> modData)
    {
        super();
        STORE = new HashMap<>();
        modData.forEach(this::addModSaveData);
    }

    public void addModSaveData(ModSaveData worldSaveData) {
        STORE.put(worldSaveData.getModId(), worldSaveData);
    }

    public void deserialize(String jsonString)
    {
        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray modSaveDataArray = json.getAsJsonArray("modSaveData");
        modSaveDataArray.forEach(modSaveData -> {
            ModSaveData data = new ModSaveData(modSaveData.getAsJsonObject());
            STORE.put(data.getModId(), data);
        });
    }

    public String serialize() {
        JsonObject json = new JsonObject();
        JsonArray modSaveDataArray = new JsonArray();
        STORE.forEach((modId, data) -> {
            modSaveDataArray.add(data.toJson());
        });
        json.add("modSaveData", modSaveDataArray);
        return json.toString();
    }

    /** STATIC METHODS **/
    public static DataStore getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DataStore();
        return INSTANCE;
    }

    /** SUBCLASSES **/

    private class DefaultDataStore extends DataStore implements IStringSerializable {
        public static final ModSaveData DATA = new ModSaveData(HBUtil.NAME);
        static {
            DATA.setComment("The purpose of this JSON file is to store data at the world save file level." +
             "This data is not intended to be modified by the user.");
        }
        public DefaultDataStore() {
            super(List.of(DATA));
        }

        @Override
        public String serialize() {
            return super.serialize();
        }

        @Override
        public void deserialize(String jsonString) {
            //Dummy
        }
    }


}
