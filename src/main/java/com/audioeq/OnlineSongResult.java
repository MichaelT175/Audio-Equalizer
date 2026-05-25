package com.audioeq;

public class OnlineSongResult {
    private String title;
    private String artist;
    private String videoId;
    private String thumbnailUrl;
    private String duration;

    public String getTitle() {
        return title != null ? title : "Unknown Title";
    }

    public String getArtist() {
        return artist != null ? artist : "";
    }

    public String getVideoId() {
        return videoId;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getDuration() {
        return duration != null ? duration : "";
    }

    public String getDisplayName() {
        if (getArtist().isEmpty()) {
            return getTitle();
        }
        return getTitle() + " - " + getArtist();
    }
}
