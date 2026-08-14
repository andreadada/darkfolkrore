package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.magic.MagicTradition;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record CaseNote(long gameTime, CaseNoteKind kind, String detail, Optional<UUID> source,
                       Optional<EvidenceType> evidence, Optional<MagicTradition> tradition, float confidence) {
    public CaseNote {
        gameTime = Math.max(0L, gameTime);
        Objects.requireNonNull(kind);
        detail = Objects.requireNonNullElse(detail, "").trim();
        if (detail.length() > 192) detail = detail.substring(0, 192);
        source = source == null ? Optional.empty() : source;
        evidence = evidence == null ? Optional.empty() : evidence;
        tradition = tradition == null ? Optional.empty() : tradition;
        confidence = Math.max(0.0F, Math.min(1.0F, confidence));
    }
}
