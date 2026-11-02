package dev.zadko.durabilityalarm;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zadko.durabilityalarm.core.AlertTracker;
import dev.zadko.durabilityalarm.core.DurabilityRules;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DurabilityAlarmClient implements ClientModInitializer {
    private final AlertTracker tracker = new AlertTracker();
    private final AlarmHud hud = new AlarmHud(this);
    private AlarmConfig config;
    private KeyMapping settingsKey;
    private LocalPlayer trackedPlayer;
    private List<WatchedItem> warnings = List.of();
    private List<WatchedItem> demo = List.of();
    private long tick;
    private long demoUntil;
    private long bannerUntil;
    private Component banner = Component.empty();
    private int bannerSeverity;

    @Override
    public void onInitializeClient() {
        config = AlarmConfig.load();
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("durability_alarm", "settings"));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.durability_alarm.settings", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("durability_alarm", "warnings"), hud::render);
    }

    private void tick(Minecraft client) {
        while (settingsKey.consumeClick()) {
            if (client.player != null && client.screen == null) {
                client.setScreen(new AlarmSettingsScreen(this));
            }
        }
        if (client.player != trackedPlayer) {
            trackedPlayer = client.player;
            resetTracking();
            demoUntil = 0;
        }
        if (client.player == null || client.level == null) return;
        if (client.isPaused()) return;
        tick++;
        if (!config.enabled || !client.player.isAlive() || client.player.isSpectator()
                || (!config.showInCreative && client.player.getAbilities().instabuild)) {
            if (!warnings.isEmpty()) resetTracking();
            return;
        }
        // At most 36 main slots, 5 equipment slots and the cursor, four scans per second.
        if (tick % 5 != 0) return;
        warnings = InventoryScanner.scan(client.player, config);
        List<AlertTracker.Observation> observations = warnings.stream().map(item ->
                new AlertTracker.Observation(item.identity(), item.remaining(), item.severity())).toList();
        int signal = tracker.update(observations, tick, config.repeatSeconds);
        if (signal > 0 && !warnings.isEmpty()) announce(client, warnings.get(0), signal);
    }

    private void announce(Minecraft client, WatchedItem item, int severity) {
        banner = Component.translatable("durability_alarm.alert", item.name(), item.remaining(), item.maximum());
        bannerSeverity = severity;
        bannerUntil = tick + 100;
        playAlert(client, severity);
        if (config.chat && client.player != null) {
            client.gui.getChat().addMessage(Component.literal("[Durability Alarm] ")
                    .append(banner.copy()).withStyle(severity >= 2 ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
    }

    private void playAlert(Minecraft client, int severity) {
        if (!config.sound || config.volumePercent == 0) return;
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(),
                severity >= DurabilityRules.CRITICAL ? 1.6F : 1.1F, config.volumePercent / 100.0F));
    }

    public void resetTracking() {
        warnings = List.of();
        tracker.reset();
        bannerUntil = 0;
    }

    public void showDemo() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        int maximum = pickaxe.getMaxDamage();
        pickaxe.set(DataComponents.DAMAGE, maximum - 5);
        demo = List.of(new WatchedItem(pickaxe, pickaxe.getHoverName(),
                Component.translatable("durability_alarm.demo"), "demo", 5, maximum, DurabilityRules.CRITICAL));
        demoUntil = tick + 160;
        playAlert(Minecraft.getInstance(), DurabilityRules.CRITICAL);
    }

    public AlarmConfig config() { return config; }
    public long ticks() { return tick; }
    public boolean isDemo() { return tick < demoUntil; }
    public List<WatchedItem> visibleWarnings() { return isDemo() ? demo : warnings; }
    public Component banner() { return banner; }
    public int bannerSeverity() { return bannerSeverity; }
    public boolean showBanner() { return !isDemo() && tick < bannerUntil && !warnings.isEmpty(); }
    public Component settingsKeyName() { return settingsKey.getTranslatedKeyMessage(); }
}
