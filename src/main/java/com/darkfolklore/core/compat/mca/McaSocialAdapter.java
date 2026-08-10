package com.darkfolklore.core.compat.mca;

import com.darkfolklore.core.DarkFolkloreCore;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fail-closed, exact-version bridge to MCA's read-only family, player affinity, personality and trait accessors.
 * Every reflective member is resolved once during initialization; queries never search for classes or methods.
 */
public final class McaSocialAdapter {
    public static final String MOD_ID = "mca";
    public static final String TESTED_VERSION = "7.7.32+1.21.1";

    private final AtomicBoolean queryFailureLogged = new AtomicBoolean();
    private Class<?> villagerClass;
    private Method familyTreeGet;
    private Method familyTreeGetOrEmpty;
    private Method nodePartner;
    private Method nodeRelationshipState;
    private Method relationshipStateIsMarried;
    private Method nodeIsParent;
    private Method nodeSiblings;
    private Method villagerGetBrain;
    private Method brainGetPersonality;
    private Method brainGetMemories;
    private Method memoriesGetHearts;
    private Method configGetInstance;
    private Field friendThreshold;
    private Field bountyThreshold;
    private Method villagerGetTraits;
    private Method traitsGetTraits;
    private Method traitId;
    private volatile boolean ready;
    private volatile String statusDetail = "not initialized";

    public synchronized boolean initialize(String actualVersion) {
        reset();
        if (!TESTED_VERSION.equals(actualVersion)) {
            statusDetail = "disabled: requires exact MCA " + TESTED_VERSION + ", found " + actualVersion;
            return false;
        }
        try {
            ClassLoader loader = McaSocialAdapter.class.getClassLoader();
            villagerClass = Class.forName("net.conczin.mca.entity.VillagerEntityMCA", false, loader);
            Class<?> familyTree = Class.forName("net.conczin.mca.server.world.data.FamilyTree", false, loader);
            Class<?> node = Class.forName("net.conczin.mca.server.world.data.FamilyTreeNode", false, loader);
            Class<?> relationshipState = Class.forName(
                    "net.conczin.mca.entity.ai.relationship.RelationshipState", false, loader);
            Class<?> brain = Class.forName("net.conczin.mca.entity.ai.brain.VillagerBrain", false, loader);
            Class<?> memories = Class.forName("net.conczin.mca.entity.ai.Memories", false, loader);
            Class<?> config = Class.forName("net.conczin.mca.Config", false, loader);
            Class<?> traits = Class.forName("net.conczin.mca.entity.ai.Traits", false, loader);
            Class<?> trait = Class.forName("net.conczin.mca.entity.ai.Traits$Trait", false, loader);

            familyTreeGet = familyTree.getMethod("get", ServerLevel.class);
            familyTreeGetOrEmpty = familyTree.getMethod("getOrEmpty", UUID.class);
            nodePartner = node.getMethod("partner");
            nodeRelationshipState = node.getMethod("getRelationshipState");
            relationshipStateIsMarried = relationshipState.getMethod("isMarried");
            nodeIsParent = node.getMethod("isParent", UUID.class);
            nodeSiblings = node.getMethod("siblings");
            villagerGetBrain = villagerClass.getMethod("getVillagerBrain");
            brainGetPersonality = brain.getMethod("getPersonality");
            brainGetMemories = brain.getMethod("getMemories");
            memoriesGetHearts = memories.getMethod("getHearts");
            configGetInstance = config.getMethod("getInstance");
            friendThreshold = config.getField("heartsToBeConsideredAsFriend");
            bountyThreshold = config.getField("bountyHunterHearts");
            villagerGetTraits = villagerClass.getMethod("getTraits");
            traitsGetTraits = traits.getMethod("getTraits");
            traitId = trait.getMethod("id");
            ready = true;
            statusDetail = "active: exact MCA " + TESTED_VERSION + " social bridge";
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            resetMembers();
            statusDetail = "disabled: MCA " + TESTED_VERSION + " social signature mismatch ("
                    + exception.getClass().getSimpleName() + ": " + safeMessage(exception) + ")";
            return false;
        }
    }

    public boolean isReady() {
        return ready;
    }

    public String statusDetail() {
        return statusDetail;
    }

    public McaSocialContext relationship(Entity observer, Entity source) {
        if (!ready) return McaSocialContext.unavailable(statusDetail);
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

        try {
            Object tree = familyTreeGet.invoke(null, level);
            Object observerNode = optionalValue(familyTreeGetOrEmpty.invoke(tree, observerId));
            Object sourceNode = optionalValue(familyTreeGetOrEmpty.invoke(tree, sourceId));
            if (observerNode != null && sourceNode != null) {
                Object partner = nodePartner.invoke(observerNode);
                Object relationshipState = nodeRelationshipState.invoke(observerNode);
                if (sourceId.equals(partner) && Boolean.TRUE.equals(relationshipStateIsMarried.invoke(relationshipState))) {
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

            OptionalInt affinity = playerAffinity(observer, source);
            if (affinity.isPresent()) {
                Object config = configGetInstance.invoke(null);
                int hearts = affinity.getAsInt();
                if (hearts >= friendThreshold.getInt(config)) {
                    return context(McaRelationshipCategory.PLAYER_FRIEND, affinity, observerPersonality,
                            sourcePersonality, "MCA heartsToBeConsideredAsFriend threshold");
                }
                if (hearts <= bountyThreshold.getInt(config)) {
                    return context(McaRelationshipCategory.PLAYER_BOUNTY_TARGET, affinity, observerPersonality,
                            sourcePersonality, "MCA bountyHunterHearts threshold");
                }
                return context(McaRelationshipCategory.STRANGER, affinity, observerPersonality, sourcePersonality,
                        "player affinity exists but reaches no verified MCA relationship threshold");
            }
            if (observerNode != null && sourceNode != null) {
                return context(McaRelationshipCategory.STRANGER, OptionalInt.empty(), observerPersonality,
                        sourcePersonality, "no verified MCA family relationship");
            }
            return context(McaRelationshipCategory.UNKNOWN, OptionalInt.empty(), observerPersonality,
                    sourcePersonality, "MCA family record or player affinity is absent");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnQueryOnce("relationship", exception);
            return McaSocialContext.unavailable("MCA relationship query failed: " + safeMessage(rootCause(exception)));
        }
    }

    public Optional<String> personality(Entity entity) {
        if (!ready || !villagerClass.isInstance(entity)) return Optional.empty();
        try {
            Object brain = villagerGetBrain.invoke(entity);
            Object personality = brainGetPersonality.invoke(brain);
            return personality instanceof Enum<?> value ? Optional.of(value.name()) : Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnQueryOnce("personality", exception);
            return Optional.empty();
        }
    }

    /** Intended for diagnostics and story selection, not per-tick polling. */
    public Set<String> traitIds(Entity entity) {
        if (!ready || !villagerClass.isInstance(entity)) return Set.of();
        try {
            Object traits = villagerGetTraits.invoke(entity);
            Object values = traitsGetTraits.invoke(traits);
            if (!(values instanceof Set<?> set)) return Set.of();
            TreeSet<String> ids = new TreeSet<>();
            for (Object value : set) {
                Object id = traitId.invoke(value);
                if (id instanceof String text && !text.isBlank()) ids.add(text);
            }
            return Collections.unmodifiableSet(ids);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnQueryOnce("traits", exception);
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
        Object brain = villagerGetBrain.invoke(villager);
        Object values = brainGetMemories.invoke(brain);
        if (!(values instanceof Map<?, ?> memories)) return OptionalInt.empty();
        Object memory = memories.get(player.getUUID());
        if (memory == null) return OptionalInt.empty();
        Object hearts = memoriesGetHearts.invoke(memory);
        return hearts instanceof Integer value ? OptionalInt.of(value) : OptionalInt.empty();
    }

    private boolean isSocialActor(Entity entity) {
        return entity instanceof Player || villagerClass.isInstance(entity);
    }

    private static McaSocialContext context(McaRelationshipCategory relationship, OptionalInt hearts,
                                            Optional<String> observerPersonality,
                                            Optional<String> sourcePersonality, String detail) {
        return new McaSocialContext(relationship, hearts, observerPersonality, sourcePersonality, detail);
    }

    private static Object optionalValue(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : null;
    }

    private void warnQueryOnce(String operation, Exception exception) {
        if (queryFailureLogged.compareAndSet(false, true)) {
            Throwable root = rootCause(exception);
            DarkFolkloreCore.LOGGER.warn("[compat/mca_social] {} query failed; MCA evidence will be unknown: {}",
                    operation, safeMessage(root));
        }
    }

    private synchronized void reset() {
        resetMembers();
        queryFailureLogged.set(false);
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
        villagerGetBrain = null;
        brainGetPersonality = null;
        brainGetMemories = null;
        memoriesGetHearts = null;
        configGetInstance = null;
        friendThreshold = null;
        bountyThreshold = null;
        villagerGetTraits = null;
        traitsGetTraits = null;
        traitId = null;
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
