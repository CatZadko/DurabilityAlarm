package dev.zadko.durabilityalarm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AlarmConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Durability Alarm");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = true;
    public boolean allInventory = true;
    public boolean sound = true;
    public boolean chat = false;
    public boolean showInCreative = false;
    public int warningPercent = 10;
    public int criticalPercent = 3;
    public int repeatSeconds = 30;
    public int volumePercent = 70;
    public int maxRows = 6;
    /** 0=top left, 1=top right, 2=bottom left, 3=bottom right. */
    public int position = 0;

    public void normalize() {
        warningPercent = Math.max(1, Math.min(50, warningPercent));
        criticalPercent = Math.max(1, Math.min(warningPercent, criticalPercent));
        repeatSeconds = repeatSeconds == 0 ? 0 : Math.max(5, Math.min(120, repeatSeconds));
        volumePercent = Math.max(0, Math.min(100, volumePercent));
        maxRows = Math.max(1, Math.min(12, maxRows));
        position = Math.floorMod(position, 4);
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("durability-alarm.json");
    }

    public static AlarmConfig load() {
        Path path = path();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                AlarmConfig config = GSON.fromJson(reader, AlarmConfig.class);
                if (config == null) throw new IOException("Empty/null config");
                config.normalize();
                return config;
            } catch (IOException | RuntimeException error) {
                LOGGER.warn("Could not read {}; using defaults", path, error);
                try {
                    Files.copy(path, path.resolveSibling("durability-alarm.invalid-"
                            + System.currentTimeMillis() + ".json"));
                } catch (IOException copyError) {
                    LOGGER.warn("Could not back up the invalid config", copyError);
                }
            }
        }
        AlarmConfig config = new AlarmConfig();
        config.save();
        return config;
    }

    public boolean save() {
        normalize();
        Path path = path();
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "durability-alarm-", ".tmp");
            Files.writeString(temporary, GSON.toJson(this) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException error) {
            LOGGER.error("Could not save {}", path, error);
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) { /* Already logged if the write failed. */ }
            }
        }
    }
}
