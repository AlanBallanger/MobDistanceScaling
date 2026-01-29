package com.mobdistancescaling.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.mobdistancescaling.MobDistanceScalingPlugin;
import com.mobdistancescaling.config.ConfigManager;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class MdsCommand extends AbstractAsyncCommand {
    private final MobDistanceScalingPlugin plugin;

    public MdsCommand(MobDistanceScalingPlugin plugin) {
        super("mds", "MobDistanceScaling commands");
        this.plugin = plugin;
        this.requirePermission("mobdistancescaling.admin");
        this.addSubCommand(new ReloadSubCommand(plugin));
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("MobDistanceScaling commands:").color(Color.YELLOW));
        commandContext.sendMessage(Message.raw("  /mds reload - Reload configuration").color(Color.GRAY));
        return CompletableFuture.completedFuture(null);
    }

    public static class ReloadSubCommand extends AbstractAsyncCommand {
        private final MobDistanceScalingPlugin plugin;

        public ReloadSubCommand(MobDistanceScalingPlugin plugin) {
            super("reload", "Reload MobDistanceScaling configuration");
            this.plugin = plugin;
            this.requirePermission("mobdistancescaling.admin.reload");
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
            try {
                ConfigManager configManager = plugin.getConfigManager();
                configManager.reload();

                int zoneCount = configManager.getZoneConfig().getZones().size();
                String pattern = configManager.getZoneConfig().getMinimapPattern();
                int opacity = configManager.getZoneConfig().getMinimapOpacity();

                commandContext.sendMessage(Message.raw("MobDistanceScaling configuration reloaded!").color(Color.GREEN));
                commandContext.sendMessage(Message.raw("  Zones: " + zoneCount).color(Color.WHITE));
                commandContext.sendMessage(Message.raw("  Pattern: " + pattern).color(Color.WHITE));
                commandContext.sendMessage(Message.raw("  Opacity: " + opacity + "%").color(Color.WHITE));
                commandContext.sendMessage(Message.raw("Note: Map cache may need to be cleared for visual changes.").color(Color.YELLOW));
            } catch (Exception e) {
                commandContext.sendMessage(Message.raw("Failed to reload configuration: " + e.getMessage()).color(Color.RED));
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
