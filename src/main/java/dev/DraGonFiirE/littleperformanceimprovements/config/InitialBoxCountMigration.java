package dev.DraGonFiirE.littleperformanceimprovements.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.*;

/** Installation migration, deliberately separate from editable LT defaults. */
public final class InitialBoxCountMigration {
    private InitialBoxCountMigration() {}

    public static synchronized void apply(Path directory) throws IOException {
        Path marker = directory.resolve("littleperformanceimprovements-box-counts-v1.done");
        if (Files.exists(marker)) return;
        Path config = directory.resolve("littletiles-client.json");
        JsonObject root = Files.exists(config)
                ? JsonParser.parseString(Files.readString(config)).getAsJsonObject() : new JsonObject();
        section(root, "building").addProperty("lowResolutionBoxCount", 1_000_000);
        section(root, "rendering").addProperty("itemLowResolutionBoxCount", 1_000_000);
        Files.createDirectories(directory);
        Path backup = directory.resolve("littletiles-client.before-lpi-box-counts.json.bak");
        if (Files.exists(config) && !Files.exists(backup)) Files.copy(config, backup);
        Path temporary = Files.createTempFile(directory, "lpi-box-counts-", ".tmp");
        try {
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n");
            try {
                Files.move(temporary, config, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, config, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        // Only mark complete after the values have been durably written to LT's config.
        Files.writeString(marker, "Applied initial LPI box counts. Keep this file to preserve later user choices.\n",
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    private static JsonObject section(JsonObject root, String name) {
        if (root.has(name)) return root.getAsJsonObject(name);
        JsonObject result = new JsonObject();
        root.add(name, result);
        return result;
    }
}
