package com.holybuckets.foundation.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DatabaseAccessor {
    private static DatabaseAccessor instance;
    private final Connection connection;
    private static final List<String> TABLE_NAMES = List.of("managed_chunks");

    private DatabaseAccessor(String levelName) throws SQLException {
        String url = "jdbc:sqlite:" + levelName + ".db";
        this.connection = DriverManager.getConnection(url);

        startDatabase(levelName);
    }

    /**
     *  Public API users should use this method to get the instance of the DatabaseAccessor, only after the world is
     *  started, else it will return null.
     * @return DatabaseAccessor instance
     *
     * @throws SQLException
     */
    public static synchronized DatabaseAccessor getLevelDatabaseInstance() throws SQLException {
        return instance;
    }

    public static synchronized DatabaseAccessor initiateInstance(String levelName) throws SQLException {
        if (instance == null) {
            instance = new DatabaseAccessor(levelName);
        }
        return instance;
    }

    /**
     * Returns a QueryExecturor for the given table. See
     * @param tableName
     * @return
     */
    public synchronized DatabaseQueryExecutor getQueryExecutor(String tableName) {
        return new DatabaseQueryExecutor(connection, tableName);
    }

    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        instance = null;
    }

    private void startDatabase(String levelName) throws SQLException
    {
        // Create tables if they do not exist
        for (String tableName : TABLE_NAMES) {
            createTableIfNotExists(tableName);
        }
    }

    private void createTableIfNotExists(String tableName) throws SQLException
    {
        // Use reflection to get column names from GenericDTO
        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

        Field[] fields = GenericDTO.class.getDeclaredFields();
        for (Field field : fields) {
            String columnName = field.getName();
            String columnType = "TEXT";  // Assuming all fields are stored as TEXT for simplicity
            createTableSQL.append(columnName).append(" ").append(columnType).append(",");
        }

        // Remove trailing comma and close the SQL statement
        createTableSQL.setLength(createTableSQL.length() - 1);
        createTableSQL.append(")");

        // Execute the create table statement
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL.toString());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}