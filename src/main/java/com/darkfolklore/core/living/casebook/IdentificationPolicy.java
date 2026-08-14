package com.darkfolklore.core.living.casebook;

import com.darkfolklore.core.investigation.Hypothesis;
import com.darkfolklore.core.living.LivingFolkloreConfig;
import java.util.List;

public final class IdentificationPolicy {
    private IdentificationPolicy() {}

    public static boolean conclusive(List<Hypothesis> ranked,int observed,int required,String factualConcept){
        return conclusive(ranked,observed,required,factualConcept,
                LivingFolkloreConfig.IDENTIFICATION_CONFIDENCE.get(),LivingFolkloreConfig.IDENTIFICATION_MARGIN.get());
    }

    public static boolean conclusive(List<Hypothesis> ranked,int observed,int required,String factualConcept,
                                     double confidenceFloor,double marginFloor){
        if(ranked==null||ranked.isEmpty()||observed<Math.max(1,required)||factualConcept==null)return false;
        Hypothesis first=ranked.getFirst();
        float second=ranked.size()>1?ranked.get(1).confidence():0.0F;
        return first.confidence()>=confidenceFloor
                &&first.confidence()-second>=marginFloor
                &&first.concept().equals(factualConcept);
    }
}
