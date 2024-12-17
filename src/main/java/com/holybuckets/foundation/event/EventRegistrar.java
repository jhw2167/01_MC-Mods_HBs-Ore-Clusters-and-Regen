package com.holybuckets.foundation.event;

//MC Imports

//Forge Imports

import com.holybuckets.foundation.LoggerBase;
import com.holybuckets.foundation.datastore.DataStore;
import com.holybuckets.foundation.model.ManagedChunk;
import jdk.jfr.Event;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;

import java.util.*;
import java.util.function.Consumer;


/**
 * Class: GeneralRealTimeConfig
 *
 * Description: Fundamental world configs, singleton

 */
public class EventRegistrar {
    public static final String CLASS_ID = "010";

    /**
     * World Data
     **/
    private static EventRegistrar instance;

    private final Deque<Consumer<LevelEvent.Load>> ON_LEVEL_LOAD = new ArrayDeque<>();
    private final Deque<Consumer<PlayerEvent.PlayerLoggedInEvent>> ON_PLAYER_LOAD = new ArrayDeque<>();
    private final Deque<Consumer<LevelEvent.Unload>> ON_LEVEL_UNLOAD = new ArrayDeque<>();

    private final Deque<Consumer<ChunkEvent.Load>> ON_CHUNK_LOAD = new ArrayDeque<>();
    private final Deque<Consumer<ChunkEvent.Unload>> ON_CHUNK_UNLOAD = new ArrayDeque<>();


    /**
     * Constructor
     **/
    private EventRegistrar()
    {
        super();
        LoggerBase.logInit( null, "010000", this.getClass().getName());

        instance = this;
    }

    public static EventRegistrar getInstance() {
        if (instance == null)
            return new EventRegistrar();
        return instance;
    }

    /**
     * Events
     */


    /** Level Events **/


    public void onLoadLevel(LevelEvent.Load event)
    {
        //Call all registered functions
        for (Consumer<LevelEvent.Load> function : ON_LEVEL_LOAD) {
            function.accept(event);
        }
    }


    public void onUnLoadLevel(LevelEvent.Unload event)
    {
        //Call all registered functions
        for (Consumer<LevelEvent.Unload> function : ON_LEVEL_UNLOAD) {
            function.accept(event);
        }
    }

    /** ############### **/



    /** Chunk Events **/

    public void onChunkLoad(final ChunkEvent.Load event)
    {

        for (Consumer<ChunkEvent.Load> function : ON_CHUNK_LOAD) {
            function.accept(event);
        }

    }

    public void onChunkUnload(final ChunkEvent.Unload event)
    {

        for (Consumer<ChunkEvent.Unload> function : ON_CHUNK_UNLOAD) {
            function.accept(event);
        }

    }




    /** ############### **/


    /** Player Events **/

    public void initPlayerConfigs(PlayerEvent.PlayerLoggedInEvent event)
    {

        for (Consumer<PlayerEvent.PlayerLoggedInEvent> function : ON_PLAYER_LOAD) {
            function.accept(event);
        }
        LoggerBase.logDebug( null,"010001", "Player Logged In");
    }

    /** ############### **/


    /**
     * Getters
     */


    /**
     * Setters
     */

    //Create public methods for pushing functions onto each function event
    public <T> void  generalRegister(Consumer<T> function, Deque<Consumer<T>> array, boolean priority) {
        if (priority)
            array.addFirst(function);
        else
            array.add(function);
    }

    public void registerOnLevelLoad(Consumer<LevelEvent.Load> function) { registerOnLevelLoad(function, false);}
    public void registerOnLevelLoad(Consumer<LevelEvent.Load> function, boolean priority) {
        generalRegister(function, ON_LEVEL_LOAD, priority);
    }

    public void registerOnPlayerLoad(Consumer<PlayerEvent.PlayerLoggedInEvent> function) { registerOnPlayerLoad(function, false);}
    public void registerOnPlayerLoad(Consumer<PlayerEvent.PlayerLoggedInEvent> function, boolean priority) {
        generalRegister(function, ON_PLAYER_LOAD, priority);
    }

    public void registerOnLevelUnload(Consumer<LevelEvent.Unload> function) { registerOnLevelUnload(function, false);}
    public void registerOnLevelUnload(Consumer<LevelEvent.Unload> function, boolean priority) {
        generalRegister(function, ON_LEVEL_UNLOAD, priority);
    }

    public void registerOnChunkLoad(Consumer<ChunkEvent.Load> function) { registerOnChunkLoad(function, false);}
    public void registerOnChunkLoad(Consumer<ChunkEvent.Load> function, boolean priority) {
        generalRegister(function, ON_CHUNK_LOAD, priority);
    }

    public void registerOnChunkUnload(Consumer<ChunkEvent.Unload> function) { registerOnChunkUnload(function, false);}
    public void registerOnChunkUnload(Consumer<ChunkEvent.Unload> function, boolean priority) {
        generalRegister(function, ON_CHUNK_UNLOAD, priority);
    }




}
//END CLASS