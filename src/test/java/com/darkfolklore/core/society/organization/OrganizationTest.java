package com.darkfolklore.core.society.organization;

import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationTest {
    @Test
    void membershipIsUniqueAndLeaderCannotBeRemoved() {
        UUID leader = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        Organization organization = new Organization(UUID.randomUUID(),
                OrganizationType.HUNTER_SOCIETY, "Founders Council", leader);
        assertTrue(organization.addMember(member));
        assertFalse(organization.addMember(member));
        assertFalse(organization.removeMember(leader));
        assertEquals(2, organization.members().size());
    }

    @Test
    void organizationHistoriesAndIntelligenceAreStrictlyBounded() {
        Organization organization = new Organization(UUID.randomUUID(),
                OrganizationType.HUNTER_SOCIETY, "Bounded Watch", UUID.randomUUID());
        for (int i = 0; i < Organization.MAX_EVENT_HISTORY + 20; i++) {
            organization.addEvent(OrganizationEvent.of(OrganizationEventType.INTELLIGENCE_RECEIVED,
                    i, null, UUID.randomUUID(), "report " + i));
        }
        for (int i = 0; i < Organization.MAX_INTELLIGENCE + 20; i++) {
            organization.recordIntelligence(new OrganizationIntelKey(UUID.randomUUID(), SecretType.VAMPIRE),
                    SocialKnowledgeState.RUMOR);
        }
        assertEquals(Organization.MAX_EVENT_HISTORY, organization.events().size());
        assertEquals(Organization.MAX_INTELLIGENCE, organization.intelligence().size());
    }

    @Test
    void naturalAffiliationNeverInventsASecretAndHuntersDoNotAutoFound() {
        assertTrue(OrganizationRules.naturalAffiliation(EnumSet.noneOf(SecretType.class)).isEmpty());
        assertEquals(OrganizationType.VAMPIRE_COVEN,
                OrganizationRules.naturalAffiliation(EnumSet.of(SecretType.VAMPIRE)).orElseThrow());
        assertFalse(OrganizationRules.mayAutoFound(OrganizationType.HUNTER_SOCIETY));
        assertTrue(OrganizationRules.mayAutoFound(OrganizationType.WEREWOLF_PACK));
    }

    @Test
    void memberSeenWritesAreThrottledButTimeRollbackCanRepairTheTimestamp() {
        UUID leader = UUID.randomUUID();
        Organization organization = new Organization(UUID.randomUUID(),
                OrganizationType.HUNTER_SOCIETY, "Quiet Ledger", leader);

        assertTrue(organization.markMemberSeen(leader, 1_000L));
        assertFalse(organization.markMemberSeen(leader, 1_000L + Organization.MEMBER_SEEN_WRITE_INTERVAL - 1L));
        assertEquals(1_000L, organization.memberLastSeen().get(leader));

        assertTrue(organization.markMemberSeen(leader, 1_000L + Organization.MEMBER_SEEN_WRITE_INTERVAL));
        assertEquals(1_000L + Organization.MEMBER_SEEN_WRITE_INTERVAL,
                organization.memberLastSeen().get(leader));

        assertTrue(organization.markMemberSeen(leader, 500L));
        assertEquals(500L, organization.memberLastSeen().get(leader));
    }
}
