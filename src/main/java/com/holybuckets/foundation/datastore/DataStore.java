package com.holybuckets.foundation.datastore;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.*;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.modelInterface.IStringSerializable;
import com.holybuckets.orecluster.OreClustersAndRegenMain;
import jdk.jfr.Unsigned;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerLifecycleEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import javax.annotation.Nullable;
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
    private Thread watchThread;
    private volatile boolean running = true;

    private DataStore(String worldId)
    {
        super();
        this.currentWorldId = worldId;
        STORE = new HashMap<>();
        String json = HBUtil.FileIO.loadJsonConfigs(DATA_STORE_FILE, DATA_STORE_FILE, new DefaultDataStore());
        this.deserialize(json);
        startWatchThread();
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

    public LevelSaveData getOrCreateLevelSaveData(String modId, LevelAccessor level) {
        WorldSaveData worldData = getOrCreateWorldSaveData(modId);
        return worldData.getOrCreateLevelSaveData(level);
    }

    static {
        EventRegistrar reg = EventRegistrar.getInstance();
        reg.registerOnModConfig(DataStore::initWorldOnConfigLoad);
    }
    public static void initWorldOnConfigLoad(ModConfigEvent event)
    {
        //Loading on world Start, Reloading, Unloading on World End
        //if(event.getConfig().getFileName() != "hbs_utility-server.toml")
        if( !(event.getConfig().getFileName().equals(OreClustersAndRegenMain.MODID + "-server.toml")) )
            return;

        if( event instanceof ModConfigEvent.Unloading )
            return;

        //on new world loading, set to null
        if( event instanceof ModConfigEvent.Loading )
        {
            String path = event.getConfig().getFullPath().toString();
            String[] dirs =  path.split("\\\\");
            INSTANCE = new DataStore( dirs[dirs.length - 3] );
        }

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
        //test if the string contains valid json
        if(jsonString == null || jsonString.isEmpty())
            return;

        String malformedJson = null;
        //test if JSON is not structured properly
        try {
            JsonParser.parseString(jsonString);
        } catch (Exception e)
        {
            StringBuilder error = new StringBuilder();
            error.append("Error parsing JSON data from Datastore, file: ");
            error.append(DATA_STORE_FILE);
            error.append(".  The datastore will be configured with defaults values. Copy the file to save your data. ");

            malformedJson = new String(jsonString);
            jsonString = new DefaultDataStore().serialize();
        }


        JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
        JsonArray modSaveDataArray = json.getAsJsonArray("modSaveData");
        modSaveDataArray.forEach(modSaveData -> {
            ModSaveData data = new ModSaveData(modSaveData.getAsJsonObject());
            STORE.put(data.getModId(), data);
        });

        if( malformedJson != null)
        {
            ModSaveData utilData = STORE.get(HBUtil.NAME);
            String asString = malformedJson.replace('"','\'' ).replaceAll("\r\n", "");
            utilData.addProperty("malformedJson",parse( asString ) );
            this.save();
        }


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

    private void save() {
        HBUtil.FileIO.serializeJsonConfigs(DATA_STORE_FILE, this.serialize());
    }

    private void startWatchThread() {
        watchThread = new Thread(this::threadWatchSave);
        watchThread.setDaemon(true);
        watchThread.setName("DataStore-AutoSave");
        watchThread.start();
    }

    private void threadWatchSave() {
        while (running) {
            try {
                Thread.sleep(30000); // 30 seconds
                if (running) {
                    save();
                }
            } catch (InterruptedException e) {
                // Interrupted, exit the thread
                break;
            }
        }
    }

    /** STATIC METHODS **/
    @Nullable
    public static DataStore getInstance() {
        return INSTANCE;
    }


    static {
        EventRegistrar.getInstance().registerOnServerStop(DataStore::shutdown);
    }
    public static void shutdown(ServerLifecycleEvent s)
    {
        if (INSTANCE != null) {
            WorldSaveData worldData = INSTANCE.getOrCreateWorldSaveData(HBUtil.NAME);
            GeneralConfig config = GeneralConfig.getInstance();

            {
                Long prevTicks = worldData.get("totalTicks").getAsLong();
                Long currentTicks = Integer.toUnsignedLong( config.getSERVER().getTickCount() );
                worldData.addProperty("totalTicks", parse(prevTicks + currentTicks) );
            }

            worldData.addProperty("worldSpawn", parse(config.getWORLD_SPAWN()) );

            // Stop the watch thread
            INSTANCE.running = false;
            if (INSTANCE.watchThread != null) {
                INSTANCE.watchThread.interrupt();
                try {
                    INSTANCE.watchThread.join(1000); // Wait up to 1 second for thread to finish
                } catch (InterruptedException e) {
                    // Ignore interruption during shutdown
                }
            }

            INSTANCE.save();

            //Clear all fields
            //INSTANCE.currentWorldId = null;
            //INSTANCE.STORE.clear();
            INSTANCE = null;
        }
    }

    static {
        EventRegistrar.getInstance().registerOnLevelUnload(DataStore::onWorldUnload, false);
    }
    private static void onWorldUnload(LevelEvent.Unload unload) {
        //INSTANCE.save();
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
