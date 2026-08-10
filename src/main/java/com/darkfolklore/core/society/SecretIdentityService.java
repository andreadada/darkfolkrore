package com.darkfolklore.core.society;

import com.darkfolklore.core.knowledge.social.*;
import com.darkfolklore.core.persistence.FolkloreSavedData;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class SecretIdentityService {
    private SecretIdentityService() {}

    /** Social interpretation hook: confirmed observers retain knowledge even when a mechanical disguise is active. */
    public static boolean canBeFooled(MinecraftServer server, UUID observer, UUID subject, SecretType secret) {
        SocialKnowledgeState state = FolkloreSavedData.get(server)
                .social(new SocialKnowledgeKey(observer, subject, secret))
                .map(SocialKnowledgeRecord::state).orElse(SocialKnowledgeState.UNKNOWN);
        return state == SocialKnowledgeState.UNKNOWN || state == SocialKnowledgeState.RUMOR;
    }
}
