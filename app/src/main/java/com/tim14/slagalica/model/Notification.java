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
    public String typeString;
    public long createdAt;
    public String userId;
    public String senderId;
    public String senderName;
    public String relatedMatchId;
    public String invitationStatus;
    public boolean friendlyMatch;
    public long expiresAt;

    public Notification() {}

    public Notification(String id, String title, String message, String timestamp, boolean read, Type type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
        this.type = type;
        this.typeString = type != null ? type.name() : Type.OTHER.name();
        this.createdAt = System.currentTimeMillis();
        this.senderId = "";
        this.senderName = "";
        this.relatedMatchId = "";
        this.invitationStatus = "PENDING";
        this.friendlyMatch = false;
        this.expiresAt = 0L;
    }

    public static Type typeFromString(String typeStr) {
        if (typeStr == null) return Type.OTHER;
        try {
            return Type.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Type.OTHER;
        }
    }

    public boolean isPendingInvite() {
        return type == Type.INVITE
                && "PENDING".equalsIgnoreCase(invitationStatus)
                && relatedMatchId != null
                && !relatedMatchId.trim().isEmpty();
    }
}
