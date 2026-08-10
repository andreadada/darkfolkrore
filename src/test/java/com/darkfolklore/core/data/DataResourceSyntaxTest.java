package com.darkfolklore.core.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataResourceSyntaxTest {
    @Test
    void everyShippedJsonResourceIsWellFormed() throws IOException {
        Path root = Path.of(System.getProperty("darkfolklore.resourcesDir"));
        assertTrue(Files.isDirectory(root), "configured source resource directory must exist");
        List<Path> jsonFiles;
        try (var paths = Files.walk(root)) {
            jsonFiles = paths.filter(path -> path.getFileName().toString().endsWith(".json")).toList();
        }

        assertTrue(jsonFiles.size() >= 50, "expected the complete data pack, not a partial resource tree");
        for (Path jsonFile : jsonFiles) {
            assertDoesNotThrow(() -> {
                try (Reader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    assertTrue(parsed.isJsonObject() || parsed.isJsonArray(), jsonFile + " must have a JSON container root");
                }
            }, () -> "invalid JSON in " + jsonFile);
        }
    }
}
