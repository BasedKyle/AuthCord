package org.authcord.authwhitelist;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

public class DiscordBotListener extends ListenerAdapter {

    private final AuthCord plugin;

    public DiscordBotListener(AuthCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        String guildId = plugin.getConfig().getString("discord.guild-id");
        if (guildId == null || guildId.isEmpty()) {
            plugin.getLogger().warning(plugin.getSysMsg("guild_missing"));
            return;
        }

        Guild guild = event.getJDA().getGuildById(guildId);
        if (guild != null) {
            guild.updateCommands().addCommands(
                    Commands.slash("verify", "Verify Minecraft Account")
                            .addOption(OptionType.STRING, "code", "Code from game", true)
            ).queue();
            plugin.getLogger().info(plugin.getSysMsg("commands_reg") + guild.getName());
        } else {
            plugin.getLogger().severe(plugin.getSysMsg("guild_not_found"));
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("verify")) return;

        String code = event.getOption("code").getAsString().toUpperCase();
        String discordId = event.getUser().getId();
        CodeEntry entry = plugin.activeCodes.get(code);

        if (entry == null) {
            event.reply(plugin.getConfig().getString("messages.discord.invalid-code")).setEphemeral(true).queue();
            return;
        }

        String playerName = entry.playerName();

        String alreadyLinkedTo = plugin.getPlayerByDiscordId(discordId);
        if (alreadyLinkedTo != null && !alreadyLinkedTo.equalsIgnoreCase(playerName)) {
            String msg = plugin.getConfig().getString("messages.discord.discord-already-used")
                    .replace("%player%", alreadyLinkedTo);
            event.reply(msg).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        plugin.runGlobalTask(() -> {
            String authMode = plugin.getConfig().getString("settings.auth-mode", "ONLINE");

            if (authMode.equalsIgnoreCase("ONLINE")) {
                plugin.saveData(playerName, discordId, "mojang-verified"); // ТЕПЕРЬ СОХРАНЯЕТСЯ
                Bukkit.getOfflinePlayer(playerName).setWhitelisted(true);
                Bukkit.reloadWhitelist();
                plugin.activeCodes.remove(code);

                event.getHook().sendMessage(plugin.getConfig().getString("messages.discord.verify-success").replace("%player%", playerName)).queue();
            } else {
                Player onlinePlayer = Bukkit.getPlayerExact(playerName);
                if (onlinePlayer != null) {
                    String ip = onlinePlayer.getAddress().getAddress().getHostAddress();
                    plugin.saveData(playerName, discordId, ip); // СОХРАНЯЕТСЯ
                    plugin.activeCodes.remove(code);

                    Bukkit.getOfflinePlayer(playerName).setWhitelisted(true);
                    Bukkit.reloadWhitelist();
                    plugin.frozenPlayers.remove(onlinePlayer.getUniqueId());

                    onlinePlayer.sendMessage(plugin.getFormattedMessage("messages.in-game.verify-success"));
                    event.getHook().sendMessage(plugin.getConfig().getString("messages.discord.verify-success").replace("%player%", playerName)).queue();
                } else {
                    event.getHook().sendMessage(plugin.getConfig().getString("messages.discord.player-offline")).queue();
                }
            }
        });
    }
}