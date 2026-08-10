package com.darkfolklore.core.society.organization;

import com.darkfolklore.core.knowledge.social.SocialKnowledgeState;

import java.util.*;

public final class Organization {
    public static final int MAX_EVENT_HISTORY = 64;
    public static final int MAX_INTELLIGENCE = 256;
    public static final int MAX_MEMBERS_HARD = 256;
    public static final int MAX_RELATIONS = 1024;

    private final UUID id;
    private final OrganizationType type;
    private final String name;
    private UUID leader;
    private final LinkedHashSet<UUID> members = new LinkedHashSet<>();
    private final LinkedHashMap<UUID, Long> memberLastSeen = new LinkedHashMap<>();
    private String home = "";
    private int influence;
    private final EnumSet<OrganizationObjective> objectives = EnumSet.noneOf(OrganizationObjective.class);
    private final LinkedHashMap<OrganizationIntelKey, SocialKnowledgeState> intelligence = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, OrganizationRelation> relations = new LinkedHashMap<>();
    private final ArrayDeque<OrganizationEvent> events = new ArrayDeque<>();

    public Organization(UUID id, OrganizationType type, String name, UUID leader) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        this.name = name.trim();
        this.leader = Objects.requireNonNull(leader, "leader");
        members.add(leader);
        memberLastSeen.put(leader, 0L);
        objectives.addAll(defaultObjectives(type));
    }

    public UUID id() { return id; }
    public OrganizationType type() { return type; }
    public String name() { return name; }
    public UUID leader() { return leader; }
    public Set<UUID> members() { return Set.copyOf(members); }
    public Map<UUID, Long> memberLastSeen() { return Map.copyOf(memberLastSeen); }
    public String home() { return home; }
    public int influence() { return influence; }
    public Set<OrganizationObjective> objectives() { return Set.copyOf(objectives); }
    public Map<OrganizationIntelKey, SocialKnowledgeState> intelligence() { return Map.copyOf(intelligence); }
    public Map<UUID, OrganizationRelation> relations() { return Map.copyOf(relations); }
    public List<OrganizationEvent> events() { return List.copyOf(events); }

    public boolean addMember(UUID member) {
        Objects.requireNonNull(member);
        boolean added = (members.contains(member) || members.size() < MAX_MEMBERS_HARD) && members.add(member);
        if (added) memberLastSeen.putIfAbsent(member, 0L);
        return added;
    }

    public boolean removeMember(UUID member) {
        if (leader.equals(member)) return false;
        boolean removed = members.remove(member);
        if (removed) memberLastSeen.remove(member);
        return removed;
    }

    public void setLeader(UUID leader) {
        this.leader = Objects.requireNonNull(leader);
        members.add(leader);
        memberLastSeen.putIfAbsent(leader, 0L);
    }

    public void setHome(String home) { this.home = Objects.requireNonNullElse(home, ""); }
    public void setInfluence(int influence) { this.influence = Math.max(0, Math.min(100, influence)); }

    public boolean markMemberSeen(UUID member, long gameTime) {
        if (!members.contains(member)) return false;
        long normalized = Math.max(0L, gameTime);
        Long previous = memberLastSeen.put(member, normalized);
        return previous == null || previous != normalized;
    }

    public Set<UUID> dormantMembers(long gameTime, long missingAfterTicks) {
        long threshold = Math.max(1L, missingAfterTicks);
        LinkedHashSet<UUID> result = new LinkedHashSet<>();
        memberLastSeen.forEach((member, seen) -> {
            if (seen > 0L && gameTime - seen > threshold) result.add(member);
        });
        return Set.copyOf(result);
    }

    public void restoreMemberLastSeen(Map<UUID, Long> restored) {
        if (restored == null) return;
        restored.forEach((member, seen) -> {
            if (members.contains(member)) memberLastSeen.put(member, Math.max(0L, seen));
        });
    }

    public boolean addObjective(OrganizationObjective objective) {
        return objectives.add(Objects.requireNonNull(objective));
    }

    public void setRelation(UUID organization, OrganizationRelation relation) {
        if (id.equals(organization)) return;
        if (!relations.containsKey(organization) && relations.size() >= MAX_RELATIONS) return;
        relations.put(Objects.requireNonNull(organization), Objects.requireNonNull(relation));
    }

    public boolean removeRelation(UUID organization) {
        return relations.remove(organization) != null;
    }

    public boolean recordIntelligence(OrganizationIntelKey key, SocialKnowledgeState state) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(state);
        SocialKnowledgeState previous = intelligence.get(key);
        if (previous != null && previous.strength() >= state.strength()) return false;
        if (previous == null && intelligence.size() >= MAX_INTELLIGENCE) {
            Iterator<Map.Entry<OrganizationIntelKey, SocialKnowledgeState>> iterator = intelligence.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue().strength() <= SocialKnowledgeState.RUMOR.strength()) {
                    iterator.remove();
                    break;
                }
            }
            if (intelligence.size() >= MAX_INTELLIGENCE) return false;
        }
        intelligence.put(key, state);
        return true;
    }

    public void addEvent(OrganizationEvent event) {
        events.addLast(Objects.requireNonNull(event));
        while (events.size() > MAX_EVENT_HISTORY) events.removeFirst();
    }

    public void restoreObjectives(Collection<OrganizationObjective> restored) {
        if (restored == null || restored.isEmpty()) return;
        objectives.clear();
        objectives.addAll(restored);
    }

    public void restoreIntelligence(Map<OrganizationIntelKey, SocialKnowledgeState> restored) {
        intelligence.clear();
        if (restored == null) return;
        restored.entrySet().stream().limit(MAX_INTELLIGENCE)
                .forEach(entry -> intelligence.put(entry.getKey(), entry.getValue()));
    }

    public void restoreRelations(Map<UUID, OrganizationRelation> restored) {
        relations.clear();
        if (restored != null) restored.entrySet().stream().limit(MAX_RELATIONS)
                .forEach(entry -> relations.put(entry.getKey(), entry.getValue()));
    }

    public void restoreEvents(Collection<OrganizationEvent> restored) {
        events.clear();
        if (restored == null) return;
        restored.stream().skip(Math.max(0, restored.size() - MAX_EVENT_HISTORY)).forEach(events::addLast);
    }

    private static Set<OrganizationObjective> defaultObjectives(OrganizationType type) {
        return switch (type) {
            case HUNTER_SOCIETY -> EnumSet.of(OrganizationObjective.PROTECT_COMMUNITY,
                    OrganizationObjective.INVESTIGATE_SUPERNATURAL);
            case VAMPIRE_COVEN -> EnumSet.of(OrganizationObjective.PROTECT_MEMBERS,
                    OrganizationObjective.PRESERVE_SECRETS, OrganizationObjective.GROW_INFLUENCE);
            case WEREWOLF_PACK -> EnumSet.of(OrganizationObjective.PROTECT_MEMBERS,
                    OrganizationObjective.DEFEND_TERRITORY);
            case WITCH_COVEN -> EnumSet.of(OrganizationObjective.PROTECT_MEMBERS,
                    OrganizationObjective.STUDY_OCCULT);
        };
    }
}
