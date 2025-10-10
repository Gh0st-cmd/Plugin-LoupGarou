package org.gh0st.loupGarou.utils.extern;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.*;
import org.bukkit.Bukkit;
import org.gh0st.loupGarou.LoupGarouPlugin;
import org.gh0st.loupGarou.role.PlayerRole;
import org.gh0st.loupGarou.game.GameManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des statistiques bStats pour le plugin Loup-Garou
 */
public class BStatsManager {

    private final LoupGarouPlugin plugin;
    private Metrics metrics;

    // Statistiques de session (réinitialisées toutes les 30 minutes)
    private int gamesThisSession = 0;
    private int totalPlayersThisSession = 0;
    private final Map<String, Integer> roleUsageSession = new HashMap<>();
    private final Map<String, Integer> commandUsageSession = new HashMap<>();
    private int villageWinsSession = 0;
    private int werewolfWinsSession = 0;
    private long totalGameDurationSession = 0; // en secondes

    // ID du plugin sur bStats (à obtenir après inscription sur https://bstats.org/)
    private static final int BSTATS_PLUGIN_ID = 27480;

    /**
     * Constructeur du gestionnaire bStats
     * @param plugin Instance du plugin
     */
    public BStatsManager(LoupGarouPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialise bStats avec toutes les statistiques personnalisées
     */
    public void initialize() {
        try {
            // Créer l'instance Metrics
            metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

            // Ajouter toutes les statistiques personnalisées
            addCustomCharts();

            plugin.getLogger().info("✅ bStats initialisé avec succès !");
            plugin.getLogger().info("📊 Les statistiques seront envoyées toutes les 30 minutes");

        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Erreur lors de l'initialisation de bStats : " + e.getMessage());
        }
    }

    /**
     * Ajoute toutes les statistiques personnalisées
     */
    private void addCustomCharts() {

        // 1. Nombre de parties jouées (depuis le dernier envoi)
        metrics.addCustomChart(new SingleLineChart("parties_jouees", () -> {
            int games = gamesThisSession;
            gamesThisSession = 0; // Reset après envoi
            return games;
        }));

        // 2. Nombre moyen de joueurs par partie
        metrics.addCustomChart(new SingleLineChart("joueurs_moyen", () -> {
            if (gamesThisSession == 0) return 0;
            int avg = totalPlayersThisSession / Math.max(1, gamesThisSession);
            totalPlayersThisSession = 0;
            return avg;
        }));

        // 3. Durée moyenne des parties (en minutes)
        metrics.addCustomChart(new SingleLineChart("duree_moyenne_parties", () -> {
            if (gamesThisSession == 0) return 0;
            int avg = (int) (totalGameDurationSession / Math.max(1, gamesThisSession) / 60);
            totalGameDurationSession = 0;
            return avg;
        }));

        // 4. Rôles les plus utilisés (camembert avancé)
        metrics.addCustomChart(new AdvancedPie("roles_utilises", () -> {
            if (roleUsageSession.isEmpty()) return null;
            Map<String, Integer> data = new HashMap<>(roleUsageSession);
            roleUsageSession.clear();
            return data;
        }));

        // 5. Taux de victoire (Villageois vs Loups-Garous)
        metrics.addCustomChart(new AdvancedPie("taux_victoire", () -> {
            if (villageWinsSession == 0 && werewolfWinsSession == 0) {
                return null;
            }
            Map<String, Integer> victories = new HashMap<>();
            victories.put("Villageois", villageWinsSession);
            victories.put("Loups-Garous", werewolfWinsSession);
            villageWinsSession = 0;
            werewolfWinsSession = 0;
            return victories;
        }));

        // 6. Commandes les plus utilisées
        metrics.addCustomChart(new AdvancedPie("commandes_utilisees", () -> {
            if (commandUsageSession.isEmpty()) return null;
            Map<String, Integer> data = new HashMap<>(commandUsageSession);
            commandUsageSession.clear();
            return data;
        }));

        // 7. Nombre de joueurs en ligne (actuel)
        metrics.addCustomChart(new SingleLineChart("joueurs_en_ligne",
                () -> Bukkit.getOnlinePlayers().size()
        ));

        // 8. Utilisation de WorldGuard
        metrics.addCustomChart(new SimplePie("worldguard_actif", () ->
                plugin.getConfigManager().isWorldGuardEnabled() ? "Oui" : "Non"
        ));

        // 9. Langue configurée
        metrics.addCustomChart(new SimplePie("langue", () ->
                plugin.getConfigManager().getConfig().getString("langue", "fr")
        ));

        // 10. Version de Minecraft du serveur
        metrics.addCustomChart(new SimplePie("version_minecraft", () -> {
            try {
                String version = Bukkit.getVersion();
                if (version.contains("MC:")) {
                    return version.split("MC: ")[1].replace(")", "").trim();
                }
                return "Unknown";
            } catch (Exception e) {
                return "Unknown";
            }
        }));

        // 11. Nombre de joueurs minimum configuré
        metrics.addCustomChart(new SimplePie("joueurs_minimum", () ->
                String.valueOf(plugin.getConfigManager().getMinPlayers())
        ));

        // 12. Ratio de loups-garous configuré
        metrics.addCustomChart(new SimplePie("ratio_loups", () -> {
            double ratio = plugin.getConfigManager().getWerewolfRatio();
            if (ratio <= 0.2) return "20% ou moins";
            if (ratio <= 0.3) return "21-30%";
            if (ratio <= 0.4) return "31-40%";
            return "Plus de 40%";
        }));

        // 13. Durée des phases configurées
        metrics.addCustomChart(new DrilldownPie("duree_phases", () -> {
            Map<String, Map<String, Integer>> data = new HashMap<>();

            Map<String, Integer> nightDuration = new HashMap<>();
            int night = plugin.getConfigManager().getNightDuration();
            nightDuration.put(formatDuration(night), 1);
            data.put("Nuit", nightDuration);

            Map<String, Integer> dayDuration = new HashMap<>();
            int day = plugin.getConfigManager().getDayDuration();
            dayDuration.put(formatDuration(day), 1);
            data.put("Jour", dayDuration);

            Map<String, Integer> voteDuration = new HashMap<>();
            int vote = plugin.getConfigManager().getVoteDuration();
            voteDuration.put(formatDuration(vote), 1);
            data.put("Vote", voteDuration);

            return data;
        }));

        // 14. État actuel du jeu
        metrics.addCustomChart(new SimplePie("etat_jeu", () -> {
            GameManager gm = plugin.getGameManager();
            if (gm == null) return "Indisponible";

            switch (gm.getCurrentState()) {
                case WAITING: return "En attente";
                case STARTING: return "Démarrage";
                case NIGHT: return "En jeu (Nuit)";
                case DAY: return "En jeu (Jour)";
                case VOTE: return "En jeu (Vote)";
                case FINISHED: return "Terminé";
                default: return "Inconnu";
            }
        }));
    }

    /**
     * Enregistre une partie jouée
     * @param playerCount Nombre de joueurs
     * @param durationSeconds Durée de la partie en secondes
     * @param winnerTeam Équipe gagnante ("village" ou "werewolves")
     */
    public void recordGame(int playerCount, long durationSeconds, String winnerTeam) {
        gamesThisSession++;
        totalPlayersThisSession += playerCount;
        totalGameDurationSession += durationSeconds;

        if ("village".equals(winnerTeam)) {
            villageWinsSession++;
        } else if ("werewolves".equals(winnerTeam)) {
            werewolfWinsSession++;
        }
    }

    /**
     * Enregistre l'utilisation d'un rôle
     * @param role Rôle utilisé
     */
    public void recordRoleUsage(PlayerRole role) {
        if (role == null) return;
        String roleName = role.getDisplayName();
        roleUsageSession.put(roleName, roleUsageSession.getOrDefault(roleName, 0) + 1);
    }

    /**
     * Enregistre l'utilisation d'une commande
     * @param command Commande utilisée (sans le /)
     */
    public void recordCommandUsage(String command) {
        if (command == null || command.isEmpty()) return;
        commandUsageSession.put(command, commandUsageSession.getOrDefault(command, 0) + 1);
    }

    /**
     * Formate une durée en secondes
     */
    private String formatDuration(int seconds) {
        if (seconds < 60) return seconds + "s";
        int minutes = seconds / 60;
        if (minutes < 5) return "< 5min";
        if (minutes < 10) return "5-10min";
        if (minutes < 15) return "10-15min";
        return "15min+";
    }

    /**
     * Obtient l'instance Metrics
     * @return Instance Metrics ou null si non initialisé
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Vérifie si bStats est activé
     * @return true si bStats est activé et fonctionnel
     */
    public boolean isEnabled() {
        return metrics != null;
    }

    /**
     * Affiche les statistiques de session actuelles (debug)
     */
    public void printSessionStats() {
        plugin.getLogger().info("=== Statistiques de session bStats ===");
        plugin.getLogger().info("Parties jouées : " + gamesThisSession);
        plugin.getLogger().info("Total joueurs : " + totalPlayersThisSession);
        plugin.getLogger().info("Victoires Village : " + villageWinsSession);
        plugin.getLogger().info("Victoires Loups : " + werewolfWinsSession);
        plugin.getLogger().info("Rôles utilisés : " + roleUsageSession.size() + " types");
        plugin.getLogger().info("Commandes utilisées : " + commandUsageSession.size() + " types");
        plugin.getLogger().info("=====================================");
    }
}