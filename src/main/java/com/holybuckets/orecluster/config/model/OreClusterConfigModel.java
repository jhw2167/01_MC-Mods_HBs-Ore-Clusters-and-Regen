package com.holybuckets.orecluster.config.model;

import com.holybuckets.foundation.ConfigBase;
import com.holybuckets.foundation.ConfigModelBase;
import com.holybuckets.foundation.LoggerBase;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
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

    public String subSeed = null;
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
    public HashSet<String> oreClusterReplaceableBlocks = processReplaceableBlocks(COreClusters.ORE_CLUSTER_NONREPLACEABLE_BLOCKS);
    public HashSet<String> oreClusterReplaceableEmptyBlock = processReplaceableBlocks(COreClusters.ORE_CLUSTER_REPLACEABLE_EMPTY_BLOCK);
    public Boolean oreClusterDoesRegenerate = COreClusters.REGENERATE_ORE_CLUSTERS;
    public Map<String, Integer> oreClusterRegenPeriods = null;

    public String oreClusterConfigModelConstructionErrors;

    private static final COreClusters oreClusterDefaultConfigs = new COreClusters(); //Used for default values
    private static final Gson gson = new GsonBuilder().create();


    public OreClusterConfigModel(Block oreClusterBlock ) {
        this.oreClusterType = oreClusterBlock.getName().toString();
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
            this.subSeed = cOreClusters.subSeed.get();  //initialized to null

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
        this.oreClusterReplaceableBlocks = processReplaceableBlocks(cOreClusters.defaultOreClusterNonReplaceableBlocks.get());
        this.oreClusterReplaceableEmptyBlock = processReplaceableBlocks(cOreClusters.defaultOreClusterReplaceableEmptyBlock.get());
        this.oreClusterDoesRegenerate = cOreClusters.regenerateOreClusters.get();

        //Iterate through the oreClusterRegenPeriods and add them to the map


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
        return new HashSet<>(Arrays.asList(replaceableBlocks.split(",")));
    }

    public static Triple<Integer, Integer, Integer> processVolume(String ore, String volume)
    {
        /** Define Errors for validation **/
        StringBuilder volumeNotParsedCorrectlyError = new StringBuilder();
        volumeNotParsedCorrectlyError.append("Volume value: ");
        volumeNotParsedCorrectlyError.append(volume);
        volumeNotParsedCorrectlyError.append(" is not formatted correctly for ore: ");
        volumeNotParsedCorrectlyError.append(ore);
        volumeNotParsedCorrectlyError.append("Using default cluster volume of 32x32x32 instead");

        StringBuilder volumeNotWithinBoundsError = new StringBuilder();
        volumeNotWithinBoundsError.append("Volume value: ");
        volumeNotWithinBoundsError.append(volume);
        volumeNotWithinBoundsError.append(" is out of bounds for ore: ");
        volumeNotWithinBoundsError.append(ore);
        volumeNotWithinBoundsError.append("Using default cluster volume of 32x32x32 instead");

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
                    oreClusterRegenPeriods.put(item, Integer.parseInt(oreClusterRegenPeriodArray[i]));
                    i++;
                } else {
                    //If there is no corresponding length, use last number we got
                    oreClusterRegenPeriods.put(item, Integer.parseInt(oreClusterRegenPeriodArray[i]));
                }

            }
        }
        catch (NumberFormatException e) {
            //Reset map to default values given error
            StringBuilder error = new StringBuilder();
            error.append("Error parsing oreClusterRegenPeriods, use comma separated list of integers" +
             "default values have been set instead");
            LoggerBase.logWarning(error.toString());
            oreClusterRegenPeriods = null;
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

    public void setMinChunksBetweenOreClusters(Integer minChunksBetweenOreClusters) {
        Boolean validConfig = validateInteger(minChunksBetweenOreClusters, oreClusterDefaultConfigs.minChunksBetweenOreClusters,
        "for ore: " + this.oreClusterType);

        if( validConfig )
            this.minChunksBetweenOreClusters = minChunksBetweenOreClusters;
    }

    public void setMaxChunksBetweenOreClusters(Integer maxChunksBetweenOreClusters) {
        Boolean validConfig = validateInteger(maxChunksBetweenOreClusters, oreClusterDefaultConfigs.maxChunksBetweenOreClusters,
        "for ore: " + this.oreClusterType);

        if( validConfig )
            this.maxChunksBetweenOreClusters = maxChunksBetweenOreClusters;
    }

    public void setOreVeinModifier(Float oreVeinModifier) {
        Boolean validConfig = validateFloat(oreVeinModifier, oreClusterDefaultConfigs.defaultOreVeinModifier,
        "for ore: " + this.oreClusterType);

        if( validConfig )
            this.oreVeinModifier = oreVeinModifier;
    }

    public void setOreClusterReplaceableBlocks(String oreClusterReplaceableBlocks) {
        this.oreClusterReplaceableBlocks = processReplaceableBlocks(oreClusterReplaceableBlocks);
    }

    public void setOreClusterReplaceableEmptyBlock(String oreClusterReplaceableEmptyBlock) {
        this.oreClusterReplaceableEmptyBlock = processReplaceableBlocks(oreClusterReplaceableEmptyBlock);
    }

    public void setOreClusterDoesRegenerate(String oreClusterDoesRegenerate) {
        this.oreClusterDoesRegenerate = parseBoolean(oreClusterDoesRegenerate);
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
        jsonObject.addProperty("oreClusterReplaceableBlocks",
            oreClusterReplaceableBlocks.stream().collect(Collectors.joining(", ")));
        jsonObject.addProperty("oreClusterReplaceableEmptyBlock",
            oreClusterReplaceableEmptyBlock.stream().collect(Collectors.joining(", ")));
        jsonObject.addProperty("oreClusterDoesRegenerate", oreClusterDoesRegenerate);

        System.err.println("jsonObject: " + jsonObject);
        return gson.toJson(jsonObject).
                //replace("\",", "\"," + System.getProperty("line.separator") ).
                        replace('"', "'".toCharArray()[0]);
    }

    public void deserialize(String jsonString) {
        StringBuilder errorBuilder = new StringBuilder();
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

        try {
            oreClusterType = jsonObject.get("oreClusterType").getAsString();
            oreClusterType = processMinecraftBlockTypeSyntax(oreClusterType);
        } catch (Exception e) {
            errorBuilder.append("Error parsing oreClusterType: ").append(e.getMessage()).append("\n");
        }

        try {
            oreClusterSpawnRate = jsonObject.get("defaultOreClusterSpawnRate").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing defaultOreClusterSpawnRate: ").append(e.getMessage()).append("\n");
        }

        try {
            setOreClusterVolume(jsonObject.get("defaultOreClusterVolume").getAsString());

        } catch (Exception e) {
            errorBuilder.append("Error parsing defaultOreClusterVolume: ").append(e.getMessage()).append("\n");
        }

        try {
            oreClusterDensity = jsonObject.get("defaultOreClusterDensity").getAsFloat();
        } catch (Exception e) {
            errorBuilder.append("Error parsing defaultOreClusterDensity: ").append(e.getMessage()).append("\n");
        }

        try {
            oreClusterShape = jsonObject.get("defaultOreClusterShape").getAsString();
        } catch (Exception e) {
            errorBuilder.append("Error parsing defaultOreClusterShape: ").append(e.getMessage()).append("\n");
        }

        try {
            oreClusterMaxYLevelSpawn = jsonObject.get("oreClusterMaxYLevelSpawn").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing oreClusterMaxYLevelSpawn: ").append(e.getMessage()).append("\n");
        }

        try {
            minChunksBetweenOreClusters = jsonObject.get("minChunksBetweenOreClusters").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing minChunksBetweenOreClusters: ").append(e.getMessage()).append("\n");
        }

        try {
            maxChunksBetweenOreClusters = jsonObject.get("maxChunksBetweenOreClusters").getAsInt();
        } catch (Exception e) {
            errorBuilder.append("Error parsing maxChunksBetweenOreClusters: ").append(e.getMessage()).append("\n");
        }

        oreClusterConfigModelConstructionErrors = errorBuilder.toString();
    }



}

