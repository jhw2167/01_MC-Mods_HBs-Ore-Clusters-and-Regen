package com.holybuckets.foundation;

import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.holybuckets.orecluster.config.model.OreClusterConfigModel;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class LoggerBase {


    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String PREFIX = "[" + OreClustersAndRegenMain.NAME + "]";
    public static final Boolean DEBUG = false;

    /*
     *  1. We want to have static methods for logging info, warnings, and errors to server console.
     *  2. We want to have static methods for logging info, warnings, and errors to Client Chat.

     */

     private static String buildBaseConsoleMessage( String prefix, String message) {
            return prefix + ":" + message;
     }

    private static String buildBaseClientMessage( String prefix, String message) {
            return prefix + ":" + message;
    }
    private static String buildClientDisplayMessage(String prefix, String message) {
        return message;
    }

    public static void logInfo(String message) {
        LOGGER.info(buildBaseConsoleMessage(PREFIX, message));
    }

    public static void logWarning(String string) {
        LOGGER.warn(buildBaseConsoleMessage(PREFIX, string));
    }

    public static void logError(String string) {
        LOGGER.error(buildBaseConsoleMessage(PREFIX, string));
    }

    public static void logDebug(String string) {
        if( DEBUG )
            LOGGER.info( buildBaseConsoleMessage(PREFIX, string));
    }

    public static void logInit(String string) {
        logDebug("--------" + string.toUpperCase() + " INITIALIZED --------");
    }


    //Client side logging
    public static void logClientInfo(String message) {
        LOGGER.info(buildBaseClientMessage(PREFIX, message));
    }


    public static void logClientDisplay(String message) {
        String msg = buildClientDisplayMessage("", message);
    }

}
