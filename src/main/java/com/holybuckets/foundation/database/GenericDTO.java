package com.holybuckets.foundation.database;

public class GenericDTO {
    private String id;
    private String timeEntered;
    private String timeLastModified;
    private String data;

    public GenericDTO(String id, String timeEntered, String timeLastModified, String data) {
        this.id = id;
        this.timeEntered = timeEntered;
        this.timeLastModified = timeLastModified;
        this.data = data;
    }

    // Getters and setters for each field
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTimeEntered() { return timeEntered; }
    public void setTimeEntered(String timeEntered) { this.timeEntered = timeEntered; }

    public String getTimeLastModified() { return timeLastModified; }
    public void setTimeLastModified(String timeLastModified) { this.timeLastModified = timeLastModified; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
}
