package dev.zadko.durabilityalarm;

import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AlarmSettingsScreen extends Screen {
    private final DurabilityAlarmClient mod;
    private final AlarmConfig config;

    public AlarmSettingsScreen(DurabilityAlarmClient mod) {
        super(Component.translatable("durability_alarm.settings.title"));
        this.mod = mod;
        this.config = mod.config();
    }

    @Override
    protected void init() {
        int totalWidth = Math.min(380, width - 20);
        int column = (totalWidth - 8) / 2;
        int left = (width - totalWidth) / 2;
        int right = left + column + 8;
        int top = Math.max(36, (height - 184) / 2);

        button(left, top, column, () -> toggle("enabled", config.enabled), () -> config.enabled = !config.enabled, "enabled");
        button(right, top, column, () -> tr("scope", tr(config.allInventory ? "scope.all" : "scope.equipment")),
                () -> config.allInventory = !config.allInventory, "scope");

        NumberSlider warning = new NumberSlider(left, top + 24, column, "warning", 1, 50,
                config.warningPercent, value -> config.warningPercent = value);
        NumberSlider critical = new NumberSlider(right, top + 24, column, "critical", 1, 50,
                config.criticalPercent, value -> config.criticalPercent = value);
        // Keep the two sliders consistent in both directions.
        warning.afterChange = () -> {
            if (config.criticalPercent > config.warningPercent) critical.setNumber(config.warningPercent);
        };
        critical.afterChange = () -> {
            if (config.criticalPercent > config.warningPercent) warning.setNumber(config.criticalPercent);
        };
        addRenderableWidget(warning);
        addRenderableWidget(critical);

        button(left, top + 48, column, () -> toggle("sound", config.sound), () -> config.sound = !config.sound, "sound");
        addRenderableWidget(new NumberSlider(right, top + 48, column, "volume", 0, 100,
                config.volumePercent, value -> config.volumePercent = value));

        button(left, top + 72, column, () -> toggle("chat", config.chat), () -> config.chat = !config.chat, "chat");
        button(right, top + 72, column, () -> toggle("creative", config.showInCreative),
                () -> config.showInCreative = !config.showInCreative, "creative");

        button(left, top + 96, column, () -> tr("position", tr("position." + config.position)),
                () -> config.position = (config.position + 1) % 4, "position");
        button(right, top + 96, column, () -> tr("repeat", config.repeatSeconds == 0
                        ? tr("off") : tr("seconds", config.repeatSeconds)),
                () -> config.repeatSeconds = nextRepeat(config.repeatSeconds), "repeat");

        button(left, top + 120, column, () -> tr("rows", config.maxRows),
                () -> config.maxRows = config.maxRows >= 9 ? 3 : config.maxRows + 3, "rows");
        addRenderableWidget(Button.builder(tr("test"), button -> {
            onClose();
            mod.showDemo();
        }).bounds(right, top + 120, column, 20).tooltip(Tooltip.create(tr("hint.test"))).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left, top + 152, totalWidth, 20).build());
    }

    private void button(int x, int y, int width, Supplier<Component> text, Runnable action, String hint) {
        addRenderableWidget(Button.builder(text.get(), button -> {
            action.run();
            button.setMessage(text.get());
        }).bounds(x, y, width, 20).tooltip(Tooltip.create(tr("hint." + hint))).build());
    }

    private static int nextRepeat(int current) {
        if (current == 0) return 15;
        if (current < 30) return 30;
        if (current < 60) return 60;
        if (current < 120) return 120;
        return 0;
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable("durability_alarm.settings." + key, args);
    }

    private static Component toggle(String key, boolean value) {
        return tr(key, tr(value ? "on" : "off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Let the standard Screen pipeline handle the background and blur once.
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = Math.max(36, (height - 184) / 2);
        graphics.drawCenteredString(font, title, width / 2, top - 27, 0xFFFFC566);
        graphics.drawCenteredString(font, tr("subtitle"), width / 2, top - 13, 0xFFAAB6C9);
    }

    @Override
    public void onClose() {
        boolean saved = config.save();
        mod.resetTracking();
        minecraft.setScreen(null);
        if (!saved && minecraft.player != null) {
            minecraft.gui.getChat().addMessage(tr("save_error"));
        }
    }

    @Override
    public boolean isPauseScreen() { return true; }

    private static final class NumberSlider extends AbstractSliderButton {
        private final String key;
        private final int minimum;
        private final int maximum;
        private final IntConsumer onChange;
        private Runnable afterChange = () -> {};

        NumberSlider(int x, int y, int width, String key, int min, int max, int current, IntConsumer onChange) {
            super(x, y, width, 20, Component.empty(), (current - min) / (double) (max - min));
            this.key = key;
            this.minimum = min;
            this.maximum = max;
            this.onChange = onChange;
            setTooltip(Tooltip.create(tr("hint." + key)));
            updateMessage();
        }

        private int number() { return minimum + (int) Math.round(value * (maximum - minimum)); }

        private void setNumber(int number) {
            value = (number - minimum) / (double) (maximum - minimum);
            onChange.accept(number());
            updateMessage();
        }

        @Override
        protected void updateMessage() { setMessage(tr(key, number())); }

        @Override
        protected void applyValue() {
            onChange.accept(number());
            afterChange.run();
        }
    }
}
