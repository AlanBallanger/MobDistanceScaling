package com.varyon.essence;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.varyon.config.GlobalRewardsConfig;
import com.varyon.faction.FactionManager;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GlobalRewardsManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    private final GlobalRewardsConfig config;
    private final EssenceManager essenceManager;
    private final FactionManager factionManager;
    
    private final Map<Integer, TierState> tierStates = new ConcurrentHashMap<>();
    
    private static class TierState {
        private boolean positiveReached = false;
        private boolean negativeReached = false;
        private long positiveLastRewardTime = 0;
        private long negativeLastRewardTime = 0;
        
        public boolean canRewardPositive(long cooldownMillis) {
            return !positiveReached || (System.currentTimeMillis() - positiveLastRewardTime >= cooldownMillis);
        }
        
        public boolean canRewardNegative(long cooldownMillis) {
            return !negativeReached || (System.currentTimeMillis() - negativeLastRewardTime >= cooldownMillis);
        }
        
        public void markPositiveRewarded() {
            positiveReached = true;
            positiveLastRewardTime = System.currentTimeMillis();
        }
        
        public void markNegativeRewarded() {
            negativeReached = true;
            negativeLastRewardTime = System.currentTimeMillis();
        }
        
        public void resetPositive() {
            positiveReached = false;
        }
        
        public void resetNegative() {
            negativeReached = false;
        }
    }
    
    public GlobalRewardsManager(@Nonnull GlobalRewardsConfig config, @Nonnull EssenceManager essenceManager, @Nonnull FactionManager factionManager) {
        this.config = config;
        this.essenceManager = essenceManager;
        this.factionManager = factionManager;
        
        for (int i = 0; i < config.getTiers().size(); i++) {
            tierStates.put(i, new TierState());
        }
    }
    
    public void checkAndDistributeRewards() {
        int globalBalance = essenceManager.getGlobalBalance();
        long cooldownMillis = config.getRewardCooldownMinutes() * 60 * 1000L;
        
        LOGGER.at(Level.INFO).log("Checking rewards for global balance: " + globalBalance + " (cooldown: " + config.getRewardCooldownMinutes() + " minutes)");
        
        java.util.List<GlobalRewardsConfig.RewardTier> tiers = config.getTiers();
        
        for (int i = 0; i < tiers.size(); i++) {
            GlobalRewardsConfig.RewardTier tier = tiers.get(i);
            TierState state = tierStates.get(i);
            
            LOGGER.at(Level.INFO).log("Tier " + (i + 1) + " - Threshold: " + tier.getThreshold() + ", Positive reached: " + state.positiveReached + ", Negative reached: " + state.negativeReached);
            
            if (globalBalance >= tier.getThreshold()) {
                LOGGER.at(Level.INFO).log("Balance >= threshold, checking positive cooldown...");
                if (state.canRewardPositive(cooldownMillis)) {
                    LOGGER.at(Level.INFO).log("Global balance " + globalBalance + " reached tier " + (i + 1) + " (threshold: " + tier.getThreshold() + ") - Rewarding Fracture faction");
                    distributeFactionReward(FactionManager.Faction.FRACTURE, tier, i + 1);
                    state.markPositiveRewarded();
                } else {
                    long timeLeft = (cooldownMillis - (System.currentTimeMillis() - state.positiveLastRewardTime)) / 1000;
                    LOGGER.at(Level.INFO).log("Tier " + (i + 1) + " positive still on cooldown (" + timeLeft + "s remaining)");
                }
            } else if (globalBalance > 0 && globalBalance < tier.getThreshold()) {
                state.resetPositive();
            }
            
            if (globalBalance <= -tier.getThreshold()) {
                LOGGER.at(Level.INFO).log("Balance <= -threshold, checking negative cooldown...");
                if (state.canRewardNegative(cooldownMillis)) {
                    LOGGER.at(Level.INFO).log("Global balance " + globalBalance + " reached tier -" + (i + 1) + " (threshold: -" + tier.getThreshold() + ") - Rewarding Noyau faction");
                    distributeFactionReward(FactionManager.Faction.NOYAU, tier, i + 1);
                    state.markNegativeRewarded();
                } else {
                    long timeLeft = (cooldownMillis - (System.currentTimeMillis() - state.negativeLastRewardTime)) / 1000;
                    LOGGER.at(Level.INFO).log("Tier " + (i + 1) + " negative still on cooldown (" + timeLeft + "s remaining)");
                }
            } else if (globalBalance < 0 && globalBalance > -tier.getThreshold()) {
                state.resetNegative();
            }
        }
    }
    
    public void resetAllCooldowns() {
        for (TierState state : tierStates.values()) {
            state.resetPositive();
            state.resetNegative();
        }
        LOGGER.at(Level.INFO).log("All reward tier cooldowns have been reset");
    }
    
    private void distributeFactionReward(@Nonnull FactionManager.Faction faction, @Nonnull GlobalRewardsConfig.RewardTier tier, int tierNumber) {
        Collection<PlayerRef> players = Universe.get().getPlayers();
        int rewardedCount = 0;
        
        for (PlayerRef playerRef : players) {
            if (playerRef == null || !playerRef.getReference().isValid()) {
                continue;
            }
            
            FactionManager.Faction playerFaction = factionManager.getFaction(playerRef.getUuid());
            if (playerFaction == faction) {
                try {
                    // Récupérer le monde du joueur
                    com.hypixel.hytale.component.Ref ref = playerRef.getReference();
                    com.hypixel.hytale.component.Store store = ref.getStore();
                    com.hypixel.hytale.server.core.universe.world.World world = 
                        ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore)store.getExternalData()).getWorld();
                    
                    // Exécuter dans le WorldThread
                    world.execute(() -> {
                        giveRewardsToPlayer(playerRef, tier, faction, tierNumber);
                    });
                    rewardedCount++;
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Failed to schedule reward for " + playerRef.getUsername() + ": " + e.getMessage());
                }
            }
        }
        
        LOGGER.at(Level.INFO).log("Distributed tier " + tierNumber + " rewards to " + rewardedCount + " members of " + faction.getDisplayName());
    }
    
    private void giveRewardsToPlayer(@Nonnull PlayerRef playerRef, @Nonnull GlobalRewardsConfig.RewardTier tier, @Nonnull FactionManager.Faction faction, int tierNumber) {
        LOGGER.at(Level.INFO).log("Starting to give rewards to player: " + playerRef.getUsername());
        
        try {
            StringBuilder rewardMessage = new StringBuilder();
            
            LOGGER.at(Level.INFO).log("Number of items to give: " + tier.getItems().size());
            
            // Donner les items
            for (GlobalRewardsConfig.RewardItem rewardItem : tier.getItems()) {
                LOGGER.at(Level.INFO).log("Attempting to give item: " + rewardItem.getItemId() + " x" + rewardItem.getAmount());
                
                try {
                    ItemStack itemStack = new ItemStack(rewardItem.getItemId(), rewardItem.getAmount());
                    LOGGER.at(Level.INFO).log("ItemStack created successfully");
                    
                    // Accéder à l'inventaire via les composants
                    com.hypixel.hytale.component.Ref playerComponentRef = playerRef.getReference();
                    LOGGER.at(Level.INFO).log("PlayerRef reference obtained: " + (playerComponentRef != null));
                    
                    if (playerComponentRef != null && playerComponentRef.isValid()) {
                        LOGGER.at(Level.INFO).log("Reference is valid");
                        com.hypixel.hytale.component.Store store = playerComponentRef.getStore();
                        LOGGER.at(Level.INFO).log("Store obtained: " + (store != null));
                        
                        com.hypixel.hytale.server.core.entity.entities.Player playerComponent = 
                            (com.hypixel.hytale.server.core.entity.entities.Player) store.getComponent(
                                playerComponentRef, 
                                com.hypixel.hytale.server.core.entity.entities.Player.getComponentType()
                            );
                        
                        LOGGER.at(Level.INFO).log("Player component obtained: " + (playerComponent != null));
                        
                        if (playerComponent != null && playerComponent.getInventory() != null) {
                            LOGGER.at(Level.INFO).log("Inventory is not null, attempting to add item");
                            
                            com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction transaction =
                                playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(itemStack);
                            
                            LOGGER.at(Level.INFO).log("Transaction completed");
                            
                            if (ItemStack.isEmpty(transaction.getRemainder())) {
                                if (rewardMessage.length() > 0) rewardMessage.append(", ");
                                rewardMessage.append(rewardItem.getAmount()).append("x ").append(rewardItem.getItemId());
                                LOGGER.at(Level.INFO).log("Successfully gave " + rewardItem.getAmount() + "x " + rewardItem.getItemId() + " to " + playerRef.getUsername());
                            } else {
                                LOGGER.at(Level.WARNING).log("Failed to give full item " + rewardItem.getItemId() + " to player " + playerRef.getUsername() + " (inventory full?)");
                            }
                        } else {
                            LOGGER.at(Level.WARNING).log("Player component or inventory is null for " + playerRef.getUsername());
                        }
                    } else {
                        LOGGER.at(Level.WARNING).log("Player reference is invalid for " + playerRef.getUsername());
                    }
                    
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("Error giving item " + rewardItem.getItemId() + " to " + playerRef.getUsername() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            String factionColor = faction == FactionManager.Faction.NOYAU ? "#5555FF" : "#FF8800";
            playerRef.sendMessage(
                Message.raw("[Récompense] Palier " + tierNumber + " atteint!").color(Color.decode(factionColor))
            );
            
            if (rewardMessage.length() > 0) {
                playerRef.sendMessage(
                    Message.raw("Vous recevez: " + rewardMessage).color(Color.GREEN)
                );
                LOGGER.at(Level.INFO).log("Sent reward message to player");
            } else {
                LOGGER.at(Level.WARNING).log("No items were successfully given to " + playerRef.getUsername());
            }
            
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Error giving rewards to player " + playerRef.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
