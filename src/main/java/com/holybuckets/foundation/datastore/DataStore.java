package com.holybuckets.foundation.datastore;

import com.google.gson.*;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.modelInterface.IStringSerializable;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import javax.lang.model.type.ArrayType;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Class DataStore - manages a simple persisted json file used as a datastore that holds
 * world and save file level data on the server side. To be read into memory at startup and serialized
 * periodically
 */
public class DataStore implements IStringSerializable {

    private static final String CLASS_ID = "007";

    private static DataStore INSTANCE;
    private static final File DATA_STORE_FILE = new File("hb_datastore.json");
    private final Map<String, ModSaveData> STORE;
    private String currentWorldId;

    private DataStore()
    {
        super();
        STORE = new HashMap<>();
        String json = HBUtil.FileIO.loadJsonConfigs(DATA_STORE_FILE, DATA_STORE_FILE, new DefaultDataStore());
        this.deserialize(json);
    }

    // Constructor for default data store
    private DataStore(ModSaveData data)
    {
        super();
        STORE = new HashMap<>();
        STORE.put(data.getModId(), data);
    }


    public ModSaveData getOrCreateModSavedData(String modId) {
        ModSaveData data = STORE.getOrDefault(modId, new ModSaveData(modId));
        STORE.put(modId, data);
        return data;
    }


    public WorldSaveData getOrCreateWorldSaveData(String modId) {
        ModSaveData modData = getOrCreateModSavedData(modId);
        return modData.getOrCreateWorldSaveData(currentWorldId);
    }

    public void initWorldOnConfigLoad(ModConfigEvent event)
    {
        //if(event.getConfig().getFileName() != "hbs_utility-server.toml")
        if( !(event.getConfig().getFileName().equals(OreClustersAndRegenMain.MODID + "-server.toml")) )
            return;

        String path = event.getConfig().getFullPath().toString();
        String[] dirs =  path.split("\\\\");
        this.currentWorldId = dirs[dirs.length - 3];
    }

    /**
     * Initialize a worldSaveData object on HB's Utility when a level loads
     * @param config
     * @return true if the worldSaveData object was initialized, false if it already exists
     */
    public boolean initWorldOnLevelLoad(GeneralConfig config)
    {
        ModSaveData modData = getOrCreateModSavedData(HBUtil.NAME);
        if (modData.worldSaveData.containsKey(currentWorldId))
            return false;

        WorldSaveData worldData = modData.getOrCreateWorldSaveData(currentWorldId);
        worldData.addProperty("worldSeed", parse(config.getWORLD_SEED()) );
        worldData.addProperty("worldSpawn", parse(config.getWORLD_SPAWN()) );
        worldData.addProperty("totalTicks", parse(Integer.valueOf(0)) );

        return true;
    }
    private static JsonElement parse(Object o) {
        return JsonParser.parseString( GeneralConfig.GSON.toJson(o) );
    }

    /** Serializers **/

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

    private void save() {
        HBUtil.FileIO.serializeJsonConfigs(DATA_STORE_FILE, this.serialize());
    }

    public static void shutdown() {
        INSTANCE.save();
        INSTANCE = null;
    }

    /** SUBCLASSES **/

    private class DefaultDataStore extends DataStore implements IStringSerializable {
        public static final ModSaveData DATA = new ModSaveData(HBUtil.NAME);
        static {
            DATA.setComment("The purpose of this JSON file is to store data at the world save file level for supporting HB" +
             " Utility Foundation mods. This data is not intended to be modified by the user.");
        }
        public DefaultDataStore() {
            super(DATA);
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
