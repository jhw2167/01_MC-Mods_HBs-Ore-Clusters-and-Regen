package com.holybuckets.orecluster.config.model;

import com.holybuckets.foundation.ConfigModelBase;
import com.holybuckets.foundation.LoggerBase;
import net.minecraft.world.level.block.Block;
import org.antlr.v4.runtime.misc.Triple;

//Java
import java.util.*;
import java.util.stream.Collectors;

import com.holybuckets.orecluster.config.COreClusters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OreClusterConfigModel extends ConfigModelBase {

    public Long subSeed = null;
    public String oreClusterType = "default";
    public HashSet<String> validOreClusterOreBlocks;
    public Integer oreClusterSpawnRate = COreClusters.DEF_ORE_CLUSTER_SPAWN_RATE;
    public Triple<Integer, Integer, Integer> oreClusterVolume = processVolume( oreClusterType, COreClusters.DEF_ORE_CLUSTER_VOLUME);
    public Float oreClusterDensity = COreClusters.DEF_ORE_CLUSTER_DENSITY;
    public String oreClusterShape = COreClusters.DEF_ORE_CLUSTER_SHAPE;
    public Integer oreClusterMaxYLevelSpawn = COreClusters.ORE_CLUSTER_MAX_Y_LEVEL_SPAWN;
    public Integer minChunksBetweenOreClusters = COreClusters.MIN_CHUNKS_BETWEEN_ORE_CLUSTERS;
    public Integer maxChunksBetweenOreClusters = COreClusters.MAX_CHUNKS_BETWEEN_ORE_CLUSTERS;
    public Float oreVeinModifier = COreClusters.DEF_ORE_VEIN_MODIFIER;
    public HashSet<String> oreClusterNonReplaceableBlocks = processReplaceableBlocks(COreClusters.ORE_CLUSTER_NONREPLACEABLE_BLOCKS);
    public HashSet<String> oreClusterReplaceableEmptyBlocks = processReplaceableBlocks(COreClusters.ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCKS);
    public Boolean oreClusterDoesRegenerate = COreClusters.REGENERATE_ORE_CLUSTERS;
    public Map<String, Integer> oreClusterRegenPeriods = null;

    private static final Gson gson = new GsonBuilder().create();
    private static final COreClusters oreClusterDefaultConfigs = new COreClusters(); //Used for default values


    public OreClusterConfigModel(Block oreClusterBlock ) {
        this.oreClusterType = oreClusterBlock.toString();
        this.oreClusterType = processMinecraftBlockTypeSyntax(oreClusterType);
    }

    public OreClusterConfigModel(String oreClusterJson) {
        deserialize(oreClusterJson);
    }

    public OreClusterConfigModel( COreClusters cOreClusters ) {

        if( cOreClusters == null ) {
            return;
        }
        if( cOreClusters.subSeed.get() != null || !cOreClusters.subSeed.get().isEmpty() )
            this.subSeed = (long) cOreClusters.subSeed.get().hashCode();  //initialized to null
        else
            this.subSeed = null;

        this.validOreClusterOreBlocks = new HashSet<>(
            processValidOreClusterOreBlocks(cOreClusters.validOreClusterOreBlocks.get()));
        this.oreClusterSpawnRate = cOreClusters.defaultOreClusterSpawnRate.get();
        this.oreClusterVolume = processVolume(oreClusterType, cOreClusters.defaultOreClusterVolume.get());
        this.oreClusterDensity = cOreClusters.defaultOreClusterDensity.getF();
        this.oreClusterShape = cOreClusters.defaultOreClusterShape.get();
        this.oreClusterMaxYLevelSpawn = cOreClusters.oreClusterMaxYLevelSpawn.get();
        this.minChunksBetweenOreClusters = cOreClusters.minChunksBetweenOreClusters.get();
        this.maxChunksBetweenOreClusters = cOreClusters.maxChunksBetweenOreClusters.get();
        this.oreVeinModifier = cOreClusters.defaultOreVeinModifier.getF();
        this.oreClusterNonReplaceableBlocks = processReplaceableBlocks(cOreClusters.defaultOreClusterNonReplaceableBlocks.get());
        this.oreClusterReplaceableEmptyBlocks = processReplaceableBlocks(cOreClusters.defaultOreClusterReplaceableEmptyBlocks.get());
        this.oreClusterDoesRegenerate = cOreClusters.regenerateOreClusters.get();

        //Iterate through the oreClusterRegenPeriods and add them to the map
        oreClusterRegenPeriods = new HashMap<>();
        this.oreClusterRegenPeriods = processRegenPeriods(
            cOreClusters.regenerateOreClusterUpgradeItems.get().split(","),
            cOreClusters.regenerateOreClusterPeriodLengths.get().split(","));

        }
    //END CONSTRUCTOR

    /*
        @javadoc
        Utility classes that read string properties into proper formats
     */

    public static String processMinecraftBlockTypeSyntax(String blockType) {
        if( blockType == null || blockType.isEmpty() )
            return null;

        return blockType.replace("Block{", "").replace("}", "");
    }

    public static List<String> processValidOreClusterOreBlocks(String validOreClusterOreBlocks) {
        return new ArrayList<>(Arrays.asList(validOreClusterOreBlocks.split(",")));
    }

    //Setup static methods to process oreClusterReplaceableBlocks and oreClusterReplaceableEmptyBlock
    public static HashSet<String> processReplaceableBlocks(String replaceableBlocks) {

        return Arrays.stream(replaceableBlocks.split(",")) //Split the string by commas
                .map(String::trim) //Trim each element
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static Triple<Integer, Integer, Integer> processVolume(String ore, String volume)
    {
        /** Define Errors for validation **/
        StringBuilder volumeNotParsedCorrectlyError = new StringBuilder();
        volumeNotParsedCorrectlyError.append("Volume value: ");
        volumeNotParsedCorrectlyError.append(volume);
        volumeNotParsedCorrectlyError.append(" is not formatted correctly for ore: ");
        volumeNotParsedCorrectlyError.append(ore);
        volumeNotParsedCorrectlyError.append(" Using default cluster volume of 32x32x32 instead");

        StringBuilder volumeNotWithinBoundsError = new StringBuilder();
        volumeNotWithinBoundsError.append("Volume value: ");
        volumeNotWithinBoundsError.append(volume);
        volumeNotWithinBoundsError.append(" is out of bounds for ore: ");
        volumeNotWithinBoundsError.append(ore);
        volumeNotWithinBoundsError.append(" Using default cluster volume of 32x32x32 instead");

        /********************************/


        String[] volumeArray = volume.toLowerCase().split("x");
        if(volume == null || volume.isEmpty() || volumeArray.length != 3) {
            LoggerBase.logWarning(volumeNotParsedCorrectlyError.toString());
            volumeArray = COreClusters.DEF_ORE_CLUSTER_VOLUME.split("x");
        }

        String[] mins = COreClusters.MIN_ORE_CLUSTER_VOLUME.split("x");
        String[] maxs = COreClusters.MAX_ORE_CLUSTER_VOLUME.split("x");

        //Validate we are within MIN and MAX
        for (int i = 0; i < 3; i++) {
            int vol = Integer.parseInt(volumeArray[i]);
            int min = Integer.parseInt(mins[i]);
            int max = Integer.parseInt(maxs[i]);
            if (vol < min || vol > max) {
                LoggerBase.logWarning(volumeNotWithinBoundsError.toString());
                volumeArray = COreClusters.DEF_ORE_CLUSTER_VOLUME.split("x");
                break;
            }
        }

        return new Triple<>(Integer.parseInt(volumeArray[0]),
                Integer.parseInt(volumeArray[1]),
                Integer.parseInt(volumeArray[2]));
    }

    public static HashMap<String, Integer> processRegenPeriods(String [] upgrades, String [] oreClusterRegenPeriodArray) {
        HashMap<String, Integer> oreClusterRegenPeriods = new HashMap<>();

        int i = 0;
        try {
            for (String item : upgrades) {
                //before putting into map,check if there is a valid corresponding length
                if (i < oreClusterRegenPeriodArray.length) {
                    oreClusterRegenPeriods.put(item, Integer.parseInt(oreClusterRegenPeriodArray[i].trim()));
                    i++;
                } else {
                    //If there is no corresponding length, use last number we got
                    oreClusterRegenPeriods.put(item, Integer.parseInt(oreClusterRegenPeriodArray[i].trim()));
                }

            }
        }
        catch (NumberFormatException e) {
            //Reset map to default values given error
            StringBuilder error = new StringBuilder();
            error.append("Error parsing oreClusterRegenPeriods, use comma separated list of integers" +
             " default values have been set instead");
            LoggerBase.logWarning(error.toString());
            oreClusterRegenPeriods = new HashMap<>();
            upgrades = COreClusters.REGENERATE_ORE_CLUSTER_UPGRDADE_ITEMS.split(",");
            oreClusterRegenPeriodArray = COreClusters.REGENERATE_ORE_CLUSTER_PERIOD_LENGTHS.split(",");
            i = 0;
            for (String item : upgrades) {
                oreClusterRegenPeriods.put(item, Integer.parseInt(oreClusterRegenPeriodArray[i]));
                i++;
            }
        }
        return oreClusterRegenPeriods;
    }



    /*
        @javadoc
        Setter Functions
     */

     public void setOreClusterType(String oreClusterType) {
        this.oreClusterType = processMinecraftBlockTypeSyntax(oreClusterType);
     }

    public void setOreClusterSpawnRate(Integer oreClusterSpawnRate) {
        Boolean validConfig = validateInteger(oreClusterSpawnRate, oreClusterDefaultConfigs.defaultOreClusterSpawnRate,
        "for ore: " + this.oreClusterType);

        if( validConfig )
            this.oreClusterSpawnRate = oreClusterSpawnRate;
    }

    public void setOreClusterVolume(String oreClusterVolume) {
        Triple<Integer, Integer, Integer> volume = processVolume(oreClusterType, oreClusterVolume);
        this.oreClusterVolume = volume;
    }

    public void setOreClusterShape(String oreClusterShape)
    {
        StringBuilder error = new StringBuilder();
        error.append("Error setting oreClusterShape for ore: ");
        error.append(oreClusterType);
        error.append(" using default value of shape: ");
        error.append(COreClusters.DEF_ORE_CLUSTER_SHAPE + " instead");

        if( oreClusterShape == null || oreClusterShape.isEmpty() )
            oreClusterShape = COreClusters.DEF_ORE_CLUSTER_SHAPE;

        if( !COreClusters.ORE_CLUSTER_VALID_SHAPES.contains( oreClusterShape ) ) {
            oreClusterShape = COreClusters.DEF_ORE_CLUSTER_SHAPE;
            LoggerBase.logWarning(error.toString());
        }

        this.oreClusterShape = oreClusterShape;
    }

    public void setOreClusterDensity(Float oreClusterDensity)
    {
        Boolean validConfig = validateFloat(oreClusterDensity, oreClusterDefaultConfigs.defaultOreClusterDensity,
         "for ore: " + this.oreClusterType);

        if( validConfig )
            this.oreClusterDensity = oreClusterDensity;

    }

    public void setOreClusterMaxYLevelSpawn(Integer oreClusterMaxYLevelSpawn) {
        Boolean validConfig = validateInteger(oreClusterMaxYLevelSpawn, oreClusterDefaultConfigs.oreClusterMaxYLevelSpawn,
        "for ore: " + this.oreClusterType);

        if( validConfig )
            this.oreClusterMaxYLevelSpawn = oreClusterMaxYLevelSpawn;
    }

    public void setMinChunksBetweenOreClusters(Integer minChunksBetweenOreClusters)
    {
        String minChunksLogicError = "minChunksBetweenOreClusters is too high for the spawnrate of the cluster";

        Boolean validConfig = validateInteger(minChunksBetweenOreClusters, oreClusterDefaultConfigs.minChunksBetweenOreClusters,
        "for ore: " + this.oreClusterType);

        if( validConfig )
        {
            //Validate there is enough cluster space to meet expected chunks per cluster
            double chunkAreaReservedPerCluster = Math.pow( 2*minChunksBetweenOreClusters + 1, 2) / 2;
            double expectedChunksPerCluster = (COreClusters.DEF_ORE_CLUSTER_SPAWNRATE_AREA / oreClusterSpawnRate);

            if (chunkAreaReservedPerCluster < expectedChunksPerCluster)
            {
                this.oreClusterSpawnRate = (int) (COreClusters.DEF_ORE_CLUSTER_SPAWN_RATE / chunkAreaReservedPerCluster);
                logPropertyWarning(minChunksLogicError, this.oreClusterType,
                "scaling down oreClusterSpawnrate to ", this.oreClusterSpawnRate.toString());
            }

            this.minChunksBetweenOreClusters = minChunksBetweenOreClusters;
        }

    }

    public void setMaxChunksBetweenOreClusters(Integer maxChunksBetweenOreClusters)
    {
        String maxChunksLogicError = "maxChunksBetweenOreClusters is too low for the spawnrate of the cluster ";

        Boolean validConfig = validateInteger(maxChunksBetweenOreClusters, oreClusterDefaultConfigs.maxChunksBetweenOreClusters,
        "for ore: " + this.oreClusterType);

        if( validConfig )
        {
            //Validate there is enough cluster space to meet expected chunks per cluster

            double minimumClustersPerArea = COreClusters.DEF_ORE_CLUSTER_SPAWNRATE_AREA /
                 Math.pow( 2*maxChunksBetweenOreClusters + 1, 2) ;

            if ( ( this.oreClusterSpawnRate / 2 ) < minimumClustersPerArea )
            {
                this.oreClusterSpawnRate = (int) minimumClustersPerArea * 2;
                logPropertyWarning(maxChunksLogicError, this.oreClusterType,
                "scaling up oreClusterSpawnrate to ", this.oreClusterSpawnRate.toString());
            }

            this.maxChunksBetweenOreClusters = maxChunksBetweenOreClusters;

        }

    }

    public void setOreVeinModifier(Float oreVeinModifier) {
        Boolean validConfig = validateFloat(oreVeinModifier, oreClusterDefaultConfigs.defaultOreVeinModifier,
        "for ore: " + this.oreClusterType);

        if( validConfig )
            this.oreVeinModifier = oreVeinModifier;
    }

    public void setOreClusterNonReplaceableBlocks(String oreClusterNonReplaceableBlocks) {
        this.oreClusterNonReplaceableBlocks = processReplaceableBlocks(oreClusterNonReplaceableBlocks);
    }

    public void setOreClusterReplaceableEmptyBlocks(String oreClusterReplaceableEmptyBlocks) {
        this.oreClusterReplaceableEmptyBlocks = processReplaceableBlocks(oreClusterReplaceableEmptyBlocks);
        //If an entry does not contain ':' add the minecraft namespace
        this.oreClusterReplaceableEmptyBlocks = this.oreClusterReplaceableEmptyBlocks.stream()
                .map(block -> block.contains(":") ? block : "minecraft:" + block)
                .collect(Collectors.toCollection(HashSet::new));

    }

    public void setOreClusterDoesRegenerate(String oreClusterDoesRegenerate) {
        this.oreClusterDoesRegenerate = parseBoolean(oreClusterDoesRegenerate);
    }


    private static void logPropertyWarning(String message, String ore, String defaultMessage, String defaultValue)
    {
        if( defaultMessage == null )
            defaultMessage = " Using default value of ";

        StringBuilder error = new StringBuilder();
        error.append(message);
        error.append(" for ore: ");
        error.append(ore);
        error.append( defaultMessage );
        error.append(defaultValue);
        LoggerBase.logWarning(error.toString());
    }


    /*
        @javadoc
        Serialize and Deserialize the config for specific ores from JSON strings

      *************
     */


    public String serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("oreClusterType", oreClusterType);
        jsonObject.addProperty("oreClusterSpawnRate", oreClusterSpawnRate);
        jsonObject.addProperty("oreClusterVolume", oreClusterVolume.a
                + "x" + oreClusterVolume.b
                + "x" + oreClusterVolume.c
        );
        jsonObject.addProperty("oreClusterDensity", oreClusterDensity);
        jsonObject.addProperty("oreClusterShape", oreClusterShape);
        jsonObject.addProperty("oreClusterMaxYLevelSpawn", oreClusterMaxYLevelSpawn);
        jsonObject.addProperty("minChunksBetweenOreClusters", minChunksBetweenOreClusters);
        jsonObject.addProperty("maxChunksBetweenOreClusters", maxChunksBetweenOreClusters);

        jsonObject.addProperty("oreVeinModifier", oreVeinModifier);
        jsonObject.addProperty("oreClusterNonReplaceableBlocks",
            oreClusterNonReplaceableBlocks.stream().collect(Collectors.joining(", ")));
        jsonObject.addProperty("oreClusterReplaceableEmptyBlocks",
            oreClusterReplaceableEmptyBlocks.stream().collect(Collectors.joining(", ")));
        jsonObject.addProperty("oreClusterDoesRegenerate", oreClusterDoesRegenerate);

        //System.err.println("jsonObject: " + jsonObject);
        return gson.toJson(jsonObject).
                //replace("\",", "\"," + System.getProperty("line.separator") ).
                        replace('"', "'".toCharArray()[0]);
    }

    public void deserialize(String jsonString) {
        JsonObject jsonObject = JsonParser.parseString(jsonString.replace("'".toCharArray()[0], '"')).getAsJsonObject();

        try {
            setOreClusterType(jsonObject.get("oreClusterType").getAsString());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing oreClusterType for an undefined ore" + e.getMessage());
        }

        try {
            setOreClusterSpawnRate(jsonObject.get("oreClusterSpawnRate").getAsInt());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.defaultOreClusterSpawnRate.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreClusterVolume(jsonObject.get("oreClusterVolume").getAsString());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.defaultOreClusterVolume.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreClusterDensity(jsonObject.get("oreClusterDensity").getAsFloat());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.defaultOreClusterDensity.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreClusterShape(jsonObject.get("oreClusterShape").getAsString());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.defaultOreClusterShape.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreClusterMaxYLevelSpawn(jsonObject.get("oreClusterMaxYLevelSpawn").getAsInt());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.oreClusterMaxYLevelSpawn.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setMinChunksBetweenOreClusters(jsonObject.get("minChunksBetweenOreClusters").getAsInt());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.minChunksBetweenOreClusters.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setMaxChunksBetweenOreClusters(jsonObject.get("maxChunksBetweenOreClusters").getAsInt());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.maxChunksBetweenOreClusters.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreVeinModifier(jsonObject.get("oreVeinModifier").getAsFloat());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.defaultOreVeinModifier.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreClusterNonReplaceableBlocks(jsonObject.get("oreClusterNonReplaceableBlocks").getAsString());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.defaultOreClusterNonReplaceableBlocks.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreClusterReplaceableEmptyBlocks(jsonObject.get("oreClusterReplaceableEmptyBlocks").getAsString());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.defaultOreClusterReplaceableEmptyBlocks.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        try {
            setOreClusterDoesRegenerate(jsonObject.get("oreClusterDoesRegenerate").getAsString());
        } catch (Exception e) {
            LoggerBase.logError("Error parsing " +
            oreClusterDefaultConfigs.regenerateOreClusters.getName() + " for ore: " + this.oreClusterType + ". " + e.getMessage());
        }

        StringBuilder complete = new StringBuilder();
        complete.append("OreClusterConfigModel for ");
        complete.append(oreClusterType);
        complete.append(" has been created with the following properties: \n");
        complete.append(serialize());
        complete.append("\n\n");
        LoggerBase.logInfo(complete.toString());
    }



}

