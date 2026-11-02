package dev.zadko.durabilityalarm;

import dev.zadko.durabilityalarm.core.DurabilityRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryScanner {
    private record Identity(Item item, Map<DataComponentType<?>, Object> components) {}

    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final int HOTBAR_SIZE = 9;
    private static final List<EquipmentSlot> EQUIPMENT = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.OFFHAND);

    private InventoryScanner() {}

    public static List<WatchedItem> scan(LocalPlayer player, AlarmConfig config) {
        List<WatchedItem> result = new ArrayList<>();
        Inventory inventory = player.getInventory();
        if (config.allInventory) {
            // Scan only the 36 main slots here; equipment storage changed across 1.21.x.
            for (int slot = 0; slot < MAIN_INVENTORY_SIZE; slot++) {
                add(result, inventory.getItem(slot), location(player, slot), config);
            }
            add(result, player.containerMenu.getCarried(),
                    Component.translatable("durability_alarm.slot.cursor"), config);
        } else {
            add(result, player.getMainHandItem(),
                    Component.translatable("durability_alarm.slot.mainhand"), config);
        }
        // Query player equipment explicitly, once in either scan mode.
        for (EquipmentSlot slot : EQUIPMENT) {
            add(result, player.getItemBySlot(slot),
                    Component.translatable("durability_alarm.slot." + slot.getName()), config);
        }
        result.sort(Comparator.comparingInt(WatchedItem::severity).reversed()
                .thenComparingDouble(WatchedItem::percent).thenComparingInt(WatchedItem::remaining));
        return List.copyOf(result);
    }

    private static Component location(LocalPlayer player, int index) {
        // Both accessors return the live stack; no version-specific selected-slot API is needed.
        if (index < HOTBAR_SIZE && player.getInventory().getItem(index) == player.getMainHandItem()) {
            return Component.translatable("durability_alarm.slot.mainhand");
        }
        if (index < HOTBAR_SIZE) return Component.translatable("durability_alarm.slot.hotbar", index + 1);
        return Component.translatable("durability_alarm.slot.inventory", index + 1);
    }

    private static void add(List<WatchedItem> result, ItemStack stack, Component location, AlarmConfig config) {
        // Vanilla's predicate also excludes the UNBREAKABLE component.
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) return;
        int maximum = stack.getMaxDamage();
        int remaining = DurabilityRules.remaining(maximum, stack.getDamageValue());
        int severity = DurabilityRules.severity(remaining, maximum, config.warningPercent, config.criticalPercent);
        if (severity == DurabilityRules.SAFE) return;
        Map<DataComponentType<?>, Object> components = new HashMap<>();
        for (var component : stack.getComponents()) {
            if (component.type() != DataComponents.DAMAGE) components.put(component.type(), component.value());
        }
        result.add(new WatchedItem(stack.copy(), stack.getHoverName().copy(), location,
                new Identity(stack.getItem(), Map.copyOf(components)), remaining, maximum, severity));
    }
}
