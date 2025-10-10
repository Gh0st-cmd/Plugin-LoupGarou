package org.gh0st.loupGarou.game;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.gh0st.loupGarou.LoupGarouPlugin;
import org.gh0st.loupGarou.role.PlayerRole;
import org.gh0st.loupGarou.utils.Utils;

import java.io.File;
import java.io.IOException;

public class StatsManager {

    private final LoupGarouPlugin plugin;
    private FileConfiguration stats;
    private File statsFile;

    public StatsManager(LoupGarouPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        loadStats();
    }

    private void loadStats() {
        if (!statsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer le fichier de statistiques !");
            }
        }
        stats = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void saveStats() {
        try {
            stats.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde des statistiques !");
        }
    }

    public void addGamePlayed(Player player) {
        String path = player.getUniqueId().toString() + ".games-played";
        stats.set(path, stats.getInt(path, 0) + 1);
        stats.set(player.getUniqueId().toString() + ".name", player.getName());
        saveStats();
    }

    public void addWin(Player player, PlayerRole role, String team) {
        String basePath = player.getUniqueId().toString();
        stats.set(basePath + ".wins", stats.getInt(basePath + ".wins", 0) + 1);
        stats.set(basePath + ".wins-as-" + role.name().toLowerCase(),
                stats.getInt(basePath + ".wins-as-" + role.name().toLowerCase(), 0) + 1);
        stats.set(basePath + ".wins-" + team, stats.getInt(basePath + ".wins-" + team, 0) + 1);
        stats.set(basePath + ".name", player.getName());
        saveStats();
    }

    public void addDeath(Player player, String causeOfDeath) {
        String basePath = player.getUniqueId().toString();
        stats.set(basePath + ".deaths", stats.getInt(basePath + ".deaths", 0) + 1);
        stats.set(basePath + ".deaths-" + causeOfDeath.replace(" ", "-"),
                stats.getInt(basePath + ".deaths-" + causeOfDeath.replace(" ", "-"), 0) + 1);
        saveStats();
    }

    public void showStats(Player player) {
        String basePath = player.getUniqueId().toString();

        player.sendMessage("§6" + "=".repeat(50));
        player.sendMessage(Utils.centerText("§6§l📊 VOS STATISTIQUES 📊", 50));
        player.sendMessage("§6" + "=".repeat(50));

        int gamesPlayed = stats.getInt(basePath + ".games-played", 0);
        int wins = stats.getInt(basePath + ".wins", 0);
        int deaths = stats.getInt(basePath + ".deaths", 0);

        player.sendMessage("§e🎮 Parties jouées : §f" + gamesPlayed);
        player.sendMessage("§a🏆 Victoires : §f" + wins);
        player.sendMessage("§c💀 Éliminations : §f" + deaths);

        if (gamesPlayed > 0) {
            double winRate = (double) wins / gamesPlayed * 100;
            player.sendMessage("§b📊 Taux de victoire : §f" + String.format("%.1f", winRate) + "%");
        }

        player.sendMessage("");
        player.sendMessage("§e🎭 Victoires par rôle :");

        // Statistiques par rôle
        for (PlayerRole role : PlayerRole.values()) {
            int roleWins = stats.getInt(basePath + ".wins-as-" + role.name().toLowerCase(), 0);
            if (roleWins > 0) {
                player.sendMessage("§f  • " + role.getDisplayName() + " §7: §f" + roleWins + " victoire" + (roleWins > 1 ? "s" : ""));
            }
        }

        // Victoires par équipe
        int villageWins = stats.getInt(basePath + ".wins-village", 0);
        int werewolfWins = stats.getInt(basePath + ".wins-werewolves", 0);

        player.sendMessage("");
        player.sendMessage("§e👥 Victoires par équipe :");
        if (villageWins > 0) {
            player.sendMessage("§f  • §aVillage §7: §f" + villageWins + " victoire" + (villageWins > 1 ? "s" : ""));
        }
        if (werewolfWins > 0) {
            player.sendMessage("§f  • §cLoups-Garous §7: §f" + werewolfWins + " victoire" + (werewolfWins > 1 ? "s" : ""));
        }

        player.sendMessage("§6" + "=".repeat(50));
    }
}