package com.mobdistancescaling.safezone;

public enum SafeZoneQuadrant {
    NORTH(0, 90, "Nord"),
    EAST(90, 180, "Est"),
    SOUTH(180, 270, "Sud"),
    WEST(270, 360, "Ouest");

    private final int startAngle;
    private final int endAngle;
    private final String displayName;

    SafeZoneQuadrant(int startAngle, int endAngle, String displayName) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.displayName = displayName;
    }

    public int getStartAngle() {
        return startAngle;
    }

    public int getEndAngle() {
        return endAngle;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean containsAngle(double angle) {
        return angle >= startAngle && angle < endAngle;
    }

    public SafeZoneQuadrant next() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    public static SafeZoneQuadrant fromAngle(double angle) {
        double normalizedAngle = ((angle % 360) + 360) % 360;
        
        for (SafeZoneQuadrant quadrant : values()) {
            if (quadrant.containsAngle(normalizedAngle)) {
                return quadrant;
            }
        }
        
        return NORTH;
    }
}
