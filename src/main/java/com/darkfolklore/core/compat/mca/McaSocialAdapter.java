package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.CompatCapabilityCircuit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Fail-closed, exact-version bridge to MCA's read-only social APIs.
 *
 * <p>Family relationships, player affinity, personality and traits are deliberately probed as independent
 * capabilities. A signature change in one optional social surface therefore degrades only that evidence instead
 * of disabling every MCA social read.</p>
 */
public final class McaSocialAdapter {
    public static final String MOD_ID = "mca";
    public static final String TESTED_VERSION = "7.7.32+1.21.1";

    private final CompatCapabilityCircuit actors = new CompatCapabilityCircuit("actors");
    private final CompatCapabilityCircuit family = new CompatCapabilityCircuit("family");
    private final CompatCapabilityCircuit affinity = new CompatCapabilityCircuit("affinity");
    private final CompatCapabilityCircuit personality = new CompatCapabilityCircuit("personality");
    private final CompatCapabilityCircuit traits = new CompatCapabilityCircuit("traits");

    private Class<?> villagerClass;

    private Method familyTreeGet;
    private Method familyTreeGetOrEmpty;
    private Method nodePartner;
    private Method nodeRelationshipState;
    private Method relationshipStateIsMarried;
    private Method nodeIsParent;
    private Method nodeSiblings;

    private Method affinityVillagerGetBrain;
    private Method brainGetMemories;
    private Method memoriesGetHearts;
    private Method configGetInstance;
    private Field friendThreshold;
    private Field bountyThreshold;

    private Method personalityVillagerGetBrain;
    private Method brainGetPersonality;

    private Method villagerGetTraits;
    private Method traitsGetTraits;
    private Method traitId;

    private volatile boolean ready;
    private volatile String statusDetail = "not initialized";

    public synchronized boolean initialize(String actualVersion) {
        reset();
        if (!TESTED_VERSION.equals(actualVersion)) {
            failAll("requires exact MCA " + TESTED_VERSION + ", found " + actualVersion);
            statusDetail = "disabled: " + actors.detail();
            return false;
        }

        ClassLoader loader = McaSocialAdapter.class.getClassLoader();
        try {
            villagerClass = Class.forName("net.conczin.mca.entity.VillagerEntityMCA", false, loader);
            actors.markReady("VillagerEntityMCA resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            failAll("MCA actor signature mismatch: " + exception.getClass().getSimpleName());
            statusDetail = "disabled: " + actors.detail();
            return false;
        }

        probeFamily(loader);
        probeAffinity(loader);
        probePersonality(loader);
        probeTraits(loader);

        ready = actors.available() && (family.available() || affinity.available()
                || personality.available() || traits.available());
        statusDetail = (fullyReady() ? "active: " : ready ? "partial: " : "disabled: ") + diagnosticDetail();
        return ready;
    }

    private void probeFamily(ClassLoader loader) {
        try {
            Class<?> familyTree = Class.forName("net.conczin.mca.server.world.data.FamilyTree", false, loader);
            Class<?> node = Class.forName("net.conczin.mca.server.world.data.FamilyTreeNode", false, loader);
            Class<?> relationshipState = Class.forName(
                    "net.conczin.mca.entity.ai.relationship.RelationshipState", false, loader);
            familyTreeGet = familyTree.getMethod("get", ServerLevel.class);
            familyTreeGetOrEmpty = familyTree.getMethod("getOrEmpty", UUID.class);
            nodePartner = node.getMethod("partner");
            nodeRelationshipState = node.getMethod("getRelationshipState");
            relationshipStateIsMarried = relationshipState.getMethod("isMarried");
            nodeIsParent = node.getMethod("isParent", UUID.class);
            nodeSiblings = node.getMethod("siblings");
            family.markReady("family tree members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            failCircuit(family, "family initialization", exception);
        }
    }

    private void probeAffinity(ClassLoader loader) {
        try {
            Class<?> brain = Class.forName("net.conczin.mca.entity.ai.brain.VillagerBrain", false, loader);
            Class<?> memories = Class.forName("net.conczin.mca.entity.ai.Memories", false, loader);
            Class<?> config = Class.forName("net.conczin.mca.Config", false, loader);
            affinityVillagerGetBrain = villagerClass.getMethod("getVillagerBrain");
            brainGetMemories = brain.getMethod("getMemories");
            memoriesGetHearts = memories.getMethod("getHearts");
            configGetInstance = config.getMethod("getInstance");
            friendThreshold = config.getField("heartsToBeConsideredAsFriend");
            bountyThreshold = config.getField("bountyHunterHearts");
            affinity.markReady("player affinity members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            failCircuit(affinity, "affinity initialization", exception);
        }
    }

    private void probePersonality(ClassLoader loader) {
        try {
            Class<?> brain = Class.forName("net.conczin.mca.entity.ai.brain.VillagerBrain", false, loader);
            personalityVillagerGetBrain = villagerClass.getMethod("getVillagerBrain");
            brainGetPersonality = brain.getMethod("getPersonality");
            personality.markReady("personality members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            failCircuit(personality, "personality initialization", exception);
        }
    }

    private void probeTraits(ClassLoader loader) {
        try {
            Class<?> traitCollection = Class.forName("net.conczin.mca.entity.ai.Traits", false, loader);
            Class<?> trait = Class.forName("net.conczin.mca.entity.ai.Traits$Trait", false, loader);
            villagerGetTraits = villagerClass.getMethod("getTraits");
            traitsGetTraits = traitCollection.getMethod("getTraits");
            traitId = trait.getMethod("id");
            traits.markReady("trait members resolved");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            failCircuit(traits, "traits initialization", exception);
        }
    }

    public boolean isReady() { return ready; }

    public boolean fullyReady() {
        return ready && family.available() && affinity.available() && personality.available() && traits.available();
    }

    public String statusDetail() { return statusDetail; }

    public String diagnosticDetail() {
        return "exact MCA " + TESTED_VERSION + "; " + actors.detail() + ", " + family.detail() + ", "
                + affinity.detail() + ", " + personality.detail() + ", " + traits.detail();
    }

    public Map<String, Boolean> circuitStatus() {
        return Map.of(
                "actors", actors.available(),
                "family", family.available(),
                "affinity", affinity.available(),
                "personality", personality.available(),
                "traits", traits.available());
    }

    public McaSocialContext relationship(Entity observer, Entity source) {
        if (!ready || !actors.available()) return McaSocialContext.unavailable(statusDetail);
        if (!isSocialActor(observer) || !isSocialActor(source)) {
            return McaSocialContext.notApplicable("both actors must be MCA villagers or players");
        }
        if (!(observer.level() instanceof ServerLevel level)
                || !(source.level() instanceof ServerLevel sourceLevel)
                || level.getServer() != sourceLevel.getServer()) {
            return McaSocialContext.unavailable("actors are not on the same logical server");
        }

        Optional<String> observerPersonality = personality(observer);
        Optional<String> sourcePersonality = personality(source);
        UUID observerId = observer.getUUID();
        UUID sourceId = source.getUUID();
        if (observerId.equals(sourceId)) {
            return context(McaRelationshipCategory.SELF, OptionalInt.empty(), observerPersonality,
                    sourcePersonality, "same UUID");
        }

        boolean familyRecordsPresent = false;
        if (family.available()) {
            try {
                Object tree = familyTreeGet.invoke(null, level);
                Object observerNode = optionalValue(familyTreeGetOrEmpty.invoke(tree, observerId));
                Object sourceNode = optionalValue(familyTreeGetOrEmpty.invoke(tree, sourceId));
                familyRecordsPresent = observerNode != null && sourceNode != null;
                if (familyRecordsPresent) {
                    Object partner = nodePartner.invoke(observerNode);
                    Object relationshipState = nodeRelationshipState.invoke(observerNode);
                    if (sourceId.equals(partner)
                            && Boolean.TRUE.equals(relationshipStateIsMarried.invoke(relationshipState))) {
                        return context(McaRelationshipCategory.SPOUSE, OptionalInt.empty(), observerPersonality,
                                sourcePersonality, "MCA family tree partner with married state");
                    }
                    if (Boolean.TRUE.equals(nodeIsParent.invoke(observerNode, sourceId))) {
                        return context(McaRelationshipCategory.SOURCE_IS_PARENT, OptionalInt.empty(), observerPersonality,
                                sourcePersonality, "MCA family tree parent");
                    }
                    if (Boolean.TRUE.equals(nodeIsParent.invoke(sourceNode, observerId))) {
                        return context(McaRelationshipCategory.SOURCE_IS_CHILD, OptionalInt.empty(), observerPersonality,
                                sourcePersonality, "MCA family tree child");
                    }
                    Object siblings = nodeSiblings.invoke(observerNode);
                    if (siblings instanceof Set<?> set && set.contains(sourceId)) {
                        return context(McaRelationshipCategory.SIBLING, OptionalInt.empty(), observerPersonality,
                                sourcePersonality, "MCA family tree sibling");
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                failCircuit(family, "relationship query", exception);
                familyRecordsPresent = false;
            }
        }

        OptionalInt playerAffinity = OptionalInt.empty();
        if (affinity.available()) {
            try {
                playerAffinity = playerAffinity(observer, source);
                if (playerAffinity.isPresent()) {
                    Object config = configGetInstance.invoke(null);
                    int hearts = playerAffinity.getAsInt();
                    if (hearts >= friendThreshold.getInt(config)) {
                        return context(McaRelationshipCategory.PLAYER_FRIEND, playerAffinity, observerPersonality,
                                sourcePersonality, "MCA heartsToBeConsideredAsFriend threshold");
                    }
                    if (hearts <= bountyThreshold.getInt(config)) {
                        return context(McaRelationshipCategory.PLAYER_BOUNTY_TARGET, playerAffinity, observerPersonality,
                                sourcePersonality, "MCA bountyHunterHearts threshold");
                    }
                    return context(McaRelationshipCategory.STRANGER, playerAffinity, observerPersonality,
                            sourcePersonality, "player affinity exists but reaches no verified MCA relationship threshold");
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                failCircuit(affinity, "affinity query", exception);
            }
        }

        if (familyRecordsPresent) {
            return context(McaRelationshipCategory.STRANGER, OptionalInt.empty(), observerPersonality,
                    sourcePersonality, "no verified MCA family relationship");
        }
        String detail = (!family.available() && !affinity.available())
                ? "MCA family and affinity capabilities are unavailable"
                : "MCA family record or player affinity is absent";
        return context(McaRelationshipCategory.UNKNOWN, OptionalInt.empty(), observerPersonality, sourcePersonality, detail);
    }

    public Optional<String> personality(Entity entity) {
        if (!ready || !actors.available() || !personality.available() || !villagerClass.isInstance(entity)) {
            return Optional.empty();
        }
        try {
            Object brain = personalityVillagerGetBrain.invoke(entity);
            Object value = brainGetPersonality.invoke(brain);
            return value instanceof Enum<?> personalityValue ? Optional.of(personalityValue.name()) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            failCircuit(personality, "personality query", exception);
            return Optional.empty();
        }
    }

    /** Intended for diagnostics and story selection, not per-tick polling. */
    public Set<String> traitIds(Entity entity) {
        if (!ready || !actors.available() || !traits.available() || !villagerClass.isInstance(entity)) return Set.of();
        try {
            Object traitContainer = villagerGetTraits.invoke(entity);
            Object values = traitsGetTraits.invoke(traitContainer);
            if (!(values instanceof Set<?> set)) return Set.of();
            TreeSet<String> ids = new TreeSet<>();
            for (Object value : set) {
                Object id = traitId.invoke(value);
                if (id instanceof String text && !text.isBlank()) ids.add(text);
            }
            return Collections.unmodifiableSet(ids);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            failCircuit(traits, "traits query", exception);
            return Set.of();
        }
    }

    private OptionalInt playerAffinity(Entity observer, Entity source) throws ReflectiveOperationException {
        Entity villager;
        Player player;
        if (villagerClass.isInstance(observer) && source instanceof Player sourcePlayer) {
            villager = observer;
            player = sourcePlayer;
        } else if (observer instanceof Player observerPlayer && villagerClass.isInstance(source)) {
            villager = source;
            player = observerPlayer;
        } else {
            return OptionalInt.empty();
        }
        Object brain = affinityVillagerGetBrain.invoke(villager);
        Object values = brainGetMemories.invoke(brain);
        if (!(values instanceof Map<?, ?> memories)) return OptionalInt.empty();
        Object memory = memories.get(player.getUUID());
        if (memory == null) return OptionalInt.empty();
        Object hearts = memoriesGetHearts.invoke(memory);
        return hearts instanceof Integer value ? OptionalInt.of(value) : OptionalInt.empty();
    }

    private boolean isSocialActor(Entity entity) {
        return entity instanceof Player || (villagerClass != null && villagerClass.isInstance(entity));
    }

    private static McaSocialContext context(McaRelationshipCategory relationship, OptionalInt hearts,
                                            Optional<String> observerPersonality,
                                            Optional<String> sourcePersonality, String detail) {
        return new McaSocialContext(relationship, hearts, observerPersonality, sourcePersonality, detail);
    }

    private static Object optionalValue(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : null;
    }

    private void failCircuit(CompatCapabilityCircuit circuit, String operation, Throwable exception) {
        boolean firstFailure = circuit.available();
        circuit.fail(exception);
        if (firstFailure) {
            Throwable root = rootCause(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_social] {} failed; only that social capability is disabled: {}",
                    operation, safeMessage(root));
        }
        refreshStatus();
    }

    private synchronized void reset() {
        resetMembers();
        actors.reset();
        family.reset();
        affinity.reset();
        personality.reset();
        traits.reset();
        statusDetail = "not initialized";
    }

    private void resetMembers() {
        ready = false;
        villagerClass = null;
        familyTreeGet = null;
        familyTreeGetOrEmpty = null;
        nodePartner = null;
        nodeRelationshipState = null;
        relationshipStateIsMarried = null;
        nodeIsParent = null;
        nodeSiblings = null;
        affinityVillagerGetBrain = null;
        brainGetMemories = null;
        memoriesGetHearts = null;
        configGetInstance = null;
        friendThreshold = null;
        bountyThreshold = null;
        personalityVillagerGetBrain = null;
        brainGetPersonality = null;
        villagerGetTraits = null;
        traitsGetTraits = null;
        traitId = null;
    }

    private void failAll(String reason) {
        actors.fail(reason);
        family.fail(reason);
        affinity.fail(reason);
        personality.fail(reason);
        traits.fail(reason);
        ready = false;
    }

    private void refreshStatus() {
        if (!actors.available()) ready = false;
        statusDetail = (fullyReady() ? "active: " : ready ? "partial: " : "disabled: ") + diagnosticDetail();
    }

    private static Throwable rootCause(Throwable throwable) {
        if (throwable instanceof InvocationTargetException invocation && invocation.getCause() != null) {
            return invocation.getCause();
        }
        return throwable;
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
