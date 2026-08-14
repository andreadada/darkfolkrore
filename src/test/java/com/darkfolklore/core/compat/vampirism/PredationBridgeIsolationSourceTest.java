package com.darkfolklore.core.compat.vampirism;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PredationBridgeIsolationSourceTest {
    @Test
    void compatibilityManagerLoadsWildBridgeFromVampirismInsteadOfFullMcaStack() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/darkfolklore/core/compat/CompatibilityManager.java"));

        int isolationComment = source.indexOf("Critical isolation rule: ordinary Vampirism predation depends only on Vampirism");
        assertTrue(isolationComment >= 0);
        String isolatedBlock = source.substring(isolationComment,
                source.indexOf("if (fullMcaVampStack) {", isolationComment));
        assertTrue(isolatedBlock.contains("if (vampirismExact) {"));
        assertTrue(isolatedBlock.contains("getConstructor(boolean.class).newInstance(fullMcaVampStack)"));
        assertFalse(isolatedBlock.contains("if (fullMcaVampStack)"));
    }

    @Test
    void predationCompatKeepsWildBaselineIndependentFromMcaProbes() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/darkfolklore/core/compat/vampirism/VampirePredationCompat.java"));

        assertTrue(source.contains("baseline.put(Circuit.WILD_FEED, true);"));
        assertTrue(source.contains("return wildRuntimeAvailable() || mcaRuntimeAvailable();"));
        assertTrue(source.contains("available && entity instanceof IVampireMob"));
        assertTrue(source.contains("restoreBaseline();"));
    }
}
