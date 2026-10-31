package dev.zadko.durabilityalarm;

import dev.zadko.durabilityalarm.core.DurabilityRules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Detached render snapshot. Never mutates an inventory stack. */
public record WatchedItem(ItemStack icon, Component name, Component location,
                          Object identity, int remaining, int maximum, int severity) {
    public double percent() { return DurabilityRules.percent(remaining, maximum); }
}
