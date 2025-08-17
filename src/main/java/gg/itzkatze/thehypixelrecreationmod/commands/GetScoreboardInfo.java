package gg.itzkatze.thehypixelrecreationmod.commands;

import gg.itzkatze.thehypixelrecreationmod.utils.ChatUtils;
import gg.itzkatze.thehypixelrecreationmod.utils.StringUtilities;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GetScoreboardInfo {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getScoreboardInfo")
                    .executes(ctx -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.world == null || client.player == null) return 1;

                        Scoreboard board = client.world.getScoreboard();
                        ScoreboardObjective objective = board.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
                        if (objective == null) {
                            ChatUtils.warn("No sidebar scoreboard found.");
                            return 1;
                        }

                        List<ScoreboardEntry> entries = board.getScoreboardEntries(objective).stream()
                                .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed())
                                .toList();

                        if (entries.isEmpty()) {
                            ChatUtils.warn("Sidebar scoreboard is empty.");
                            return 1;
                        }

                        ChatUtils.sendLine();

                        client.player.sendMessage(Text.empty()
                                .append(objective.getDisplayName())
                                .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(StringUtilities.toLegacyString(objective.getDisplayName())))), false);

                        for (ScoreboardEntry scoreboardEntry : entries) {
                            Text line = renderEntry(board, scoreboardEntry);
                            String legacyLine = StringUtilities.toLegacyString(line);

                            Text clickable = Text.empty()
                                    .append(line)
                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(legacyLine)));
                            client.player.sendMessage(clickable, false);
                        }

                        ChatUtils.sendLine();

                        return 1;
                    }));
        });
    }

    private static Text renderEntry(Scoreboard board, ScoreboardEntry e) {
        Text base = e.display() != null ? e.display() : Text.literal(e.owner());

        Team team = board.getScoreHolderTeam(e.owner());
        if (team != null) {
            return Text.empty().append(team.getPrefix()).append(base).append(team.getSuffix());
        }
        return base;
    }
}
