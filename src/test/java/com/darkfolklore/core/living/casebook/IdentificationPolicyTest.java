package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.investigation.Hypothesis;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IdentificationPolicyTest {
    @Test void clueCountAloneCannotIdentify(){
        var ranked=List.of(new Hypothesis("darkfolklore:vampire",8,2,2,.95F));
        assertFalse(IdentificationPolicy.conclusive(ranked,2,3,"darkfolklore:vampire",.67,.15));
    }
    @Test void closeAlternativesRemainContested(){
        var ranked=List.of(new Hypothesis("darkfolklore:vampire",10,3,3,.80F),new Hypothesis("darkfolklore:chupacabra",9,3,3,.72F));
        assertFalse(IdentificationPolicy.conclusive(ranked,3,3,"darkfolklore:vampire",.67,.15));
    }
    @Test void discriminatingEvidenceCanBecomeConclusive(){
        var ranked=List.of(new Hypothesis("darkfolklore:vampire",16,4,4,.90F),new Hypothesis("darkfolklore:chupacabra",5,2,4,.45F));
        assertTrue(IdentificationPolicy.conclusive(ranked,4,3,"darkfolklore:vampire",.67,.15));
    }
    @Test void differentLeadingConceptCannotAuthorizeTarget(){
        var ranked=List.of(new Hypothesis("darkfolklore:werewolf",18,4,4,.95F),new Hypothesis("darkfolklore:vampire",2,1,4,.20F));
        assertFalse(IdentificationPolicy.conclusive(ranked,4,3,"darkfolklore:vampire",.67,.15));
    }
}
