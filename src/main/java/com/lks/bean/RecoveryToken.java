package com.lks.bean;

import java.sql.Timestamp;

public class RecoveryToken {

    private int id;
    private int userId;
    private String tokenHash;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private int failedAttempts;
    private boolean isUsed;

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }

    // Optional: Override toString for better logging
    @Override
    public String toString() {
        return "RecoveryToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", tokenHash='" + tokenHash + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", failedAttempts=" + failedAttempts +
                ", isUsed=" + isUsed +
                '}';
    }
}
