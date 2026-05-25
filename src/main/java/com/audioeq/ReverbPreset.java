package com.audioeq;

public enum ReverbPreset {
    STUDIO("Studio", 0.4, 0.2, 0.22),
    SMALL_ROOM("Small Room", 0.5, 0.25, 0.25),
    CONCERT_HALL("Concert Hall", 0.75, 0.35, 0.35),
    CATHEDRAL("Cathedral", 0.85, 0.45, 0.42),
    UNDERGROUND_TUNNEL("Underground Tunnel", 0.7, 0.55, 0.38),
    AMBIENT_SPACE("Ambient Space", 0.6, 0.25, 0.3);

    final String displayName;
    final double roomSize;
    final double damping;
    final double baseMix;

    ReverbPreset(String displayName, double roomSize, double damping, double baseMix) {
        this.displayName = displayName;
        this.roomSize = roomSize;
        this.damping = damping;
        this.baseMix = baseMix;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
