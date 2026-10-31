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

    private InventoryScanner() {}

    public static List<WatchedItem> scan(LocalPlayer player, AlarmConfig config) {
        List<WatchedItem> result = new ArrayList<>();
        Inventory inventory = player.getInventory();
        if (config.allInventory) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                add(result, inventory.getItem(slot), location(inventory, slot), config);
            }
            // A tool being dragged in an inventory GUI remains monitored.
            add(result, player.containerMenu.getCarried(),
                    Component.translatable("durability_alarm.slot.cursor"), config);
        } else {
            add(result, inventory.getSelectedItem(),
                    Component.translatable("durability_alarm.slot.mainhand"), config);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot == EquipmentSlot.MAINHAND) continue;
                add(result, player.getItemBySlot(slot),
                        Component.translatable("durability_alarm.slot." + slot.getName()), config);
            }
        }
        result.sort(Comparator.comparingInt(WatchedItem::severity).reversed()
                .thenComparingDouble(WatchedItem::percent).thenComparingInt(WatchedItem::remaining));
        return List.copyOf(result);
    }

    private static Component location(Inventory inventory, int index) {
        if (index == inventory.getSelectedSlot()) return Component.translatable("durability_alarm.slot.mainhand");
        if (index < Inventory.getSelectionSize()) return Component.translatable("durability_alarm.slot.hotbar", index + 1);
        if (index < Inventory.INVENTORY_SIZE) return Component.translatable("durability_alarm.slot.inventory", index + 1);
        EquipmentSlot slot = Inventory.EQUIPMENT_SLOT_MAPPING.get(index);
        return Component.translatable("durability_alarm.slot." + (slot == null ? "equipment" : slot.getName()));
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
