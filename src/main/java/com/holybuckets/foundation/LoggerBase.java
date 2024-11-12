package com.holybuckets.foundation;

import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.PrintStream;


public class LoggerBase {


    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String PREFIX = "[" + OreClustersAndRegenMain.NAME + "]";
    public static final Boolean DEBUG_MODE = OreClustersAndRegenMain.DEBUG;

    /*
     *  1. We want to have static methods for logging info, warnings, and errors to server console.
     *  2. We want to have static methods for logging info, warnings, and errors to Client Chat.

     */

    private static String buildBaseConsoleMessage(String id, String prefix, String message) {
        return prefix + " " + "(" + id + "): " + message;
    }

    private static String buildBaseClientMessage(String prefix, String message) {
        return prefix + ":" + message;
    }

    private static String buildClientDisplayMessage(String prefix, String message) {
        return message;
    }

    public static void logInfo(String logId, String message) {
        LOGGER.info(buildBaseConsoleMessage(logId, PREFIX, message));
    }

    public static void logWarning(String logId, String string) {
        LOGGER.warn(buildBaseConsoleMessage(logId, PREFIX, string));
    }

    public static void logError(String logId, String string) {
        LOGGER.error(buildBaseConsoleMessage(logId, PREFIX, string));
    }

    public static void logDebug(String logId, String string) {
        if (DEBUG_MODE)
            LOGGER.info(buildBaseConsoleMessage(logId, PREFIX, string));
    }

    public static void logInit(String logId, String string) {
        logDebug(logId, "--------" + string.toUpperCase() + " INITIALIZED --------");
    }


    //Client side logging
    public static void logClientInfo(String message) {
        LOGGER.info(buildBaseClientMessage(PREFIX, message));
    }


    public static void logClientDisplay(String message) {
        String msg = buildClientDisplayMessage("", message);
    }

    /**
     * Returns time in milliseconds
     *
     * @param t1
     * @param t2
     */
    public static float getTime(long t1, long t2) {
        return (t2 - t1) / 1000_000L;
    }

    public static void threadExited(String logId, Object threadContainer, Throwable thrown) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread " + Thread.currentThread().getName() + " exited");

        if (thrown == null)
        {
            logDebug(logId, sb + " gracefully");
        } else
        {
            sb.append(" with exception: " + thrown.getMessage());

            //get the stack trace of the exception into a string to load into sb
            StackTraceElement[] stackTrace = thrown.getStackTrace();
            for (StackTraceElement ste : stackTrace) {
                sb.append(ste.toString() + "\n");
            }
            logError(logId, sb.toString());

        }
    }

}
//END CLASS