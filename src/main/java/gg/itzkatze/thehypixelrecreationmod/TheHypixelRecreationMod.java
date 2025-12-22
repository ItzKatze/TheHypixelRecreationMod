package gg.itzkatze.thehypixelrecreationmod;

import gg.itzkatze.thehypixelrecreationmod.commands.*;
import gg.itzkatze.thehypixelrecreationmod.features.KeybindRegistry;
import gg.itzkatze.thehypixelrecreationmod.features.region.RegionRenderer;
import gg.itzkatze.thehypixelrecreationmod.features.region.RegionTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.fabric.event.HypixelModAPICallback;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheHypixelRecreationMod implements ClientModInitializer {
	public static final String MOD_ID = "thehypixelrecreationmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Add a static variable to track if rendering is enabled
	public static boolean regionRenderingEnabled = true;

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initialized");

		// Commands
		PlayerSkinCommand.register();
		GetArmorStandArmorColorsCommand.register();
		GetArmorStandInfos.register();
		GetScoreboardInfo.register();
		ExportRegionsCommand.register();
		ToggleRegionCommand.register(); // Add this line

		// Keybinds
		new KeybindRegistry().onInitializeClient();

		// Region tracking system
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (RegionTracker.isEnabled()) {
				RegionTracker.update();
			}
		});

		WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> {
			if (RegionRenderer.isRenderEnabled()) {
				RegionRenderer.render(context.matrices(), context.gameRenderer().getMainCamera(), 1.0f);
			}
		});

		HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);

		HypixelModAPICallback.EVENT.register(clientboundHypixelPacket -> {
			LOGGER.info("ClientboundHypixelPacket: " + clientboundHypixelPacket.toString());
		});

		HypixelModAPI.getInstance().registerHandler(ClientboundLocationPacket.class, packet -> {
			LOGGER.info("ClientboundLocationPacket: " + packet.toString());
		});
	}
}