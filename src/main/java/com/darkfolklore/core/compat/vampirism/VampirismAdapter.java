package com.darkfolklore.core.compat.vampirism;

import com.darkfolklore.core.DarkFolkloreCore;
import com.darkfolklore.core.compat.FactResult;
import com.darkfolklore.core.compat.SupernaturalStateAdapter;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Typed bridge using Vampirism's documented public faction API. */
public final class VampirismAdapter implements SupernaturalStateAdapter {
    private static final ResourceLocation VAMPIRE = ResourceLocation.fromNamespaceAndPath("vampirism", "vampire");
    private static final ResourceLocation HUNTER = ResourceLocation.fromNamespaceAndPath("vampirism", "hunter");
    private static final ResourceLocation WEREWOLF = ResourceLocation.fromNamespaceAndPath("werewolves", "werewolf");
    private final boolean werewolvesEnabled;

    public VampirismAdapter(boolean werewolvesEnabled) {
        this.werewolvesEnabled = werewolvesEnabled;
    }

    @Override
    public String modId() { return "vampirism"; }

    @Override
    public FactResult isVampire(Entity entity) { return faction(entity, VAMPIRE); }

    @Override
    public FactResult isWerewolf(Entity entity) {
        return werewolvesEnabled ? faction(entity, WEREWOLF) : FactResult.NOT_APPLICABLE;
    }

    @Override
    public FactResult isHunter(Entity entity) { return faction(entity, HUNTER); }

    private static FactResult faction(Entity entity, ResourceLocation expected) {
        try {
            IFaction<?> faction = VampirismAPI.factionRegistry().getFaction(entity);
            return FactResult.of(faction != null && expected.equals(faction.getID()));
        } catch (RuntimeException | LinkageError exception) {
            DarkFolkloreCore.LOGGER.warn("[compat/vampirism] Public faction query failed; returning unknown", exception);
            return FactResult.UNKNOWN;
        }
    }
}
