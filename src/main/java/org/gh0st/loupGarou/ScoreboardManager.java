package org.gh0st.loupGarou;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.ChatColor;

public class ScoreboardManager {

    private final LoupGarouPlugin plugin;
    private Scoreboard scoreboard;
    private Objective objective;

    public ScoreboardManager(LoupGarouPlugin plugin) {
        this.plugin = plugin;
        createScoreboard();
    }

    private void createScoreboard() {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            scoreboard = manager.getNewScoreboard();
            objective = scoreboard.registerNewObjective("loupgarou", "dummy", "§6§l🐺 LOUP-GAROU 🐺");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
    }

    public void updateScoreboard() {
        if (scoreboard == null || objective == null) return;

        GameManager gm = plugin.getGameManager();
        WorldGuardIntegration wg = plugin.getWorldGuardIntegration();

        // Effacer le scoreboard précédent
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int line = 15;

        // Ligne de séparation supérieure
        objective.getScore("§7" + "─".repeat(16)).setScore(line--);

        // État de la partie
        objective.getScore("§e📊 État: §f" + gm.getCurrentState().getDisplayName()).setScore(line--);

        if (gm.isGameRunning()) {
            // Informations de partie
            objective.getScore("§e🌅 Jour: §f" + gm.getDayNumber()).setScore(line--);
            objective.getScore("").setScore(line--);

            // Compteurs de rôles vivants
            int werewolves = 0, villagers = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (gm.isPlayerAlive(player)) {
                    PlayerRole role = gm.getPlayers().get(player.getUniqueId());
                    if (role == PlayerRole.WEREWOLF) {
                        werewolves++;
                    } else if (role != null) {
                        villagers++;
                    }
                }
            }

            objective.getScore("§c🐺 Loups: §f" + werewolves).setScore(line--);
            objective.getScore("§a👥 Village: §f" + villagers).setScore(line--);
            objective.getScore(" ").setScore(line--);

            // Joueurs vivants vs morts
            int alive = gm.getAlivePlayersCount();
            int total = gm.getPlayers().size();
            objective.getScore("§a✅ Vivants: §f" + alive).setScore(line--);
            objective.getScore("§c💀 Morts: §f" + (total - alive)).setScore(line--);

        } else {
            // Mode attente
            objective.getScore("§a👥 Joueurs: §f" + Bukkit.getOnlinePlayers().size()).setScore(line--);
            objective.getScore("§7Minimum: §f" + plugin.getConfigManager().getMinPlayers()).setScore(line--);
            objective.getScore("   ").setScore(line--);
            objective.getScore("§e💡 /lg start pour").setScore(line--);
            objective.getScore("§e   commencer !").setScore(line--);
        }

        // Ligne de séparation inférieure
        objective.getScore("§7" + "─".repeat(16) + " ").setScore(line--);

        // Appliquer le scoreboard aux joueurs concernés
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Afficher le rôle du joueur dans le scoreboard si en partie
            if (gm.isGameRunning()) {
                PlayerRole role = gm.getPlayers().get(player.getUniqueId());
                if (role != null) {
                    // Créer un scoreboard personnalisé pour ce joueur
                    updatePlayerScoreboard(player, role, gm);
                    continue;
                }
            }

            // Si le scoreboard ne doit être affiché que dans la région
            if (plugin.getConfigManager().isScoreboardOnlyInRegion()) {
                // Vérifier si le joueur est dans la bonne région
                if (wg.isWorldGuardAvailable() && plugin.getConfigManager().isWorldGuardEnabled()) {
                    if (wg.canPlayerPlay(player)) {
                        player.setScoreboard(scoreboard);
                    } else {
                        // Retirer le scoreboard si le joueur n'est pas dans la région
                        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    }
                } else {
                    // WorldGuard désactivé, afficher à tout le monde
                    player.setScoreboard(scoreboard);
                }
            } else {
                // Afficher à tout le monde
                player.setScoreboard(scoreboard);
            }
        }
    }

    /**
     * Met à jour le scoreboard personnel d'un joueur avec son rôle
     */
    private void updatePlayerScoreboard(Player player, PlayerRole role, GameManager gm) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard playerScoreboard = manager.getNewScoreboard();
        Objective playerObjective = playerScoreboard.registerNewObjective("loupgarou", "dummy",
                plugin.getConfigManager().getScoreboardTitle());
        playerObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;

        // Ligne de séparation supérieure
        playerObjective.getScore("§7" + "─".repeat(16)).setScore(line--);

        // Afficher le rôle du joueur
        playerObjective.getScore("§e🎭 Votre rôle:").setScore(line--);
        playerObjective.getScore(role.getDisplayName()).setScore(line--);
        playerObjective.getScore("").setScore(line--);

        // État de la partie
        playerObjective.getScore("§e📊 État: §f" + gm.getCurrentState().getDisplayName()).setScore(line--);
        playerObjective.getScore("§e🌅 Jour: §f" + gm.getDayNumber()).setScore(line--);
        playerObjective.getScore(" ").setScore(line--);

        // Compteurs
        int werewolves = 0, villagers = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (gm.isPlayerAlive(p)) {
                PlayerRole r = gm.getPlayers().get(p.getUniqueId());
                if (r == PlayerRole.WEREWOLF) {
                    werewolves++;
                } else if (r != null) {
                    villagers++;
                }
            }
        }

        playerObjective.getScore("§c🐺 Loups: §f" + werewolves).setScore(line--);
        playerObjective.getScore("§a👥 Village: §f" + villagers).setScore(line--);
        playerObjective.getScore("  ").setScore(line--);

        // Statut du joueur
        if (gm.isPlayerAlive(player)) {
            playerObjective.getScore("§a✅ Vous êtes vivant").setScore(line--);
        } else {
            playerObjective.getScore("§c💀 Vous êtes mort").setScore(line--);
        }

        // Ligne de séparation inférieure
        playerObjective.getScore("§7" + "─".repeat(16) + " ").setScore(line--);

        player.setScoreboard(playerScoreboard);
    }

    public void addPlayerRole(Player player, PlayerRole role) {
        if (scoreboard == null) return;

        // Créer ou récupérer l'équipe pour ce rôle
        Team team = scoreboard.getTeam(role.name());
        if (team == null) {
            team = scoreboard.registerNewTeam(role.name());

            // Configuration des couleurs et préfixes par rôle
            switch (role) {
                case WEREWOLF:
                    team.setPrefix("§c🐺 ");
                    team.setColor(ChatColor.RED);
                    break;
                case SEER:
                    team.setPrefix("§5🔮 ");
                    team.setColor(ChatColor.DARK_PURPLE);
                    break;
                case GUARD:
                    team.setPrefix("§b🛡️ ");
                    team.setColor(ChatColor.AQUA);
                    break;
                case WITCH:
                    team.setPrefix("§d🧪 ");
                    team.setColor(ChatColor.LIGHT_PURPLE);
                    break;
                case VILLAGER:
                default:
                    team.setPrefix("§a👤 ");
                    team.setColor(ChatColor.GREEN);
                    break;
            }
        }

        team.addEntry(player.getName());
    }

    public void removeAllTeams() {
        if (scoreboard == null) return;

        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
    }
}
