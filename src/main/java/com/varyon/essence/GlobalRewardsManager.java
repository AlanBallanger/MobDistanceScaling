package com.varyon.essence;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.FactionRewardsConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.faction.FactionManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GlobalRewardsManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FactionRewardsConfig config;
    private final EssenceManager essenceManager;
    private final FactionManager factionManager;
    private ZonePermissionsConfig zonePermsConfig;
    private final PendingRewardsStore pendingStore;

    /**
     * Participation tracking per faction since last tier trigger.
     * Key = player UUID, Value = essence deposited this cycle.
     */
    private final Map<UUID, Double> fractureParticipation = new ConcurrentHashMap<>();
    private final Map<UUID, Double> noyauParticipation    = new ConcurrentHashMap<>();

    private static class TierState {
        private boolean positiveRewarded = false;
        private boolean negativeRewarded = false;
        private long positiveLastRewardTime = 0;
        private long negativeLastRewardTime = 0;

        boolean canRewardPositive(long cooldownMs) {
            return !positiveRewarded || (System.currentTimeMillis() - positiveLastRewardTime >= cooldownMs);
        }

        boolean canRewardNegative(long cooldownMs) {
            return !negativeRewarded || (System.currentTimeMillis() - negativeLastRewardTime >= cooldownMs);
        }

        void markPositiveRewarded() { positiveRewarded = true; positiveLastRewardTime = System.currentTimeMillis(); }
        void markNegativeRewarded() { negativeRewarded = true; negativeLastRewardTime = System.currentTimeMillis(); }
        void resetPositive()        { positiveRewarded = false; }
        void resetNegative()        { negativeRewarded = false; }
    }

    private final Map<Integer, TierState> tierStates = new ConcurrentHashMap<>();

    public GlobalRewardsManager(@Nonnull FactionRewardsConfig config,
                                @Nonnull EssenceManager essenceManager,
                                @Nonnull FactionManager factionManager,
                                @Nonnull ZonePermissionsConfig zonePermsConfig,
                                @Nonnull Path dataFolder) {
        this.config = config;
        this.essenceManager = essenceManager;
        this.factionManager = factionManager;
        this.zonePermsConfig = zonePermsConfig;
        this.pendingStore = new PendingRewardsStore(dataFolder);

        for (int i = 0; i < config.getTiers().size(); i++) {
            tierStates.put(i, new TierState());
        }
    }

    public synchronized void applyReloadedConfigs(@Nonnull FactionRewardsConfig factionRewards,
                                                  @Nonnull ZonePermissionsConfig zonePerms) {
        this.config = factionRewards;
        this.zonePermsConfig = zonePerms;
        int n = config.getTiers().size();
        for (int i = 0; i < n; i++) {
            tierStates.computeIfAbsent(i, k -> new TierState());
        }
        tierStates.keySet().removeIf(k -> k >= n);
    }

    /**
     * Called when a player deposits essence for their faction.
     */
    public void recordDeposit(@Nonnull UUID uuid, @Nonnull FactionManager.Faction faction, double amount) {
        Map<UUID, Double> map = faction == FactionManager.Faction.FRACTURE ? fractureParticipation : noyauParticipation;
        map.merge(uuid, amount, Double::sum);
    }

    public void checkAndDistributeRewards() {
        int globalBalance = essenceManager.getGlobalBalance();
        long cooldownMs = config.getCooldownMinutes() * 60_000L;

        for (int i = 0; i < config.getTiers().size(); i++) {
            FactionRewardsConfig.RewardTier tier = config.getTiers().get(i);
            TierState state = tierStates.get(i);

            if (globalBalance >= tier.getThreshold() && state.canRewardPositive(cooldownMs)) {
                LOGGER.at(Level.INFO).log("Tier " + (i + 1) + " reached (+" + tier.getThreshold() + ") — rewarding Fracture");
                distributeFactionReward(FactionManager.Faction.FRACTURE, tier, i + 1);
                state.markPositiveRewarded();
            }

            if (globalBalance <= -tier.getThreshold() && state.canRewardNegative(cooldownMs)) {
                LOGGER.at(Level.INFO).log("Tier " + (i + 1) + " reached (-" + tier.getThreshold() + ") — rewarding Noyau");
                distributeFactionReward(FactionManager.Faction.NOYAU, tier, i + 1);
                state.markNegativeRewarded();
            }
        }
    }

    private void distributeFactionReward(@Nonnull FactionManager.Faction faction,
                                         @Nonnull FactionRewardsConfig.RewardTier tier,
                                         int tierNumber) {
        Map<UUID, Double> participation = faction == FactionManager.Faction.FRACTURE
            ? fractureParticipation : noyauParticipation;
        double minPart   = config.getMinParticipationEssence();
        double passRate  = config.getPassiveRewardRate();
        int    fullAmt   = tier.getFragmentAmount();

        Set<UUID> onlineUuids = new java.util.HashSet<>();

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef == null || !playerRef.getReference().isValid()) continue;
            UUID uuid = playerRef.getUuid();

            Ref ref = playerRef.getReference();
            Store store = ref.getStore();
            Player onlinePlayer = (Player) store.getComponent(ref, Player.getComponentType());
            if (onlinePlayer == null) continue;
            FactionManager.Faction playerFaction = factionManager.getFaction(onlinePlayer);
            if (playerFaction != faction) continue;

            onlineUuids.add(uuid);

            double deposited = participation.getOrDefault(uuid, 0.0);
            boolean participated = deposited >= minPart;
            int fragments = participated ? fullAmt : (int) Math.floor(fullAmt * passRate);
            if (fragments <= 0) continue;

            try {
                World world = ((EntityStore) store.getExternalData()).getWorld();
                final int finalFragments = fragments;
                final boolean finalParticipated = participated;
                world.execute(() -> giveFragmentsOnline(playerRef, ref, store, finalFragments, finalParticipated, tierNumber, faction));
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to schedule reward for " + playerRef.getUsername() + ": " + e.getMessage());
            }
        }

        // Offline players who participated → save pending
        for (Map.Entry<UUID, Double> entry : participation.entrySet()) {
            UUID uuid = entry.getKey();
            if (onlineUuids.contains(uuid)) continue;
            if (entry.getValue() < minPart) continue;

            pendingStore.add(uuid, fullAmt);
            LOGGER.at(Level.INFO).log("Stored " + fullAmt + " pending fragments for offline player " + uuid + " (tier " + tierNumber + ")");
        }

        // Reset participation for this faction
        participation.clear();
        LOGGER.at(Level.INFO).log("Participation reset for " + faction.getDisplayName() + " after tier " + tierNumber);
    }

    private void giveFragmentsOnline(@Nonnull PlayerRef playerRef, @Nonnull Ref ref,
                                     @Nonnull Store store, int fragments,
                                     boolean participated, int tierNumber,
                                     @Nonnull FactionManager.Faction faction) {
        try {
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null || playerComponent.getInventory() == null) return;

            int maxZone = zonePermsConfig.getMaxAccessibleZone(playerComponent);
            if (maxZone <= 0) maxZone = 1;
            String itemId = "Key_Fragment" + maxZone;

            ItemStack stack = new ItemStack(itemId, fragments);
            ItemStackTransaction tx = playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(stack);

            String factionColor = faction == FactionManager.Faction.NOYAU ? "#5555FF" : "#FF8800";
            playerRef.sendMessage(Message.raw("[Palier " + tierNumber + "] " + faction.getDisplayName() + " a atteint un seuil!").color(Color.decode(factionColor)));

            if (ItemStack.isEmpty(tx.getRemainder())) {
                String pct = participated ? "100%" : ((int)(config.getPassiveRewardRate() * 100)) + "%";
                playerRef.sendMessage(Message.raw("Vous recevez: " + fragments + "x " + itemId + " (" + pct + ")").color(Color.GREEN));
                LOGGER.at(Level.INFO).log("Gave " + fragments + "x " + itemId + " to " + playerRef.getUsername() + " (" + pct + ", tier " + tierNumber + ")");
            } else {
                LOGGER.at(Level.WARNING).log("Inventory full for " + playerRef.getUsername() + " — could not give all fragments");
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Error giving fragments to " + playerRef.getUsername() + ": " + e.getMessage());
        }
    }

    /**
     * Called when a player connects. Delivers any pending fragments.
     */
    public void onPlayerReady(@Nonnull PlayerRef playerRef, @Nonnull Ref ref, @Nonnull Store store) {
        UUID uuid = playerRef.getUuid();
        if (!pendingStore.has(uuid)) return;

        int fragments = pendingStore.get(uuid);
        pendingStore.clear(uuid);

        try {
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null || playerComponent.getInventory() == null) {
                // Restore in case delivery failed
                pendingStore.add(uuid, fragments);
                return;
            }

            int maxZone = zonePermsConfig.getMaxAccessibleZone(playerComponent);
            if (maxZone <= 0) maxZone = 1;
            String itemId = "Key_Fragment" + maxZone;

            ItemStack stack = new ItemStack(itemId, fragments);
            ItemStackTransaction tx = playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(stack);

            if (ItemStack.isEmpty(tx.getRemainder())) {
                playerRef.sendMessage(Message.raw("[Récompense en attente] Vous recevez: " + fragments + "x " + itemId).color(Color.GREEN));
                LOGGER.at(Level.INFO).log("Delivered " + fragments + "x " + itemId + " (pending) to " + playerRef.getUsername());
            } else {
                // Inventory full — restore pending
                pendingStore.add(uuid, fragments);
                playerRef.sendMessage(Message.raw("[Récompense en attente] Inventaire plein — réessayez plus tard.").color(Color.YELLOW));
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Error delivering pending rewards to " + playerRef.getUsername() + ": " + e.getMessage());
            pendingStore.add(uuid, fragments);
        }
    }

    public void resetAllCooldowns() {
        for (TierState state : tierStates.values()) {
            state.resetPositive();
            state.resetNegative();
        }
        LOGGER.at(Level.INFO).log("All reward tier cooldowns have been reset");
    }

    @Nullable
    public PendingRewardsStore getPendingStore() {
        return pendingStore;
    }
}
