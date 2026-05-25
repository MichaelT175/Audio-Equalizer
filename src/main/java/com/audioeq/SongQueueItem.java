package com.audioeq;

public class SongQueueItem {
    private final boolean online;
    private final String displayName;
    private final String filePath;
    private final OnlineSongResult onlineSource;

    private SongQueueItem(boolean online, String displayName, String filePath, OnlineSongResult onlineSource) {
        this.online = online;
        this.displayName = displayName;
        this.filePath = filePath;
        this.onlineSource = onlineSource;
    }

    public static SongQueueItem forOnline(OnlineSongResult result) {
        return new SongQueueItem(true, result.getDisplayName(), null, result);
    }

    public static SongQueueItem forLocal(String name, String filePath) {
        return new SongQueueItem(false, name, filePath, null);
    }

    public boolean isOnline() {
        return online;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFilePath() {
        return filePath;
    }

    public OnlineSongResult getOnlineSource() {
        return onlineSource;
    }
}
