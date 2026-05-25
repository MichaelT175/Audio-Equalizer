package com.audioeq;

public enum MoodPreset {
    NIGHT_DRIVE("Night Drive", Theme.MIDNIGHT_BLUE, 4.0f, -1.0f, 3.0f, ReverbPreset.STUDIO, 0.18f, 1.2f),
    CYBERPUNK("Cyberpunk", Theme.CYBER_BLUE, 5.0f, 2.0f, 4.0f, ReverbPreset.AMBIENT_SPACE, 0.28f, 1.3f),
    RETRO_ANALOG("Retro Analog", Theme.RETRO_AMBER, 2.0f, 1.5f, -1.0f, ReverbPreset.SMALL_ROOM, 0.2f, 0.9f),
    DEEP_FOCUS("Deep Focus", Theme.DEEP_INDIGO, -1.0f, 2.0f, 1.0f, ReverbPreset.STUDIO, 0.12f, 1.0f),
    CONCERT_MODE("Concert Mode", Theme.CONCERT_RED, 6.0f, 1.5f, 3.0f, ReverbPreset.CONCERT_HALL, 0.38f, 1.1f),
    NEON_PULSE("Neon Pulse", Theme.NEON_MAGENTA, 4.5f, 0.5f, 4.5f, ReverbPreset.AMBIENT_SPACE, 0.3f, 1.4f),
    SPACE_DRIFT("Space Drift", Theme.SPACE_TEAL, 1.5f, -0.5f, 3.5f, ReverbPreset.CATHEDRAL, 0.4f, 1.3f),
    OCEAN_WAVES("Ocean Waves", Theme.OCEAN_BLUE, 2.5f, -1.5f, 1.0f, ReverbPreset.UNDERGROUND_TUNNEL, 0.32f, 1.15f),
    BASS_BOOST("Bass Boost", Theme.BASS_STORM, 7.5f, 0.5f, 1.5f, ReverbPreset.CONCERT_HALL, 0.22f, 1.05f),
    CHILL_LOUNGE("Chill Lounge", Theme.CHILL_PLUM, 2.0f, 0.5f, 2.0f, ReverbPreset.SMALL_ROOM, 0.24f, 1.1f);

    final String displayName;
    final Theme theme;
    final float bassDb;
    final float midDb;
    final float trebleDb;
    final ReverbPreset reverbPreset;
    final float reverbIntensity;
    final float stereoWidth;

    MoodPreset(String displayName, Theme theme, float bassDb, float midDb, float trebleDb,
               ReverbPreset reverbPreset, float reverbIntensity, float stereoWidth) {
        this.displayName = displayName;
        this.theme = theme;
        this.bassDb = bassDb;
        this.midDb = midDb;
        this.trebleDb = trebleDb;
        this.reverbPreset = reverbPreset;
        this.reverbIntensity = reverbIntensity;
        this.stereoWidth = stereoWidth;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
