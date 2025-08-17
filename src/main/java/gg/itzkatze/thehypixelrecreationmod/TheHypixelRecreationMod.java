package gg.itzkatze.thehypixelrecreationmod;

import gg.itzkatze.thehypixelrecreationmod.commands.GetArmorStandArmorColorsCommand;
import gg.itzkatze.thehypixelrecreationmod.commands.GetArmorStandInfos;
import gg.itzkatze.thehypixelrecreationmod.commands.GetScoreboardInfo;
import gg.itzkatze.thehypixelrecreationmod.commands.PlayerSkinCommand;
import gg.itzkatze.thehypixelrecreationmod.features.KeybindRegistry;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheHypixelRecreationMod implements ClientModInitializer {
	public static final String MOD_ID = "thehypixelrecreationmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initialized");

		// Commands
		PlayerSkinCommand.register();
		GetArmorStandArmorColorsCommand.register();
		GetArmorStandInfos.register();
        GetScoreboardInfo.register();

		// Other
		new KeybindRegistry().onInitializeClient();
	}
}