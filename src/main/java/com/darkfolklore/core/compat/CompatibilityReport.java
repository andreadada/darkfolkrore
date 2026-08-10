package com.darkfolklore.core.compat;

public record CompatibilityReport(
        String modId,
        String displayName,
        String testedVersion,
        String actualVersion,
        String mechanism,
        CompatibilityStatus status,
        String detail
) {}
