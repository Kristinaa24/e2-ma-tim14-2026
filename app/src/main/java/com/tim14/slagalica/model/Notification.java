package com.tim14.slagalica.model;

public class Notification {
    public enum Type {
        CHAT, RANKING, REWARD, INVITE, OTHER
    }

    public String id;
    public String title;
    public String message;
    public String timestamp;
    public boolean read;
    public Type type;

    public Notification() {}

    public Notification(String id, String title, String message, String timestamp, boolean read, Type type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
        this.type = type;
    }
}
