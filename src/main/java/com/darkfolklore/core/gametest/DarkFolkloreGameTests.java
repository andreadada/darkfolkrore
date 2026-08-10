package com.darkfolklore.core.gametest;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.social.SecretClaimKey;
import com.darkfolklore.core.knowledge.social.SecretType;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeKey;
import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.society.organization.Organization;
import com.darkfolklore.core.society.organization.OrganizationType;
import com.darkfolklore.core.compat.mcacapitals.PoliticalRole;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Small live-server regression suite; pure policy remains covered by fast JUnit tests. */
@GameTestHolder(DarkFolkloreCore.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DarkFolkloreGameTests {
    private DarkFolkloreGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty", timeoutTicks = 40)
    public static void validatedDataPackStateIsAvailable(GameTestHelper helper) {
        helper.assertTrue(FolkloreDataManager.INSTANCE.validationErrors().isEmpty(),
                "Dark Folklore data reload reported validation errors");
        helper.assertTrue(FolkloreDataManager.INSTANCE.canonical().concept("darkfolklore:vampire").isPresent(),
                "vampire canonical definition did not load");
        helper.assertTrue(FolkloreDataManager.INSTANCE.storyTemplates().size() >= 10,
                "society story templates did not load");
        for (OrganizationType type : OrganizationType.values()) {
            helper.assertTrue(FolkloreDataManager.INSTANCE.organizationArchetype(type).isPresent(),
                    "missing organization archetype " + type);
        }
        helper.assertTrue(FolkloreDataManager.INSTANCE.politicalWeights(PoliticalRole.SOVEREIGN).credibility() > 0,
                "political weighting definitions did not load");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty", timeoutTicks = 40)
    public static void falseBeliefNeverChangesFactualVillagerState(GameTestHelper helper) {
        Villager subject = helper.spawn(EntityType.VILLAGER, 0, 1, 0);
        UUID observer = UUID.randomUUID();
        FolkloreSavedData data = FolkloreSavedData.get(helper.getLevel().getServer());
        data.markPublic(new SecretClaimKey(subject.getUUID(), SecretType.VAMPIRE), helper.getLevel().getGameTime());

        helper.assertTrue(data.social(new SocialKnowledgeKey(observer, subject.getUUID(), SecretType.VAMPIRE))
                        .map(record -> record.state() == SocialKnowledgeState.PUBLIC).orElse(false),
                "global public belief was not visible to a new observer");
        helper.assertTrue(!SecretFacts.actualSecrets(subject).contains(SecretType.VAMPIRE),
                "belief incorrectly mutated the villager's factual supernatural state");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "bastion/mobs/empty", timeoutTicks = 40)
    public static void organizationLeaderSuccessionRunsInWorldState(GameTestHelper helper) {
        UUID leader = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID successor = UUID.fromString("10000000-0000-0000-0000-000000000001");
        Organization organization = new Organization(UUID.randomUUID(), OrganizationType.HUNTER_SOCIETY,
                "GameTest Watch", leader);
        organization.addMember(successor);
        FolkloreSavedData data = FolkloreSavedData.get(helper.getLevel().getServer());
        data.putOrganization(organization);
        FolkloreSavedData.DeathCleanupResult result = data.handleConfirmedDeath(leader,
                helper.getLevel().getGameTime());

        helper.assertTrue(result.successions() == 1, "confirmed death did not trigger one succession");
        helper.assertTrue(organization.leader().equals(successor), "deterministic successor was not selected");
        helper.assertTrue(data.organizationsForMember(leader).isEmpty(), "dead leader remained in membership index");
        helper.succeed();
    }
}
