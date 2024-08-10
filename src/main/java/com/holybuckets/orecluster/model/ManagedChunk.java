package com.holybuckets.orecluster.model;

import com.holybuckets.foundation.HolyBucketsUtility.ChunkUtil;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Class: ManagedChunk
 * Description: Dedicating a class to hold the many manged states of a chunk preparing for cluster generation
 *
 *  #Variables
 *  - ChunkAccess chunk: The chunk object
 *  - ChunkPos pos: The 2D position of the chunk in the world
 *  - String id: The unique id of the chunk
 *  - String status: The status of the chunk in the cluster generation process
 *
 *  - HashMap<String, Vec3i> clusters: The clusters in the chunk
 *  - isLoaded: The chunk is loaded
 *
 */

public class ManagedChunk {


    public static enum ClusterStatus {
        NONE,
        EXPLORED,
        CLEANED,
        GENERATED,
        MANIFESTED
    }

    /** Variables **/
    private ChunkAccess chunk;
    private ChunkPos pos;
    private String id;
    private ClusterStatus status;
    private HashMap<String, Vec3i> clusterTypes;
    private List<Pair<String, Vec3i>> clusters;



    /** Constructors **/

    //One for building with chunk
    public ManagedChunk(ChunkAccess chunk) {
        this.chunk = chunk;
        this.pos = chunk.getPos();
        this.id = ChunkUtil.getId( this.pos );
        this.status = ClusterStatus.NONE;
        this.clusterTypes = new HashMap<>();
        this.clusters = new LinkedList<>();
    }

    //One for building with id
    public ManagedChunk(String id) {
        this.chunk = null;
        this.id = id;
        this.pos = ChunkUtil.getPos( id );
        this.status = ClusterStatus.NONE;
        this.clusterTypes = new HashMap<>();
        this.clusters = new LinkedList<>();
    }



    /** Getters **/
    public ChunkAccess getChunk() {
        return chunk;
    }

    public ChunkPos getPos() {
        return pos;
    }

    public String getId() {
        return id;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    public List<Pair<String, Vec3i>> getClusters() {
        return clusters;
    }



    public HashMap<String, Vec3i> getClusterTypes() {
        return clusterTypes;
    }


    /** Setters **/

    public void setChunk(ChunkAccess chunk) {
        this.chunk = chunk;
    }

    public void setPos(ChunkPos pos) {
        this.pos = pos;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
    }

    public void setClusters(HashMap<String, Vec3i> clusters) {
        this.clusterTypes = clusters;
    }


    public void addClusterTypes(HashMap<String, Vec3i> clusterMap) {
        this.clusterTypes.putAll( clusterMap );
    }


}
