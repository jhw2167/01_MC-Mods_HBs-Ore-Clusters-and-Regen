package com.holybuckets.foundation.database;

import com.holybuckets.foundation.LoggerBase;

import java.sql.SQLException;

public class DatabaseManager
{
    private static DatabaseManager instance;
    public static final String CLASS_ID = "000";

    private DatabaseManager() { }

    /**
     * DatabaseManger is a singleton, get the instance using this method. API users should rarely need this method.
     * @return
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public synchronized void startDatabase(String levelName) throws SQLException
    {
        try {
            DatabaseAccessor.initiateInstance(levelName);

        } catch (SQLException e) {
            StringBuilder sb = new StringBuilder( "Error starting database, this is considered a critical error and the game will crash, SQL error message: ");
            sb.append( e.getMessage() );
            LoggerBase.logError("000000", sb.toString() );

            throw e;
        }
    }

    /**
     * Database connection should be closed when the world is stopped.
     * @throws SQLException
     */
    public synchronized void closeDatabase() throws SQLException
    {
        DatabaseAccessor accessor = DatabaseAccessor.getLevelDatabaseInstance();
        if (accessor != null) {
            accessor.close();
        }
    }


}
//END CLASS
