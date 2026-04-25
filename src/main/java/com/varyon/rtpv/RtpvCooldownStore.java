package com.varyon.rtpv;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

public final class RtpvCooldownStore {

    private static final ConcurrentHashMap<UUID, Long> LAST_SUCCESS_MS = new ConcurrentHashMap<>();

    private RtpvCooldownStore() {}

    public static int getRemainingCooldownSeconds(@Nonnull UUID uuid, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return 0;
        }
        Long last = LAST_SUCCESS_MS.get(uuid);
        if (last == null) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - last;
        long need = cooldownSeconds * 1000L;
        if (elapsed >= need) {
            return 0;
        }
        return (int) ((need - elapsed + 999) / 1000);
    }

    public static void recordSuccessfulRtpv(@Nonnull UUID uuid) {
        LAST_SUCCESS_MS.put(uuid, System.currentTimeMillis());
    }

    public static void onPlayerDisconnect(@Nonnull UUID uuid) {
        LAST_SUCCESS_MS.remove(uuid);
    }
}
