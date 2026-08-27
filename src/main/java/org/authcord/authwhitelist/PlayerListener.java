package org.authcord.authwhitelist;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.net.InetSocketAddress;

public class PlayerListener implements Listener {

    private final AuthCord plugin;

    public PlayerListener(AuthCord plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getConfig().getString("settings.auth-mode", "ONLINE").equalsIgnoreCase("ONLINE")) return;

        if (!Bukkit.getOfflinePlayer(event.getName()).isWhitelisted()) {
            String code = plugin.generateCode(event.getName(), event.getUniqueId(), false);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    plugin.getFormattedMessage("messages.in-game.kick-online", "%code%", code));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getString("settings.auth-mode").equalsIgnoreCase("OFFLINE")) return;

        Player player = event.getPlayer();
        String playerName = player.getName();

        InetSocketAddress address = player.getAddress();
        String currentIp = (address != null) ? address.getAddress().getHostAddress() : "неизвестно";

        String linkedDiscord = plugin.getLinkedDiscord(playerName);
        String linkedIp = plugin.getLinkedIp(playerName);

        if (linkedDiscord == null) {
            plugin.frozenPlayers.add(player.getUniqueId());
            player.sendMessage(plugin.getFormattedMessage("messages.in-game.not-registered"));
            return;
        }

        if (!currentIp.equals(linkedIp)) {
            plugin.frozenPlayers.add(player.getUniqueId());
            player.sendMessage(plugin.getFormattedMessage("messages.in-game.new-ip"));

            if (plugin.getJda() != null) {
                plugin.getJda().retrieveUserById(linkedDiscord).queue(discordUser -> {
                    discordUser.openPrivateChannel().queue(channel -> {

                        String alertMsg = plugin.getMessage("messages.discord.dm-alert")
                                .replace("%player%", playerName)
                                .replace("%ip%", currentIp);

                        channel.sendMessage(alertMsg).queue(
                                success -> plugin.getLogger().info(plugin.getSysMsg("dm_success") + playerName),
                                error -> plugin.getLogger().warning(plugin.getSysMsg("dm_fail") + playerName)
                        );
                    });
                }, error -> plugin.getLogger().warning("Не найден Discord ID: " + linkedDiscord));
            }
            return;
        }

        player.sendMessage(plugin.getFormattedMessage("messages.in-game.auth-success"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.frozenPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.frozenPlayers.contains(player.getUniqueId())) {
            if (event.getMessage().toLowerCase().startsWith("/getcode")) {
                event.setCancelled(true);
                String result = plugin.generateCode(player.getName(), player.getUniqueId(), true);

                if (result.startsWith("COOLDOWN:")) {
                    String timeLeft = result.split(":")[1];
                    player.sendMessage(plugin.getFormattedMessage("messages.in-game.cooldown", "%time%", timeLeft));
                } else {
                    player.sendMessage(plugin.getFormattedMessage("messages.in-game.getcode-success", "%code%", result));
                }
            } else {
                event.setCancelled(true);
                player.sendMessage(plugin.getFormattedMessage("messages.in-game.action-blocked"));
            }
        }
    }

    @EventHandler public void onMove(PlayerMoveEvent event) {
        if (plugin.frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler public void onChat(AsyncChatEvent event) { if (plugin.frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler public void onInteract(PlayerInteractEvent event) { if (plugin.frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler public void onBlockBreak(BlockBreakEvent event) { if (plugin.frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler public void onBlockPlace(BlockPlaceEvent event) { if (plugin.frozenPlayers.contains(event.getPlayer().getUniqueId())) event.setCancelled(true); }
}