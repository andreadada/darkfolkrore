package com.darkfolklore.core.investigation;

import com.darkfolklore.core.knowledge.social.EvidenceType;
import com.darkfolklore.core.traits.CreatureTrait;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HypothesisEngineTest {
    @Test
    void genericBloodAndBiteCanKeepMultiplePredatorsPlausible() {
        InvestigationProfile vampire = new InvestigationProfile("darkfolklore:vampire",
                Set.of(CreatureTrait.VAMPIRE), Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK,
                EvidenceType.GARLIC_REACTION), Map.of(), List.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK), 3, 96);
        InvestigationProfile chupacabra = new InvestigationProfile("darkfolklore:chupacabra",
                Set.of(CreatureTrait.CRYPTID), Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK, EvidenceType.TRACK),
                Map.of(), List.of(EvidenceType.BLOOD, EvidenceType.TRACK), 3, 96);
        var ranked = HypothesisEngine.rank(Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK),
                List.of(vampire, chupacabra));
        assertEquals(2, ranked.size());
        assertEquals(ranked.get(0).score(), ranked.get(1).score());
    }

    @Test
    void garlicReactionBreaksTheTieTowardVampire() {
        InvestigationProfile vampire = new InvestigationProfile("darkfolklore:vampire",
                Set.of(CreatureTrait.VAMPIRE), Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK,
                EvidenceType.GARLIC_REACTION), Map.of(), List.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK), 3, 96);
        InvestigationProfile chupacabra = new InvestigationProfile("darkfolklore:chupacabra",
                Set.of(CreatureTrait.CRYPTID), Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK, EvidenceType.TRACK),
                Map.of(), List.of(EvidenceType.BLOOD, EvidenceType.TRACK), 3, 96);
        var ranked = HypothesisEngine.rank(Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK, EvidenceType.GARLIC_REACTION),
                List.of(vampire, chupacabra));
        assertFalse(ranked.isEmpty());
        assertEquals("darkfolklore:vampire", ranked.getFirst().concept());
    }

    @Test
    void spiritAndSoulEvidenceFavorSpectralProfiles() {
        InvestigationProfile ghost = new InvestigationProfile("darkfolklore:ghost",
                Set.of(CreatureTrait.SPIRIT), Set.of(EvidenceType.SPIRIT_ECHO, EvidenceType.SOUL_ECHO),
                Map.of(), List.of(EvidenceType.SPIRIT_ECHO), 2, 80);
        InvestigationProfile vampire = new InvestigationProfile("darkfolklore:vampire",
                Set.of(CreatureTrait.VAMPIRE), Set.of(EvidenceType.BLOOD, EvidenceType.BITE_MARK),
                Map.of(), List.of(EvidenceType.BLOOD), 2, 96);
        var ranked = HypothesisEngine.rank(Set.of(EvidenceType.SPIRIT_ECHO, EvidenceType.SOUL_ECHO),
                List.of(ghost, vampire));
        assertFalse(ranked.isEmpty());
        assertEquals("darkfolklore:ghost", ranked.getFirst().concept());
    }
}
