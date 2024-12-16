package com.holybuckets.foundation;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.model.ManagedChunk;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class GeneralConfig {
    public static final String CLASS_ID = "000";

    /**
     * World Data
     **/
    private static GeneralConfig instance;
    private static final EventRegistrar eventRegistrar = EventRegistrar.getInstance();
    private final DataStore DATA_STORE;

    private MinecraftServer SERVER;
    private final Map<Integer, LevelAccessor> LEVELS;
    private Long WORLD_SEED;
    private Vec3i WORLD_SPAWN;
    private Boolean PLAYER_LOADED = false;

    /**
     * Constructor
     **/
    private GeneralConfig()
    {
        super();
        LoggerBase.logInit( null, "000000", this.getClass().getName());
        this.DATA_STORE = DataStore.getInstance();
        this.LEVELS = new HashMap<>();

        //Register Events
        eventRegistrar.registerOnLevelLoad(this::onLoadLevel);

        instance = this;
    }

    public static GeneralConfig getInstance() {
        if (instance == null)
            return new GeneralConfig();
        return instance;
    }

    /**
     * Events
     */


    /** Level Events **/

    public void onLoadLevel(LevelEvent.Load event)
    {
        // Capture the world seed, use logical server
        LevelAccessor level = event.getLevel();
        if (!level.isClientSide())
        {
            MinecraftServer server = level.getServer();
            instance.WORLD_SEED = server.overworld().getSeed();
            instance.WORLD_SPAWN = server.overworld().getSharedSpawnPos();
            instance.LEVELS.put(level.hashCode(), level);
            instance.SERVER = level.getServer();

            LoggerBase.logInfo( null, "010001", "World Seed: " + instance.WORLD_SEED);
            LoggerBase.logInfo( null, "010002", "World Spawn: " + instance.WORLD_SPAWN);
        }

    }

    public void onUnLoadLevel(LevelEvent.Unload event)  {
        //not implemented
    }

    public void initPlayerConfigs(PlayerEvent.PlayerLoggedInEvent event)
    {
        PLAYER_LOADED = true;
        LoggerBase.logDebug( null,"006001", "Player Logged In");
    }

    /**
     * Getters
     */
    public Map<Integer, LevelAccessor> getLEVELS() {
        return LEVELS;
    }

    public Long getWORLD_SEED() {
        return WORLD_SEED;
    }

    public Vec3i getWORLD_SPAWN() {
        return WORLD_SPAWN;
    }

    public Boolean getIsPLAYER_LOADED() {
        return PLAYER_LOADED;
    }

    public MinecraftServer getSERVER() {
        return SERVER;
    }


    /**
     * Setters
     */






}
//END CLASS