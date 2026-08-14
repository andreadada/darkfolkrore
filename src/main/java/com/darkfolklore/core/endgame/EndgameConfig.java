package com.darkfolklore.core.endgame;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EndgameConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FORBIDDEN_ENDGAME = BUILDER
            .comment("Master toggle for Dark Folklore forbidden-endgame orchestration")
            .define("forbiddenEndgame", true);
    public static final ModConfigSpec.BooleanValue DAY_OF_BEAST = BUILDER
            .comment("Ritualized The Day of the Beast progression and player-built invocation monument")
            .define("dayOfTheBeastEndgame", true);
    public static final ModConfigSpec.BooleanValue CULT_OF_AZAZEL = BUILDER
            .comment("Cult of Azazel endgame milestones while keeping native altar/quota mechanics provider-owned")
            .define("cultOfAzazelEndgame", true);
    public static final ModConfigSpec.BooleanValue REQUIRE_WITCHING_HOUR = BUILDER
            .comment("Require the 17500-18500 world-time witching-hour window to activate the Demon Invocation Frame")
            .define("demonInvocationRequiresWitchingHour", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
    private EndgameConfig() {}
}
