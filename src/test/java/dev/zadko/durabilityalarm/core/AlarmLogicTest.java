package dev.zadko.durabilityalarm.core;

import java.util.List;
import static dev.zadko.durabilityalarm.core.DurabilityRules.*;

/** Standalone behavior checks. No game, third-party test framework or stubs needed. */
public final class AlarmLogicTest {
    private static int assertions;

    public static void main(String[] args) {
        thresholds();
        initialWarningsAndMovement();
        escalationAndRepair();
        duplicateItems();
        reminders();
        cooldownAndSessionReset();
        System.out.println("Durability Alarm: " + assertions + " behavior checks passed.");
    }

    private static void thresholds() {
        equal(SAFE, severity(0, 0, 10, 3), "Items without durability are ignored");
        equal(SAFE, severity(101, 1000, 10, 3), "Above warning boundary");
        equal(LOW, severity(100, 1000, 10, 3), "Exact warning boundary");
        equal(LOW, severity(31, 1000, 10, 3), "Above critical boundary");
        equal(CRITICAL, severity(30, 1000, 10, 3), "Exact critical boundary");
        equal(LAST_POINT, severity(1, 10, 10, 3), "Final point on a weak item");
        equal(LAST_POINT, severity(0, 432, 10, 3), "Exhausted elytra");
        equal(SAFE, severity(157, 1561, 10, 3), "Do not round a diamond pickaxe down prematurely");
        equal(LOW, severity(156, 1561, 10, 3), "Diamond pickaxe threshold");
        equal(CRITICAL, severity(46, 1561, 10, 3), "Diamond pickaxe critical threshold");
        equal(SAFE, severity(Integer.MAX_VALUE, Integer.MAX_VALUE, 10, 3), "No overflow on modded maximum");
        equal(0, remaining(100, Integer.MAX_VALUE), "Clamp excessive damage");
        equal(100, remaining(100, Integer.MIN_VALUE), "Clamp negative damage without overflow");
        equal(5, remaining(100, 95), "Ordinary remaining durability");
    }

    private static void initialWarningsAndMovement() {
        AlertTracker tracker = new AlertTracker();
        var a = sample("pickaxe", 90, LOW);
        var b = sample("helmet", 9, CRITICAL);
        equal(CRITICAL, tracker.update(List.of(a, b), 5, 30), "Combine simultaneous alerts into one sound");
        equal(0, tracker.update(List.of(b, a), 10, 30), "Reordering slots does not beep");
        equal(0, tracker.update(List.of(a, b), 2000, 30), "Idle damaged equipment does not repeat");
        equal(0, tracker.update(List.of(), 2005, 30), "Removing all warnings clears state");
    }

    private static void escalationAndRepair() {
        AlertTracker tracker = new AlertTracker();
        equal(LOW, tracker.update(List.of(sample("a", 10, LOW)), 0, 30), "First warning");
        equal(CRITICAL, tracker.update(List.of(sample("a", 3, CRITICAL)), 5, 30), "Critical escalation bypasses cooldown");
        equal(LAST_POINT, tracker.update(List.of(sample("a", 1, LAST_POINT)), 10, 30), "Final point bypasses cooldown");
        equal(0, tracker.update(List.of(sample("a", 8, LOW)), 60, 30), "Repair does not alert");
        equal(CRITICAL, tracker.update(List.of(sample("a", 3, CRITICAL)), 100, 30), "Repair rearms the critical threshold");
        equal(0, tracker.update(List.of(), 150, 30), "Full repair clears alert");
        equal(LOW, tracker.update(List.of(sample("a", 10, LOW)), 200, 30), "New wear after repair alerts again");
    }

    private static void duplicateItems() {
        AlertTracker tracker = new AlertTracker();
        var a = sample("same-pickaxe", 80, LOW);
        var b = sample("same-pickaxe", 70, LOW);
        equal(LOW, tracker.update(List.of(a, b), 0, 30), "Two identical damaged items");
        equal(0, tracker.update(List.of(b, a), 50, 30), "Swapping identical items does not beep");
        equal(LOW, tracker.update(List.of(a, b, sample("same-pickaxe", 75, LOW)), 100, 30),
                "An additional identical damaged item is detected");
        equal(CRITICAL, tracker.update(List.of(a, sample("same-pickaxe", 20, CRITICAL)), 150, 30),
                "Critical duplicate is detected even as total count decreases");
        equal(0, tracker.update(List.of(sample("same-pickaxe", 20, CRITICAL), a), 155, 30),
                "Moving the critical duplicate remains silent");
    }

    private static void reminders() {
        AlertTracker tracker = new AlertTracker();
        equal(LOW, tracker.update(List.of(sample("a", 100, LOW)), 0, 30), "Reminder setup");
        equal(0, tracker.update(List.of(sample("a", 99, LOW)), 100, 30), "Do not remind too early");
        equal(LOW, tracker.update(List.of(sample("a", 99, LOW)), 600, 30), "Remind after additional wear");
        equal(0, tracker.update(List.of(sample("a", 99, LOW)), 1200, 30), "No endless repeats after use stops");
        equal(0, tracker.update(List.of(sample("a", 98, LOW)), 1800, 0), "Reminder disabled");
        equal(CRITICAL, tracker.update(List.of(sample("a", 30, CRITICAL)), 1805, 0), "New critical alerts still work");
    }

    private static void cooldownAndSessionReset() {
        AlertTracker tracker = new AlertTracker();
        var a = sample("a", 10, LOW);
        var b = sample("b", 10, LOW);
        equal(LOW, tracker.update(List.of(a), 0, 30), "Cooldown setup");
        equal(0, tracker.update(List.of(a, b), 5, 30), "Throttle close low alerts");
        equal(LOW, tracker.update(List.of(a, b), 40, 30), "Queued low alert is not lost");
        equal(0, tracker.update(List.of(a, b, sample("c", 10, LOW)), 45, 30), "Queue another alert");
        equal(0, tracker.update(List.of(), 50, 30), "Drop queued alert when nothing is damaged");
        tracker.reset();
        equal(LOW, tracker.update(List.of(a), 51, 30), "Reconnect starts fresh");
    }

    private static AlertTracker.Observation sample(Object key, int remaining, int severity) {
        return new AlertTracker.Observation(key, remaining, severity);
    }

    private static void equal(int expected, int actual, String scenario) {
        assertions++;
        if (expected != actual) throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
    }
}
