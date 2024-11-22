package com.holybuckets.foundation;

//MC Imports

//Forge Imports

import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class GeneralRealTimeConfig
{
    public static final String CLASS_ID = "000";

    /** World Data **/
    private static GeneralRealTimeConfig instance;
    private MinecraftServer SERVER;
    private Map<Integer, LevelAccessor> LEVELS;
    private Long WORLD_SEED;
    private Vec3i WORLD_SPAWN;
    private Boolean PLAYER_LOADED = false;

    private final List<Consumer<LevelEvent.Load>> ON_LEVEL_LOAD = new ArrayList<>();
    private final List<Consumer<PlayerEvent.PlayerLoggedInEvent>> ON_PLAYER_LOAD = new ArrayList<>();
    private final List<Consumer<LevelEvent.Unload>> ON_LEVEL_UNLOAD = new ArrayList<>();


        /** Constructor **/
        private GeneralRealTimeConfig()
        {
            this.LEVELS = new HashMap<>();

            instance = this;
            LoggerBase.logInit("000000", this.getClass().getName());
        }

        public static GeneralRealTimeConfig getInstance() {
            if( instance == null )
                return new GeneralRealTimeConfig();
            return instance;
        }

        /**
         * Events
         */

        //Helper
        public void onLoadLevel(LevelEvent.Load event )
        {
            // Capture the world seed, use logical server
            LevelAccessor level = event.getLevel();
            if( !level.isClientSide() )
            {
                MinecraftServer server = level.getServer();
                instance.WORLD_SEED = server.overworld().getSeed();
                instance.WORLD_SPAWN = server.overworld().getSharedSpawnPos();
                instance.LEVELS.put( level.hashCode(), level );
                instance.SERVER = level.getServer();

                LoggerBase.logInfo("000001","World Seed: " + instance.WORLD_SEED);
                LoggerBase.logInfo("000002","World Spawn: " + instance.WORLD_SPAWN);
            }

            //Call all registered functions
            for (Consumer<LevelEvent.Load> function : ON_LEVEL_LOAD) {
                function.accept(event);
            }
        }

        public void initPlayerConfigs( PlayerEvent.PlayerLoggedInEvent event )
        {
            {
                instance.PLAYER_LOADED = true;
            }

            for( Consumer<PlayerEvent.PlayerLoggedInEvent> function : ON_PLAYER_LOAD ) {
                function.accept(event);
            }
            LoggerBase.logDebug("006001", "Player Logged In");
        }

        public void onUnLoadLevel(LevelEvent.Unload event )
        {
            {
                //no General Configs
            }
            //Call all registered functions
            for (Consumer<LevelEvent.Unload> function : ON_LEVEL_UNLOAD) {
                function.accept(event);
            }
        }


        /**
         *  Getters
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

        public MinecraftServer getSERVER() { return SERVER; }
}

        /**
         *  Setters
         */

        //Create public methods for pushing functions onto each function event
        public void registerOnLevelLoad( Consumer<LevelEvent.Load> function ) {
            ON_LEVEL_LOAD.add( function );
        }

        public void registerOnPlayerLoad( Consumer<PlayerEvent.PlayerLoggedInEvent> function ) {
            ON_PLAYER_LOAD.add( function );
        }

        public void registerOnLevelUnload( Consumer<LevelEvent.Unload> function ) {
            ON_LEVEL_UNLOAD.add( function );
        }



//END CLASS