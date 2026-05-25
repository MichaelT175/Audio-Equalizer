package com.audioeq;

public class OnlineTrack {
    private final String title;
    private final String artist;
    private final String album;
    private final String previewUrl;
    private final String artworkUrl;

    public OnlineTrack(String title, String artist, String album, String previewUrl, String artworkUrl) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.previewUrl = previewUrl;
        this.artworkUrl = artworkUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    @Override
    public String toString() {
        return title + " — " + artist;
    }
}
