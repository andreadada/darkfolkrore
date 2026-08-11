package com.darkfolklore.core.diagnostics;

import com.darkfolklore.core.canonical.CanonicalDefinition;
import com.darkfolklore.core.compat.*;
import com.darkfolklore.core.compat.mcacapitals.PoliticalContext;
import com.darkfolklore.core.compat.wolfsbane.WolfsbaneIntegration;
import com.darkfolklore.core.config.FolkloreConfig;
import com.darkfolklore.core.contracts.ContractAssignment;
import com.darkfolklore.core.data.FolkloreDataManager;
import com.darkfolklore.core.knowledge.lore.LoreEngine;
import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import com.darkfolklore.core.society.SecretFacts;
import com.darkfolklore.core.society.organization.*;
import com.darkfolklore.core.society.rumor.RumorEngine;
import com.darkfolklore.core.society.rumor.RumorDiagnostic;
import com.darkfolklore.core.society.story.PersistentStory;
import com.darkfolklore.core.society.village.*;
import com.darkfolklore.core.traits.TraitResolver;
import com.darkfolklore.core.world.WorldEventDirector;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.stream.Collectors;

public final class FolkloreCommands {
    private FolkloreCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("folklore");
        root.requires(source -> source.hasPermission(2));
        root.then(Commands.literal("diagnostics").executes(FolkloreCommands::diagnostics));
        root.then(Commands.literal("inspect")
                .then(Commands.argument("entity", EntityArgument.entity()).executes(FolkloreCommands::inspect)));
        root.then(Commands.literal("canonical")
                .then(Commands.argument("concept", StringArgumentType.greedyString()).executes(FolkloreCommands::canonical)));

        LiteralArgumentBuilder<CommandSourceStack> knowledge = Commands.literal("knowledge");
        knowledge.then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("concept", ResourceLocationArgument.id()).executes(FolkloreCommands::knowledgeGet))));
        knowledge.then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("concept", ResourceLocationArgument.id())
                        .then(Commands.argument("points", IntegerArgumentType.integer(1, 100))
                                .executes(FolkloreCommands::knowledgeGrant)))));
        root.then(knowledge);

        LiteralArgumentBuilder<CommandSourceStack> social = Commands.literal("social");
        social.then(Commands.literal("get").then(Commands.argument("observer", EntityArgument.entity())
                .then(Commands.argument("subject", EntityArgument.entity())
                        .then(Commands.argument("secret", StringArgumentType.word()).executes(FolkloreCommands::socialGet)))));
        social.then(Commands.literal("set").then(Commands.argument("observer", EntityArgument.entity())
                .then(Commands.argument("subject", EntityArgument.entity())
                        .then(Commands.argument("secret", StringArgumentType.word())
                                .then(Commands.argument("state", StringArgumentType.word())
                                        .then(Commands.argument("confidence", FloatArgumentType.floatArg(0, 1))
                                                .executes(FolkloreCommands::socialSet)))))));
        social.then(Commands.literal("inspect")
                .then(Commands.argument("entity", EntityArgument.entity()).executes(FolkloreCommands::socialInspect)));
        root.then(social);

        root.then(Commands.literal("rumor")
                .then(Commands.literal("inspect").executes(FolkloreCommands::rumorInspect)));

        root.then(Commands.literal("fieldguide")
                .then(Commands.literal("diagnostics").executes(FolkloreCommands::fieldGuideDiagnostics)));

        root.then(Commands.literal("capitals")
                .then(Commands.literal("inspect")
                        .then(Commands.argument("entity", EntityArgument.entity())
                                .executes(FolkloreCommands::capitalsInspect))));

        LiteralArgumentBuilder<CommandSourceStack> organization = Commands.literal("organization");
        organization.then(Commands.literal("list").executes(FolkloreCommands::organizations));
        organization.then(Commands.literal("create").then(Commands.argument("type", StringArgumentType.word())
                .then(Commands.argument("leader", EntityArgument.entity())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(FolkloreCommands::organizationCreate)))));
        organization.then(Commands.literal("inspect")
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(FolkloreCommands::organizationInspect)));
        root.then(organization);
        LiteralArgumentBuilder<CommandSourceStack> village = Commands.literal("village")
                .executes(FolkloreCommands::village);
        village.then(Commands.literal("inspect").executes(FolkloreCommands::village));
        root.then(village);
        root.then(Commands.literal("story")
                .then(Commands.literal("list").executes(FolkloreCommands::stories)));
        root.then(Commands.literal("stories").executes(FolkloreCommands::stories));
        root.then(Commands.literal("contracts").executes(FolkloreCommands::contracts));
        dispatcher.register(root);
    }

    private static int diagnostics(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        send(source, "Dark Folklore Core | MC 1.21.1 | NeoForge target 21.1.248 | schema "
                + FolkloreSavedData.SCHEMA_VERSION);
        for (CompatibilityReport report : CompatibilityManager.INSTANCE.reports()) {
            send(source, report.displayName() + " tested=" + report.testedVersion() + " actual="
                    + report.actualVersion() + " status=" + report.status() + " via " + report.mechanism());
        }
        send(source, "Data: canonical=" + FolkloreDataManager.INSTANCE.canonical().definitions().size()
                + " weaknesses=" + FolkloreDataManager.INSTANCE.weaknesses().rules().size()
                + " spawns=" + FolkloreDataManager.INSTANCE.spawns().profiles().size()
                + " magic=" + FolkloreDataManager.INSTANCE.magic().size()
                + " investigationProfiles=" + FolkloreDataManager.INSTANCE.investigationProfiles().size()
                + " storyTemplates=" + FolkloreDataManager.INSTANCE.storyTemplates().size()
                + " invalid=" + FolkloreDataManager.INSTANCE.validationErrors().size());
        send(source, "Rumor queue=" + RumorEngine.INSTANCE.queued());
        var wolfsbane = WolfsbaneIntegration.snapshot();
        send(source, "Wolfsbane=" + wolfsbane.state() + " werewolves="
                + wolfsbane.actualWerewolvesVersion() + " enchanted=" + wolfsbane.actualEnchantedVersion()
                + " diffuser=" + wolfsbane.diffuserFuelBridge() + " contact="
                + wolfsbane.canonicalCropContactEffect() + " finder=" + wolfsbane.finderCropLocator()
                + " detail=" + wolfsbane.detail());
        if (source.getLevel() != null) send(source, "World events=" + WorldEventDirector.INSTANCE.active(source.getLevel()));
        return 1;
    }

    private static int inspect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "entity");
        FolkloreSavedData data = FolkloreSavedData.get(context.getSource().getServer());
        String registry = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        send(context.getSource(), "Inspect " + entity.getName().getString() + " " + entity.getUUID());
        String staticCanonical = FolkloreDataManager.INSTANCE.canonical().resolve(registry)
                .map(CanonicalDefinition::concept).orElse("unmapped");
        Set<SecretType> facts = SecretFacts.actualSecrets(entity);
        String semanticCanonical = facts.isEmpty() ? staticCanonical : SecretFacts.canonicalConcept(entity);
        CompatibilityManager compatibility = CompatibilityManager.INSTANCE;
        send(context.getSource(), "REGISTRY: " + registry);
        send(context.getSource(), "CANONICAL CREATURE: static=" + staticCanonical + " semantic=" + semanticCanonical);
        send(context.getSource(), "STATIC TRAITS: " + TraitResolver.staticCreatureTraits(entity));
        send(context.getSource(), "DYNAMIC FACTS: " + facts);
        send(context.getSource(), "SUPERNATURAL PROVIDER: vampire=" + compatibility.isVampire(entity)
                + " werewolf=" + compatibility.isWerewolf(entity) + " hunter=" + compatibility.isHunter(entity));
        if (!semanticCanonical.equals("unmapped")) {
            var lore = data.lore(entity.getUUID(), semanticCanonical);
            send(context.getSource(), "LORE: " + semanticCanonical + "=" + lore.points() + " " + lore.stage());
        }
        Map<SocialKnowledgeState, Long> counts = data.knowledgeAbout(entity.getUUID()).stream()
                .collect(Collectors.groupingBy(entry -> entry.getValue().state(), Collectors.counting()));
        send(context.getSource(), "SOCIAL KNOWLEDGE SUMMARY: knownBy=" + counts
                + " public=" + data.publicSecrets().keySet().stream().filter(key -> key.subject().equals(entity.getUUID())).toList());
        List<String> orgs = data.organizationsForMember(entity.getUUID()).stream().map(data::organization)
                .flatMap(Optional::stream)
                .map(Organization::name).toList();
        send(context.getSource(), "ORGANIZATION: " + orgs);
        send(context.getSource(), "LINEAGE: " + data.lineage(entity.getUUID()).orElse(null));
        PoliticalContext political = compatibility.mcaCapitals().politicalContext(context.getSource().getLevel(),
                entity.getUUID());
        send(context.getSource(), "POLITICAL ROLE: " + political.role() + " title="
                + political.exactTitle().orElse("-") + " status=" + political.status());
        return 1;
    }

    private static int canonical(CommandContext<CommandSourceStack> context) {
        String concept = StringArgumentType.getString(context, "concept");
        if (!concept.contains(":")) concept = "darkfolklore:" + concept;
        CanonicalDefinition definition = FolkloreDataManager.INSTANCE.canonical().concept(concept).orElse(null);
        if (definition == null) { send(context.getSource(), "Unknown canonical concept " + concept); return 0; }
        send(context.getSource(), definition.concept() + " kind=" + definition.kind() + " canonical="
                + definition.canonicalId() + " policy=" + definition.policy());
        send(context.getSource(), "Implementations=" + definition.implementations());
        send(context.getSource(), "Reason=" + definition.reason());
        if (definition.concept().equals("darkfolklore:wolfsbane")) {
            var status = WolfsbaneIntegration.snapshot();
            send(context.getSource(), "Runtime integration=" + status.state() + " diffuser="
                    + status.diffuserFuelBridge() + " contact=" + status.canonicalCropContactEffect()
                    + " finder=" + status.finderCropLocator() + " detail=" + status.detail());
        }
        return 1;
    }

    private static int knowledgeGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String concept = ResourceLocationArgument.getId(context, "concept").toString();
        var progress = FolkloreSavedData.get(context.getSource().getServer()).lore(player.getUUID(), concept);
        send(context.getSource(), player.getName().getString() + " " + concept + "=" + progress.points() + " " + progress.stage());
        return progress.points();
    }

    private static int knowledgeGrant(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String concept = ResourceLocationArgument.getId(context, "concept").toString();
        int points = IntegerArgumentType.getInteger(context, "points");
        var progress = LoreEngine.INSTANCE.grant(player, concept, points);
        send(context.getSource(), "Granted; now " + progress.points() + " " + progress.stage());
        return progress.points();
    }

    private static int socialGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity observer = EntityArgument.getEntity(context, "observer");
        Entity subject = EntityArgument.getEntity(context, "subject");
        SecretType secret = parse(SecretType.class, StringArgumentType.getString(context, "secret"));
        var value = FolkloreSavedData.get(context.getSource().getServer())
                .social(new SocialKnowledgeKey(observer.getUUID(), subject.getUUID(), secret));
        send(context.getSource(), value.map(Object::toString).orElse("UNKNOWN"));
        return value.map(record -> record.state().strength()).orElse(0);
    }

    private static int socialSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity observer = EntityArgument.getEntity(context, "observer");
        Entity subject = EntityArgument.getEntity(context, "subject");
        SecretType secret = parse(SecretType.class, StringArgumentType.getString(context, "secret"));
        SocialKnowledgeState state = parse(SocialKnowledgeState.class, StringArgumentType.getString(context, "state"));
        float confidence = FloatArgumentType.getFloat(context, "confidence");
        long now = context.getSource().getLevel().getGameTime();
        var value = FolkloreSavedData.get(context.getSource().getServer()).mergeSocial(
                new SocialKnowledgeKey(observer.getUUID(), subject.getUUID(), secret),
                new SocialKnowledgeRecord(state, confidence, KnowledgeSource.ADMIN, now, null));
        send(context.getSource(), "Social knowledge=" + value);
        return value.state().strength();
    }

    private static int socialInspect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "entity");
        FolkloreSavedData data = FolkloreSavedData.get(context.getSource().getServer());
        List<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> held = data.knowledgeHeldBy(entity.getUUID());
        List<Map.Entry<SocialKnowledgeKey, SocialKnowledgeRecord>> about = data.knowledgeAbout(entity.getUUID());
        send(context.getSource(), "Social inspect " + entity.getName().getString() + " " + entity.getUUID()
                + " held=" + held.size() + " knownBy=" + about.size());
        held.stream().sorted(Comparator.comparingLong(entry -> -entry.getValue().gameTime())).limit(12)
                .forEach(entry -> send(context.getSource(), "holds subject=" + entry.getKey().subject()
                        + " secret=" + entry.getKey().secret() + " state=" + entry.getValue().state()
                        + " confidence=" + entry.getValue().confidence() + " source=" + entry.getValue().source()
                        + " familyReaction=" + data.familyReaction(entry.getKey()).orElse(null)));
        return held.size();
    }

    private static int rumorInspect(CommandContext<CommandSourceStack> context) {
        List<RumorDiagnostic> values = RumorEngine.INSTANCE.diagnostics();
        send(context.getSource(), "Transient rumor diagnostics retained=" + values.size()
                + " queued=" + RumorEngine.INSTANCE.queued());
        int start = Math.max(0, values.size() - 10);
        values.subList(start, values.size()).reversed().forEach(value -> {
            send(context.getSource(), "t=" + value.gameTime() + " sender=" + value.sender()
                    + " recipient=" + value.recipient() + " subject=" + value.subject() + " claim="
                    + value.secret() + " trust=" + value.trust() + " result=" + value.resultingConfidence()
                    + " outcome=" + value.outcome());
            value.contributions().forEach(reason -> send(context.getSource(), "  " + reason.reason()
                    + " " + (reason.amount() >= 0 ? "+" : "") + reason.amount()));
        });
        return values.size();
    }

    private static int fieldGuideDiagnostics(CommandContext<CommandSourceStack> context) {
        CompatibilityReport report = CompatibilityManager.INSTANCE.report("fieldguide").orElse(null);
        int categories = context.getSource().getServer().getResourceManager()
                .listResources("fieldguide/categories", id -> id.getNamespace().equals("darkfolklore")).size();
        send(context.getSource(), "Field Guide compat=" + (report == null ? "not initialized" : report.status())
                + " tested=1.14.0 actual=" + (report == null ? "-" : report.actualVersion()));
        send(context.getSource(), "Dark Folklore categories=" + categories
                + " expectedEntries=9 localization=en_us+it_it progress=binary server bridge");
        send(context.getSource(), "Startup validator contract: nonempty categories, exact translation keys, "
                + "canonical targets, audited icons, SCAN+KILL triggers");
        return categories;
    }

    private static int capitalsInspect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "entity");
        PoliticalContext political = CompatibilityManager.INSTANCE.mcaCapitals()
                .politicalContext(context.getSource().getLevel(), entity.getUUID());
        send(context.getSource(), "MCA Capitals status=" + political.status() + " role=" + political.role()
                + " title=" + political.exactTitle().orElse("-") + " capital="
                + political.capitalId().map(UUID::toString).orElse("-") + " villageId="
                + (political.mcaVillageId().isPresent() ? political.mcaVillageId().getAsInt() : "-")
                + " state=" + political.capitalState().orElse("-") + " detail=" + political.detail());
        return political.role().ordinal();
    }

    private static int organizations(CommandContext<CommandSourceStack> context) {
        Collection<Organization> organizations = FolkloreSavedData.get(context.getSource().getServer()).organizations();
        send(context.getSource(), "Organizations: " + organizations.size());
        organizations.forEach(org -> send(context.getSource(), org.id() + " " + org.type() + " '" + org.name()
                + "' leader=" + org.leader() + " members=" + org.members().size() + " home=" + org.home()));
        return organizations.size();
    }

    private static int organizationCreate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        OrganizationType type = parse(OrganizationType.class, StringArgumentType.getString(context, "type"));
        Entity leader = EntityArgument.getEntity(context, "leader");
        String name = StringArgumentType.getString(context, "name");
        Organization organization = new Organization(UUID.randomUUID(), type, name, leader.getUUID());
        if (leader.level() instanceof ServerLevel level) organization.setHome(VillageKey.at(level, leader.blockPosition()).serialized());
        organization.addEvent(OrganizationEvent.of(OrganizationEventType.FOUNDED,
                context.getSource().getLevel().getGameTime(), leader.getUUID(), null, "created by administrator"));
        if (!FolkloreSavedData.get(context.getSource().getServer())
                .tryPutOrganization(organization, FolkloreConfig.MAX_ORGANIZATIONS.get())) {
            send(context.getSource(), "Organization cap reached; creation rejected");
            return 0;
        }
        send(context.getSource(), "Created " + organization.id());
        return 1;
    }

    private static int organizationInspect(CommandContext<CommandSourceStack> context) {
        UUID id;
        try {
            id = UUID.fromString(StringArgumentType.getString(context, "id"));
        } catch (IllegalArgumentException exception) {
            send(context.getSource(), "Invalid organization UUID");
            return 0;
        }
        Organization organization = FolkloreSavedData.get(context.getSource().getServer())
                .organization(id).orElse(null);
        if (organization == null) {
            send(context.getSource(), "Unknown organization " + id);
            return 0;
        }
        send(context.getSource(), organization.id() + " " + organization.type() + " '" + organization.name()
                + "' leader=" + organization.leader() + " home=" + organization.home()
                + " influence=" + organization.influence());
        send(context.getSource(), "members=" + organization.members());
        send(context.getSource(), "memberLastSeen=" + organization.memberLastSeen() + " dormant="
                + organization.dormantMembers(context.getSource().getLevel().getGameTime(), 168000L));
        send(context.getSource(), "objectives=" + organization.objectives() + " relations=" + organization.relations());
        send(context.getSource(), "intelligence=" + organization.intelligence().size()
                + " events=" + organization.events().size());
        organization.events().reversed().stream().limit(10).forEach(event -> send(context.getSource(),
                "  t=" + event.gameTime() + " " + event.type() + " actor=" + event.actor().orElse(null)
                        + " subject=" + event.subject().orElse(null) + " " + event.detail()));
        return organization.members().size();
    }

    private static int village(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        VillageKey key = VillageKey.at(player.serverLevel(), player.blockPosition());
        VillageSocietyState value = FolkloreSavedData.get(context.getSource().getServer()).village(key.serialized());
        send(context.getSource(), key.serialized() + " awareness=" + value.publicAwareness() + " suspicion="
                + value.suspicion() + " fear=" + value.fear() + " political=" + value.politicalImportance()
                + " influences[v=" + value.vampireInfluence()
                + ",h=" + value.hunterInfluence() + ",w=" + value.werewolfInfluence() + ",witch=" + value.witchInfluence() + "]");
        return value.suspicion();
    }

    private static int stories(CommandContext<CommandSourceStack> context) {
        Collection<PersistentStory> values = FolkloreSavedData.get(context.getSource().getServer()).stories();
        values.forEach(value -> send(context.getSource(), value.story().id() + " " + value.story().template()
                + " " + value.story().concept() + " " + value.story().status() + " actors=" + value.story().actors()));
        return values.size();
    }

    private static int contracts(CommandContext<CommandSourceStack> context) {
        Collection<ContractAssignment> values = FolkloreSavedData.get(context.getSource().getServer()).contracts();
        values.forEach(value -> send(context.getSource(), value.contract().id() + " player=" + value.player()
                + " target=" + value.contract().targetConcept() + " status=" + value.contract().status()
                + " clues=" + value.contract().evidence()));
        return values.size();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    private static void send(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
