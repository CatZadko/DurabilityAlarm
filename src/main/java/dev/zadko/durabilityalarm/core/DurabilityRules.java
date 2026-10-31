package dev.zadko.durabilityalarm.core;

/** Pure arithmetic: no Minecraft classes, no rendering, no approximation of time. */
public final class DurabilityRules {
    public static final int SAFE = 0;
    public static final int LOW = 1;
    public static final int CRITICAL = 2;
    public static final int LAST_POINT = 3;

    private DurabilityRules() {}

    public static int remaining(int maximum, int damage) {
        if (maximum <= 0) return 0;
        return (int) Math.max(0L, Math.min((long) maximum, (long) maximum - damage));
    }

    public static int severity(int remaining, int maximum, int warningPercent, int criticalPercent) {
        if (maximum <= 0) return SAFE;
        if (remaining <= 1) return LAST_POINT;
        // Use long multiplication so custom high-durability items cannot overflow.
        if ((long) remaining * 100 <= (long) maximum * criticalPercent) return CRITICAL;
        if ((long) remaining * 100 <= (long) maximum * warningPercent) return LOW;
        return SAFE;
    }

    public static double percent(int remaining, int maximum) {
        return maximum <= 0 ? 0.0 : Math.max(0.0, Math.min(100.0, 100.0 * remaining / maximum));
    }
}
