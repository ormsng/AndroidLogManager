package com.example.logviewer;

import java.io.Serializable;

public class LogEntry implements Serializable {
    private LogType type;
    private String tag;
    private String message;
    private String timestamp;

    public LogEntry(String message) {
        this.message = message;
    }

    public LogEntry(LogType type, String tag, String message, String timestamp) {
        this.type = type;
        this.tag = tag;
        this.message = message;
        this.timestamp = timestamp;
    }

    public LogType getType() {
        return type;
    }

    public String getTag() {
        return tag;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
