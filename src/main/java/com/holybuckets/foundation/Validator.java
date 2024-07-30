package com.holybuckets.foundation;

/*
    Class chiefly used to validate the presence and properties of blocks and items in the game.

 */

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class Validator {

    public static boolean blockExists(String blockName) {
        return ForgeRegistries.BLOCKS.containsKey( new ResourceLocation(blockName) );
    }

    public static boolean blockHasProperty(String blockName, String propertyname) {
        return true;
    }

    public static boolean itemExists(String itemname) {
        return ForgeRegistries.ITEMS.containsKey( new ResourceLocation(itemname) );
    }
}
