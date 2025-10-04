package org.gh0st.loupGarou;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

public class LoupGarouPlugin extends JavaPlugin {

    private GameManager gameManager;
    private ConfigManager configManager;
    private ScoreboardManager scoreboardManager;
    private StatsManager statsManager;
    private WorldGuardIntegration worldGuardIntegration;
    private UpdateChecker updateChecker;
    private BukkitTask scoreboardUpdateTask;

    @Override
    public void onEnable() {
        // Dans la classe principale qui étend JavaPlugin
        String version = this.getDescription().getVersion();
        // Message de démarrage avec style
        getLogger().info("=================================");
        getLogger().info("   🐺 LOUP-GAROU PLUGIN v" + version + " 🐺");
        getLogger().info("   Chargement en cours...");
        getLogger().info("=================================");

        // Création du dossier de configuration si nécessaire
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
            getLogger().info("Dossier de configuration créé : " + getDataFolder().getPath());
        }

        // Initialisation des managers dans le bon ordre
        try {
            // 1. Configuration en premier
            this.configManager = new ConfigManager(this);
            getLogger().info("✅ ConfigManager initialisé");

            // 2. Statistiques
            this.statsManager = new StatsManager(this);
            getLogger().info("✅ StatsManager initialisé");

            // 3. Gestionnaire de partie
            this.gameManager = new GameManager(this);
            getLogger().info("✅ GameManager initialisé");

            // 4. Scoreboard
            this.scoreboardManager = new ScoreboardManager(this);
            getLogger().info("✅ ScoreboardManager initialisé");

            // 5. Intégration WorldGuard
            this.worldGuardIntegration = new WorldGuardIntegration(this);
            getLogger().info("✅ WorldGuardIntegration initialisé");

        } catch (Exception e) {
            getLogger().severe("❌ Erreur lors de l'initialisation des managers : " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Enregistrement des commandes
        try {
            if (getCommand("lg") != null) {
                LoupGarouCommand lgCommand = new LoupGarouCommand(this);
                getCommand("lg").setExecutor(lgCommand);
                getCommand("lg").setTabCompleter(lgCommand);
                getLogger().info("✅ Commande /lg enregistrée");
            } else {
                getLogger().warning("⚠️ Commande /lg non trouvée dans plugin.yml");
            }

            if (getCommand("vote") != null) {
                VoteCommand voteCommand = new VoteCommand(this);
                getCommand("vote").setExecutor(voteCommand);
                getCommand("vote").setTabCompleter(voteCommand);
                getLogger().info("✅ Commande /vote enregistrée");
            } else {
                getLogger().warning("⚠️ Commande /vote non trouvée dans plugin.yml");
            }

        } catch (Exception e) {
            getLogger().severe("❌ Erreur lors de l'enregistrement des commandes : " + e.getMessage());
            e.printStackTrace();
        }

        // Enregistrement des événements
        try {
            getServer().getPluginManager().registerEvents(new GameListener(this), this);
            getLogger().info("✅ Événements enregistrés");
        } catch (Exception e) {
            getLogger().severe("❌ Erreur lors de l'enregistrement des événements : " + e.getMessage());
            e.printStackTrace();
        }

        // Création de la configuration par défaut
        try {
            configManager.createDefaultConfig();
            getLogger().info("✅ Configuration chargée");
        } catch (Exception e) {
            getLogger().severe("❌ Erreur lors du chargement de la configuration : " + e.getMessage());
            e.printStackTrace();
        }

        // Démarrage de la tâche de mise à jour du scoreboard
        startScoreboardUpdateTask();

        // Messages de fin de chargement
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "╔════════════════════════════════════╗");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "║  🐺 Plugin Loup-Garou activé ! 🐺    ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "║                                    ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "║  Commandes : /lg aide              ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "║  Version : " + version + "                   ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "║  Support : Minecraft 1.21.8+       ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "║  Créé par : Gh0st-cmd              ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "╚════════════════════════════════════╝");

        // Afficher les informations de configuration
        getLogger().info("Configuration du jeu :");
        getLogger().info("  - Monde de jeu : " + configManager.getSpawnWorld());
        getLogger().info("  - Spawn : X=" + configManager.getSpawnX() + " Y=" + configManager.getSpawnY() + " Z=" + configManager.getSpawnZ());
        getLogger().info("  - WorldGuard : " + (configManager.isWorldGuardEnabled() ? "Activé (région: " + configManager.getRegionName() + ")" : "Désactivé"));
        getLogger().info("  - Joueurs min/max : " + configManager.getMinPlayers() + "/" + configManager.getMaxPlayers());

        getLogger().info("Plugin Loup-Garou chargé avec succès !");

        // Vérification du nombre de joueurs connectés
        int playersOnline = Bukkit.getOnlinePlayers().size();
        if (playersOnline > 0) {
            getLogger().info("Joueurs connectés détectés : " + playersOnline);
            // Mise à jour immédiate du scoreboard
            Bukkit.getScheduler().runTaskLater(this, () -> {
                scoreboardManager.updateScoreboard();
            }, 20L); // 1 seconde après le démarrage
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("=================================");
        getLogger().info("   Arrêt du plugin Loup-Garou...");
        getLogger().info("=================================");

        // Arrêter une partie en cours si nécessaire
        if (gameManager != null && gameManager.isGameRunning()) {
            getLogger().info("Arrêt de la partie en cours...");
            gameManager.stopGame();

            // Informer les joueurs
            Bukkit.broadcastMessage("§c⚠️ Le serveur redémarre, la partie est interrompue !");
        }

        // Arrêter la tâche de mise à jour du scoreboard
        if (scoreboardUpdateTask != null && !scoreboardUpdateTask.isCancelled()) {
            scoreboardUpdateTask.cancel();
            getLogger().info("Tâche de mise à jour du scoreboard arrêtée");
        }

        // Sauvegarder les statistiques
        if (statsManager != null) {
            try {
                statsManager.saveStats();
                getLogger().info("✅ Statistiques sauvegardées");
            } catch (Exception e) {
                getLogger().warning("⚠️ Erreur lors de la sauvegarde des statistiques : " + e.getMessage());
            }
        }

        // Sauvegarder la configuration
        if (configManager != null) {
            try {
                configManager.saveConfig();
                getLogger().info("✅ Configuration sauvegardée");
            } catch (Exception e) {
                getLogger().warning("⚠️ Erreur lors de la sauvegarde de la configuration : " + e.getMessage());
            }
        }

        // Nettoyer les scoreboards
        if (scoreboardManager != null) {
            try {
                scoreboardManager.removeAllTeams();
                getLogger().info("✅ Scoreboards nettoyés");
            } catch (Exception e) {
                getLogger().warning("⚠️ Erreur lors du nettoyage des scoreboards : " + e.getMessage());
            }
        }

        // Remettre les joueurs en mode normal
        try {
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.resetPlayerTime();
                // Retirer le scoreboard personnalisé
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            });
            getLogger().info("✅ Joueurs remis en mode normal");
        } catch (Exception e) {
            getLogger().warning("⚠️ Erreur lors de la remise en mode normal : " + e.getMessage());
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "╔════════════════════════════════════╗");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "║   🐺 Plugin Loup-Garou arrêté 🐺     ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "║                                    ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "║  Merci d'avoir utilisé le plugin!  ║");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "╚════════════════════════════════════╝");

        getLogger().info("Plugin Loup-Garou arrêté proprement !");
    }

    /**
     * Démarre la tâche de mise à jour automatique du scoreboard
     */
    private void startScoreboardUpdateTask() {
        if (configManager != null && configManager.isScoreboardEnabled()) {
            int updateInterval = configManager.getScoreboardUpdateInterval();

            scoreboardUpdateTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                try {
                    if (scoreboardManager != null) {
                        scoreboardManager.updateScoreboard();
                    }
                } catch (Exception e) {
                    getLogger().warning("Erreur lors de la mise à jour du scoreboard : " + e.getMessage());
                }
            }, 20L, updateInterval); // Première mise à jour après 1 seconde, puis selon l'intervalle

            getLogger().info("✅ Tâche de mise à jour du scoreboard démarrée (intervalle: " + updateInterval + " ticks)");
        } else {
            getLogger().info("ℹ️ Scoreboard désactivé dans la configuration");
        }
    }

    /**
     * Redémarre la tâche de mise à jour du scoreboard (utile après rechargement de config)
     */
    public void restartScoreboardUpdateTask() {
        if (scoreboardUpdateTask != null && !scoreboardUpdateTask.isCancelled()) {
            scoreboardUpdateTask.cancel();
            getLogger().info("Arrêt de l'ancienne tâche de mise à jour du scoreboard");
        }
        startScoreboardUpdateTask();
        getLogger().info("Tâche de mise à jour du scoreboard redémarrée");
    }

    /**
     * Vérifie si le plugin est correctement initialisé
     */
    public boolean isFullyLoaded() {
        return gameManager != null &&
                configManager != null &&
                scoreboardManager != null &&
                statsManager != null &&
                worldGuardIntegration != null;
    }

    /**
     * Affiche des informations de debug sur le plugin
     */
    public void debugInfo() {
        getLogger().info("=== DEBUG INFO ===");
        getLogger().info("GameManager: " + (gameManager != null ? "✅" : "❌"));
        getLogger().info("ConfigManager: " + (configManager != null ? "✅" : "❌"));
        getLogger().info("ScoreboardManager: " + (scoreboardManager != null ? "✅" : "❌"));
        getLogger().info("StatsManager: " + (statsManager != null ? "✅" : "❌"));
        getLogger().info("WorldGuardIntegration: " + (worldGuardIntegration != null ? "✅" : "❌"));
        getLogger().info("Scoreboard Update Task: " + (scoreboardUpdateTask != null && !scoreboardUpdateTask.isCancelled() ? "✅" : "❌"));

        if (gameManager != null) {
            getLogger().info("Game State: " + gameManager.getCurrentState().getDisplayName());
            getLogger().info("Players in game: " + gameManager.getPlayers().size());
        }

        if (worldGuardIntegration != null) {
            getLogger().info("WorldGuard Available: " + (worldGuardIntegration.isWorldGuardAvailable() ? "✅" : "❌"));
        }

        if (configManager != null) {
            getLogger().info("Config World: " + configManager.getSpawnWorld());
            getLogger().info("Config Region: " + configManager.getRegionName());
            getLogger().info("WorldGuard Enabled in config: " + configManager.isWorldGuardEnabled());
        }

        getLogger().info("Players online: " + Bukkit.getOnlinePlayers().size());
        getLogger().info("==================");
    }

    // Getters pour les autres classes
    public GameManager getGameManager() {
        return gameManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public WorldGuardIntegration getWorldGuardIntegration() {
        return worldGuardIntegration;
    }

    /**
     * Méthode utilitaire pour envoyer des messages avec le préfixe du plugin
     */
    public void broadcastMessage(String message) {
        String prefix = configManager != null ? configManager.getMessagePrefix() : "§7[§6Loup-Garou§7]";
        Bukkit.broadcastMessage(prefix + " " + message);
    }

    /**
     * Méthode utilitaire pour les messages de debug en mode verbose
     */
    public void debugMessage(String message) {
        if (configManager != null && configManager.isDebugEnabled()) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Recharge la configuration du plugin
     */
    public void reloadPluginConfig() {
        try {
            if (configManager != null) {
                configManager.reloadConfig();
                restartScoreboardUpdateTask();

                // Mettre à jour le scoreboard avec la nouvelle config
                if (scoreboardManager != null) {
                    scoreboardManager.updateScoreboard();
                }

                getLogger().info("✅ Configuration rechargée avec succès !");

                // Afficher les nouvelles valeurs
                getLogger().info("Nouvelles valeurs :");
                getLogger().info("  - Monde : " + configManager.getSpawnWorld());
                getLogger().info("  - Région : " + configManager.getRegionName());
                getLogger().info("  - WorldGuard : " + (configManager.isWorldGuardEnabled() ? "Activé" : "Désactivé"));
            }
        } catch (Exception e) {
            getLogger().severe("❌ Erreur lors du rechargement de la configuration : " + e.getMessage());
            e.printStackTrace();
        }
    }
}