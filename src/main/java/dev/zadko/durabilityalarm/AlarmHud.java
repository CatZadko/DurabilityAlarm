package dev.zadko.durabilityalarm;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class AlarmHud {
    private static final int AMBER = 0xFFFFC566;
    private static final int RED = 0xFFFF717B;
    private static final int WHITE = 0xFFF2F3F7;
    private final DurabilityAlarmClient mod;

    public AlarmHud(DurabilityAlarmClient mod) { this.mod = mod; }

    public void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden() || client.gui.screen() != null) return;
        List<WatchedItem> items = mod.visibleWarnings();
        if (items.isEmpty() || (!mod.config().enabled && !mod.isDemo())) return;
        Font font = client.font;
        int width = Math.min(262, graphics.guiWidth() - 16);
        if (width < 100) return;
        int capacity = Math.max(1, Math.min(mod.config().maxRows, (graphics.guiHeight() - 114) / 31));
        int visible = Math.min(capacity, items.size());
        int pages = items.size() <= capacity ? 1 : capacity == 1 ? items.size()
                : (items.size() - 2) / (capacity - 1) + 1;
        int page = (int) ((mod.ticks() / 100) % pages);
        int height = 24 + visible * 31 + 15;
        int position = mod.config().position;
        int x = position % 2 == 0 ? 8 : graphics.guiWidth() - width - 8;
        int y = position < 2 ? 32 : Math.max(8, graphics.guiHeight() - height - 72);
        int accent = items.get(0).severity() >= 2 ? RED : AMBER;

        graphics.fill(x, y, x + width, y + height, 0xDD11151E);
        graphics.fill(x, y, x + 3, y + height, accent);
        graphics.text(font, Component.translatable(mod.isDemo() ? "durability_alarm.hud.demo"
                : "durability_alarm.hud.title"), x + 10, y + 8, accent, false);

        for (int row = 0; row < visible; row++) {
            // Keep the most endangered item on screen while cycling the remaining pages.
            int index = pages == 1 ? row : capacity == 1 ? page
                    : row == 0 ? 0 : 1 + page * (capacity - 1) + row - 1;
            if (index >= items.size()) break;
            WatchedItem item = items.get(index);
            int rowY = y + 24 + row * 31;
            int color = item.severity() >= 2 ? RED : AMBER;
            graphics.item(item.icon(), x + 10, rowY + 3);
            graphics.text(font, fit(font, item.name().getString(), width - 43), x + 33, rowY, WHITE, true);
            String detail = item.remaining() + " / " + item.maximum() + "  ("
                    + String.format(Locale.ROOT, "%.1f", item.percent()) + "%)";
            graphics.text(font, fit(font, detail, width - 43), x + 33, rowY + 10, color, false);
            graphics.text(font, fit(font, item.location().getString(), width - 43),
                    x + 33, rowY + 20, 0xFF9AA5B7, false);
            int barX = x + 10;
            int barY = rowY + 22;
            graphics.fill(barX, barY, barX + 16, barY + 2, 0xFF384052);
            int barWidth = (int) Math.round(16 * item.percent() / 100);
            if (item.remaining() > 0) barWidth = Math.max(1, barWidth);
            graphics.fill(barX, barY, barX + barWidth, barY + 2, color);
        }
        Component footer = pages > 1
                ? Component.translatable("durability_alarm.hud.pages", items.size(), page + 1, pages)
                : Component.translatable("durability_alarm.hud.settings", mod.settingsKeyName());
        graphics.text(font, fit(font, footer.getString(), width - 20), x + 10, y + height - 11, 0xFF9AA5B7, false);

        if (mod.showBanner()) {
            String text = fit(font, mod.banner().getString(), graphics.guiWidth() - 40);
            int bannerWidth = font.width(text) + 20;
            int bannerX = (graphics.guiWidth() - bannerWidth) / 2;
            int bannerY = graphics.guiHeight() - 97;
            int color = mod.bannerSeverity() >= 2 ? RED : AMBER;
            graphics.fill(bannerX, bannerY, bannerX + bannerWidth, bannerY + 22, 0xE211151E);
            graphics.fill(bannerX, bannerY, bannerX + bannerWidth, bannerY + 2, color);
            graphics.centeredText(font, text, graphics.guiWidth() / 2, bannerY + 8, color);
        }
    }

    private static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        String ellipsis = "…";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(ellipsis))) + ellipsis;
    }
}
