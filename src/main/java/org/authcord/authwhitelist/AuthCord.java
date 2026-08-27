package org.authcord.authwhitelist;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthCord extends JavaPlugin {

    private JDA jda;
    private boolean isFolia = false;
    private String lang = "ru";

    public final Map<String, CodeEntry> activeCodes = new ConcurrentHashMap<>();
    public final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDataFile();
        lang = getConfig().getString("settings.language", "ru").toLowerCase();

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info(getSysMsg("folia_detected"));
        } catch (ClassNotFoundException ignored) {
            getLogger().info(getSysMsg("paper_detected"));
        }

        String token = getConfig().getString("discord.token");
        if (token != null && !token.isEmpty() && !token.equals("ВАШ_ТОКЕН_БОТА_ЗДЕСЬ")) {
            runAsyncTask(() -> {
                try {
                    jda = JDABuilder.createLight(token)
                            .addEventListeners(new DiscordBotListener(this))
                            .build().awaitReady();
                    getLogger().info(getSysMsg("bot_started"));
                } catch (Exception e) {
                    getLogger().severe(getSysMsg("bot_error") + e.getMessage());
                }
            });
        } else {
            getLogger().warning(getSysMsg("token_missing"));
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }

    @Override
    public void onDisable() {
        if (jda != null) jda.shutdown();
    }

    public String getSysMsg(String key) {
        boolean ru = lang.equals("ru");
        return switch (key) {
            case "folia_detected" -> ru ? "Folia сервер" : "Folia server";
            case "paper_detected" -> ru ? "Paper сервер." : "Paper server.";
            case "bot_started" -> ru ? "Discord-бот успешно запущен!" : "Discord bot started successfully!";
            case "bot_error" -> ru ? "Ошибка при запуске бота: " : "Error starting bot: ";
            case "token_missing" -> ru ? "Токен бота не указан в config.yml!" : "Bot token is missing in config.yml!";
            case "guild_missing" -> ru ? "ID сервера (guild-id) не указан в конфиге!" : "Guild-id is missing in config.yml!";
            case "guild_not_found" -> ru ? "Бот не нашел сервер с указанным ID. Проверьте ID и пригласите бота!" : "Bot could not find guild with provided ID. Check ID and invite the bot!";
            case "commands_reg" -> ru ? "Слэш-команды зарегистрированы для: " : "Slash commands registered for: ";
            case "dm_success" -> ru ? "Отправлено предупреждение в ЛС игроку " : "Sent DM alert to player ";
            case "dm_fail" -> ru ? "Не удалось отправить ЛС игроку (возможно, закрыта личка): " : "Failed to send DM to (DMs might be closed): ";
            case "user_not_found" -> ru ? "Пользователь Discord не найден для: " : "Discord user not found for: ";
            default -> "Unknown key: " + key;
        };
    }

    public String getMessage(String path) {
        String msg = getConfig().getString(path);
        return msg != null ? msg.replace("&", "§") : "Missing text: " + path;
    }

    public JDA getJda() { return jda; }

    private void loadDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) { try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); } }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData(String playerName, String discordId, String ip) {
        dataConfig.set("users." + playerName.toLowerCase() + ".discord", discordId);
        dataConfig.set("users." + playerName.toLowerCase() + ".ip", ip);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public String getLinkedDiscord(String playerName) { return dataConfig.getString("users." + playerName.toLowerCase() + ".discord"); }
    public String getLinkedIp(String playerName) { return dataConfig.getString("users." + playerName.toLowerCase() + ".ip"); }

    public String generateCode(String name, UUID uuid, boolean checkCooldown) {
        long now = System.currentTimeMillis();
        if (checkCooldown) {
            int cooldownSec = getConfig().getInt("settings.request-cooldown-seconds", 30);
            if (requestCooldowns.containsKey(uuid)) {
                long lastRequest = requestCooldowns.get(uuid);
                long secondsPassed = (now - lastRequest) / 1000;
                if (secondsPassed < cooldownSec) return "COOLDOWN:" + (cooldownSec - secondsPassed);
            }
        }
        String code = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        activeCodes.put(code, new CodeEntry(name, now));
        requestCooldowns.put(uuid, now);
        return code;
    }

    public boolean isCodeExpired(long createdAt) {
        return (System.currentTimeMillis() - createdAt) > (getConfig().getInt("settings.code-expiration-minutes", 5) * 60000L);
    }

    public Component getFormattedMessage(String path, String... replacements) {
        String raw = getConfig().getString(path, "Missing path: " + path);
        raw = raw.replace("%url%", getConfig().getString("settings.discord-invite-url", ""));

        for (int i = 0; i < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i+1]);
        }

        return MiniMessage.miniMessage().deserialize(raw);
    }

    public String getPlayerByDiscordId(String discordId) {
        ConfigurationSection users = dataConfig.getConfigurationSection("users");
        if (users == null) return null;

        for (String mcName : users.getKeys(false)) {
            if (discordId.equals(users.getString(mcName + ".discord"))) {
                return mcName;
            }
        }
        return null;
    }

    public void runGlobalTask(Runnable task) {
        if (isFolia) Bukkit.getGlobalRegionScheduler().execute(this, task);
        else Bukkit.getScheduler().runTask(this, task);
    }

    public void runAsyncTask(Runnable task) {
        if (isFolia) Bukkit.getAsyncScheduler().runNow(this, st -> task.run());
        else Bukkit.getScheduler().runTaskAsynchronously(this, task);
    }
}