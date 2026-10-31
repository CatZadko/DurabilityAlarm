package dev.zadko.durabilityalarm.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Track counts by item identity excluding damage and inventory position.
 * Moving identical stacks cannot create an alert; an additional worn stack can.
 * No server-side item UUID or mutation of the player's stacks is required.
 */
public final class AlertTracker {
    public record Observation(Object identity, int remaining, int severity) {}

    private static final class Group {
        int low, critical, last, minimum = Integer.MAX_VALUE;
        void add(Observation observation) {
            low++;
            if (observation.severity() >= DurabilityRules.CRITICAL) critical++;
            if (observation.severity() >= DurabilityRules.LAST_POINT) last++;
            minimum = Math.min(minimum, observation.remaining());
        }
    }

    private Map<Object, Group> previous = Map.of();
    private int pending;
    private long lastSignal = -1;
    private int lastSignalSeverity;
    private boolean damagedSinceSignal;

    public int update(List<Observation> observations, long tick, int repeatSeconds) {
        Map<Object, Group> current = new HashMap<>();
        int highest = 0;
        for (Observation observation : observations) {
            if (observation.severity() <= DurabilityRules.SAFE) continue;
            current.computeIfAbsent(observation.identity(), key -> new Group()).add(observation);
            highest = Math.max(highest, observation.severity());
        }

        for (var entry : current.entrySet()) {
            Group now = entry.getValue();
            Group before = previous.get(entry.getKey());
            if (before == null) before = new Group();
            if (now.low > before.low) pending = Math.max(pending, DurabilityRules.LOW);
            if (now.critical > before.critical) pending = Math.max(pending, DurabilityRules.CRITICAL);
            if (now.last > before.last) pending = DurabilityRules.LAST_POINT;
            if (before.minimum != Integer.MAX_VALUE && now.minimum < before.minimum) {
                damagedSinceSignal = true;
            }
        }
        previous = current;
        if (current.isEmpty()) {
            pending = 0;
            damagedSinceSignal = false;
            return 0;
        }

        // Never deliver a queued critical alert after that item was repaired/removed.
        pending = Math.min(pending, highest);
        boolean reminder = repeatSeconds > 0 && lastSignal >= 0 && damagedSinceSignal
                && tick - lastSignal >= repeatSeconds * 20L;
        if (reminder) pending = Math.max(pending, highest);

        boolean cooldownPassed = lastSignal < 0 || tick - lastSignal >= 40;
        // A worsening critical/last-point condition must not wait for the cooldown.
        boolean urgent = pending >= DurabilityRules.CRITICAL && pending > lastSignalSeverity;
        if (pending > 0 && (cooldownPassed || urgent)) {
            int result = pending;
            pending = 0;
            lastSignal = tick;
            lastSignalSeverity = result;
            damagedSinceSignal = false;
            return result;
        }
        return 0;
    }

    public void reset() {
        previous = Map.of();
        pending = 0;
        lastSignal = -1;
        lastSignalSeverity = 0;
        damagedSinceSignal = false;
    }
}
