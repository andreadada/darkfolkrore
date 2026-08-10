package com.darkfolklore.core.compat.mca;

import java.util.List;

/** Pure relationship contribution model. It never promotes a belief to a fact. */
public final class McaTrustModel {
    private McaTrustModel() {}

    public static McaTrustResult evaluate(McaSocialContext context) {
        return evaluate(context, McaTrustSettings.DEFAULTS);
    }

    public static McaTrustResult evaluate(McaSocialContext context, McaTrustSettings settings) {
        McaTrustContribution contribution = switch (context.relationship()) {
            case SELF -> contribution("MCA source is observer", settings.self());
            case SPOUSE -> contribution("MCA spouse", settings.spouse());
            case SOURCE_IS_PARENT -> contribution("MCA source is observer's parent", settings.parentOrChild());
            case SOURCE_IS_CHILD -> contribution("MCA source is observer's child", settings.parentOrChild());
            case SIBLING -> contribution("MCA sibling", settings.sibling());
            case PLAYER_FRIEND -> contribution("MCA configured player-friend threshold", settings.playerFriend());
            case PLAYER_BOUNTY_TARGET -> contribution("MCA configured bounty-target threshold",
                    settings.playerBountyTarget());
            case STRANGER, UNKNOWN, NOT_APPLICABLE -> null;
        };
        return contribution == null
                ? new McaTrustResult(0.0, List.of())
                : new McaTrustResult(contribution.amount(), List.of(contribution));
    }

    private static McaTrustContribution contribution(String reason, double value) {
        return value == 0.0 ? null : new McaTrustContribution(reason, value);
    }
}
