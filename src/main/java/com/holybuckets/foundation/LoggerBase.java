package com.holybuckets.foundation;

import com.holybuckets.orecluster.OreClustersAndRegenMain;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.PrintStream;
import java.util.function.Predicate;
import java.util.ArrayList;
import java.util.List;


public class LoggerBase {
    // Store log entries for filtering
    private static List<LogEntry> logHistory = new ArrayList<>();
    private static final int MAX_LOG_HISTORY = 1000; // Limit the history size

    // Sampling configuration
    private static final float SAMPLE_RATE = 0.1f; // Sample 10% of messages by default
    private static String FILTER_TYPE = null; // Only log messages of this type if set
    private static String FILTER_ID = null; // Only log messages with this ID if set
    private static String FILTER_PREFIX = null; // Only log messages with this prefix if set
    private static String FILTER_CONTENT = null; // Only log messages containing this content if set
    private static boolean SAMPLING_ENABLED = false;

    // Log entry class to store log information
    protected static class LogEntry {
        String type;
        String id;
        String prefix;
        String message;
        long timestamp;

        LogEntry(String type, String id, String prefix, String message) {
            this.type = type;
            this.id = id;
            this.prefix = prefix;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static void addToHistory(LogEntry entry) {
        logHistory.add(entry);
        if (logHistory.size() > MAX_LOG_HISTORY) {
            logHistory.remove(0);
        }
    }


    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String PREFIX = "[" + HolyBucketsUtility.NAME + "]";
    public static final Boolean DEBUG_MODE = true;

    /*
     *  1. We want to have static methods for logging info, warnings, and errors to server console.
     *  2. We want to have static methods for logging info, warnings, and errors to Client Chat.

     */

    protected static String buildBaseConsoleMessage(String id, String prefix, String message) {
        return prefix + " " + "(" + id + "): " + message;
    }

    protected static String buildBaseConsoleMessage(LogEntry entry) {
        return entry.prefix + " " + "(" + entry.id + "): " + entry.message;
    }

    public static void setSamplingEnabled(boolean enabled) {
        SAMPLING_ENABLED = enabled;
    }

    public static void setTypeFilter(String type) {
        FILTER_TYPE = type;
    }

    public static void setIdFilter(String id) {
        FILTER_ID = id;
    }

    public static void setPrefixFilter(String prefix) {
        FILTER_PREFIX = prefix;
    }

    public static void setContentFilter(String content) {
        FILTER_CONTENT = content;
    }

    private static boolean shouldSampleLog(LogEntry entry) {
        if (!SAMPLING_ENABLED) {
            return true;
        }

        // Check if entry matches any active filters
        if (FILTER_TYPE != null && !entry.type.equals(FILTER_TYPE)) {
            return false;
        }
        if (FILTER_ID != null && !entry.id.equals(FILTER_ID)) {
            return false;
        }
        if (FILTER_PREFIX != null && !entry.prefix.equals(FILTER_PREFIX)) {
            return false;
        }
        if (FILTER_CONTENT != null && !entry.message.contains(FILTER_CONTENT)) {
            return false;
        }

        // Apply sampling rate
        return Math.random() < SAMPLE_RATE;
    }

    protected static String buildBaseClientMessage(String prefix, String message)
    {
        return prefix + ":" + message;
    }

    protected static String buildClientDisplayMessage(String prefix, String message) {
        return message;
    }

    public static void logInfo(String logId, String message) {
        LogEntry entry = new LogEntry("INFO", logId, PREFIX, message);
        if (shouldSampleLog(entry)) {
            addToHistory(entry);
            LOGGER.info(buildBaseConsoleMessage(entry));
        }
    }

    public static void logWarning(String logId, String string) {
        LogEntry entry = new LogEntry("WARN", logId, PREFIX, string);
        if (shouldSampleLog(entry)) {
            addToHistory(entry);
            LOGGER.warn(buildBaseConsoleMessage(entry));
        }
    }

    public static void logError(String logId, String string) {
        LogEntry entry = new LogEntry("ERROR", logId, PREFIX, string);
        if (shouldSampleLog(entry)) {
            addToHistory(entry);
            LOGGER.error(buildBaseConsoleMessage(entry));
        }
    }

    public static void logDebug(String logId, String string) {
        if (DEBUG_MODE) {
            LogEntry entry = new LogEntry("DEBUG", logId, PREFIX, string);
            if (shouldSampleLog(entry)) {
                addToHistory(entry);
                LOGGER.info(buildBaseConsoleMessage(entry));
            }
        }
    }

    // Filtering methods
    public static List<LogEntry> filterByType(String type) {
        return filterLogs(entry -> entry.type.equals(type));
    }

    public static List<LogEntry> filterById(String id) {
        return filterLogs(entry -> entry.id.equals(id));
    }

    public static List<LogEntry> filterByPrefix(String prefix) {
        return filterLogs(entry -> entry.prefix.equals(prefix));
    }

    public static List<LogEntry> filterByMessageContent(String content) {
        return filterLogs(entry -> entry.message.contains(content));
    }

    public static List<LogEntry> filterByTimeRange(long startTime, long endTime) {
        return filterLogs(entry -> entry.timestamp >= startTime && entry.timestamp <= endTime);
    }

    private static List<LogEntry> filterLogs(Predicate<LogEntry> predicate) {
        List<LogEntry> filtered = new ArrayList<>();
        for (LogEntry entry : logHistory) {
            if (predicate.test(entry)) {
                filtered.add(entry);
            }
        }
        return filtered;
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
                sb.append("\n" + ste.toString() );
            }
            sb.append("\n\n");

            logError(logId, sb.toString());

        }
    }

}
//END CLASS
