package com.varyon.rtpv;

import com.varyon.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RtpvJoinManager {
    private static RtpvJoinManager instance;

    private final Map<UUID, JoinableEntry> joinableByUuid = new ConcurrentHashMap<>();

    public static void setInstance(@Nullable RtpvJoinManager manager) {
        instance = manager;
    }

    @Nullable
    public static RtpvJoinManager getInstance() {
        return instance;
    }

    public void markJoinable(@Nonnull UUID playerUuid, int zoneId, @Nonnull String worldName, long expireAtMillis) {
        joinableByUuid.put(playerUuid, new JoinableEntry(zoneId, worldName, expireAtMillis));
    }

    public void onPlayerDisconnect(@Nonnull UUID playerUuid) {
        joinableByUuid.remove(playerUuid);
    }

    @Nullable
    public JoinableEntry getJoinable(@Nonnull UUID targetUuid, @Nonnull String targetWorldName,
                                     @Nonnull ZoneConfig zoneConfig) {
        JoinableEntry entry = joinableByUuid.get(targetUuid);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireAtMillis) {
            joinableByUuid.remove(targetUuid);
            return null;
        }
        if (!targetWorldName.equals(entry.worldName)) return null;
        if (zoneConfig.getZoneIdForInstanceWorld(targetWorldName) != null) return null;
        return entry;
    }

    public record JoinableEntry(int zoneId, String worldName, long expireAtMillis) {}
}
