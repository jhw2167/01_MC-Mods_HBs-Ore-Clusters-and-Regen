package com.holybuckets.foundation.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseQueryExecutor {

    private final Connection connection;
    private final String tableName;

    public DatabaseQueryExecutor(Connection connection, String tableName) {
        this.connection = connection;
        this.tableName = tableName;
    }

    public List<GenericDTO> getAll() throws SQLException {
        String query = "SELECT * FROM " + tableName;
        List<GenericDTO> results = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                results.add(new GenericDTO(
                    rs.getString("id"),
                    rs.getString("timeEntered"),
                    rs.getString("timeLastModified"),
                    rs.getString("data")
                ));
            }
        }
        return results;
    }

    public List<String> getAllData() throws SQLException {
        String query = "SELECT data FROM " + tableName;
        List<String> results = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                results.add(rs.getString("data"));
            }
        }
        return results;
    }

    public GenericDTO getById(String id) throws SQLException {
        String query = "SELECT * FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new GenericDTO(
                    rs.getString("id"),
                    rs.getString("timeEntered"),
                    rs.getString("timeLastModified"),
                    rs.getString("data")
                );
            }
        }
        return null;
    }

    public String getDataById(String id) throws SQLException {
        String query = "SELECT data FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("data");
            }
        }
        return null;
    }
}
