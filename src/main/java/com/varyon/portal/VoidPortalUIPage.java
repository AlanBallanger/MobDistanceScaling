package com.varyon.portal;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.RtpvConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VoidPortalUIPage extends InteractiveCustomUIPage<VoidPortalUIPage.EventDataClass> {

    public VoidPortalUIPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("VoidPortalMenu.ui");

        RtpvConfig rtpvConfig = null;
        boolean economyEnabled = false;
        double safeMultiplier = 2.0;
        try {
            if (VaryonPlugin.getStaticConfigManager() != null) {
                rtpvConfig = VaryonPlugin.getStaticConfigManager().getRtpvConfig();
                economyEnabled = rtpvConfig.isEconomyEnabled();
                safeMultiplier = rtpvConfig.getSafeCostMultiplier();
            }
        } catch (Exception ignored) {}

        for (int i = 1; i <= 10; i++) {
            int baseCost = rtpvConfig != null ? rtpvConfig.getCostForZone(i) : 0;
            int safeCost = (int) Math.ceil(baseCost * safeMultiplier);

            String basePrice = economyEnabled ? "(" + baseCost + " coins)" : "";
            String safePrice = economyEnabled ? "(" + safeCost + " coins)" : "";

            commandBuilder.set("#ZoneMainPrice" + i + ".Text", basePrice);
            commandBuilder.set("#ZonePvPPrice" + i + ".Text", basePrice);
            commandBuilder.set("#ZoneSafePrice" + i + ".Text", safePrice);

            int zoneId = i;
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ZoneMain" + i,
                EventData.of("Action", "zone").append("ZoneId", String.valueOf(zoneId)).append("Pvp", "none"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ZonePvP" + i,
                EventData.of("Action", "zone").append("ZoneId", String.valueOf(zoneId)).append("Pvp", "true"));
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ZoneSafe" + i,
                EventData.of("Action", "zone").append("ZoneId", String.valueOf(zoneId)).append("Pvp", "false"));
        }

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull EventDataClass data) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if ("close".equals(data.action)) {
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
            return;
        }

        if ("zone".equals(data.action) && data.zoneId != null) {
            PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && playerRefComp != null) {
                player.getPageManager().setPage(ref, store, Page.None);
                String command;
                if ("true".equals(data.pvp)) {
                    command = "rtpv " + data.zoneId + " true";
                } else if ("false".equals(data.pvp)) {
                    command = "rtpv " + data.zoneId + " false";
                } else {
                    command = "rtpv " + data.zoneId;
                }
                CommandManager.get().handleCommand(playerRefComp, command);
            }
        }
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING),
                    (entry, s) -> entry.action = s, entry -> entry.action)
                .addField(new KeyedCodec<>("ZoneId", Codec.STRING),
                    (entry, s) -> entry.zoneId = s, entry -> entry.zoneId)
                .addField(new KeyedCodec<>("Pvp", Codec.STRING),
                    (entry, s) -> entry.pvp = s, entry -> entry.pvp)
                .build();

        public String action;
        public String zoneId;
        @Nullable public String pvp;
    }
}