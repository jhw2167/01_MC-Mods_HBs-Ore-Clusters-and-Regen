package com.holybuckets.foundation.database;
import com.holybuckets.foundation.LoggerBase;
import java.sql.SQLException;
import org.sqlite.JDBC;
import org.sqlite.SQLiteJDBCLoader;

public class DatabaseManager
{
    private static DatabaseManager instance;
    public static final String CLASS_ID = "001";

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

            boolean sqliteLoaded = SQLiteJDBCLoader.initialize();
            if (!sqliteLoaded)
                throw new RuntimeException("Failed to load SQLite native library. Hopefully SQLite logged a reason for this failure.");

            Class.forName("org.sqlite.JDBC");
            DatabaseAccessor.initiateInstance(levelName);
        }
        catch (ClassNotFoundException e) {
            StringBuilder sb = new StringBuilder( "Class not found exception for JDBC driver" );
            sb.append( e.getMessage() );
            LoggerBase.logError("001000", sb.toString() );

        }
         catch (SQLException e) {
            StringBuilder sb = new StringBuilder( "Error starting database, this is considered a critical error and the game will crash, SQL error message: ");
            sb.append( e.getMessage() );
            LoggerBase.logError("001001", sb.toString() );

            throw e;
        }
        catch (Throwable e) {
            StringBuilder sb = new StringBuilder( "Critical programmer error: One or more libraries aren't present. Error: [" );
            sb.append( e.getMessage() );
            sb.append( "].");
            LoggerBase.logError("001002", sb.toString() );

            throw new RuntimeException(e);
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
