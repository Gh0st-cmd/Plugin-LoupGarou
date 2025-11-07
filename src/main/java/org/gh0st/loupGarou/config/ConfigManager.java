package org.gh0st.loupGarou.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.gh0st.loupGarou.LoupGarouPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final LoupGarouPlugin plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(LoupGarouPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void createDefaultConfig() {
        // Créer le dossier si nécessaire
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Si le fichier n'existe pas, créer la config par défaut
        if (!configFile.exists()) {
            plugin.getLogger().info("Création du fichier de configuration par défaut...");
            config = new YamlConfiguration();
            setDefaultValues();
            saveConfig();
        } else {
            // Charger la config existante et ajouter les valeurs manquantes
            loadConfig();
            boolean modified = addMissingValues();
            if (modified) {
                plugin.getLogger().info("Nouvelles options ajoutées à la configuration.");
                saveConfig();
            }
        }

        loadConfig();
    }

    private void setDefaultValues() {
        // Configuration du jeu
        config.set("game.min-players", 4);
        config.set("game.max-players", 20);
        config.set("game.night-duration", 60);
        config.set("game.day-duration", 120);
        config.set("game.vote-duration", 60);
        config.set("game.auto-restart", true);
        config.set("game.restart-delay", 30);
        config.set("game.enable-mayor", true); // Activer le système de maire
        config.set("game.blindness-at-night", true); // Effet de cécité la nuit
        config.set("game.freeze-players-at-night", false); // Immobiliser les joueurs la nuit

        // Configuration du spawn
        config.set("spawn.world", "minijeux");
        config.set("spawn.x", 0.5);
        config.set("spawn.y", 100.0);
        config.set("spawn.z", 0.5);
        config.set("spawn.yaw", 0.0f);
        config.set("spawn.pitch", 0.0f);

        // Configuration WorldGuard
        config.set("worldguard.enabled", true);
        config.set("worldguard.region-name", "loupgarou");
        config.set("worldguard.check-on-join", true);
        config.set("worldguard.world-name", "minijeux");

        // Configuration des rôles
        config.set("roles.werewolf.ratio", 0.25);
        config.set("roles.seer.min-players", 6);
        config.set("roles.guard.min-players", 8);
        config.set("roles.witch.min-players", 10);

        // Messages personnalisables
        config.set("messages.prefix", "§7[§6Loup-Garou§7]");
        config.set("messages.game-start", "§a🎮 La partie commence !");
        config.set("messages.game-end", "§c🏁 Partie terminée !");
        config.set("messages.welcome", "§a👋 Bienvenue dans la zone Loup-Garou !");
        config.set("messages.not-in-region", "§c❌ Vous devez être dans la zone 'loupgarou' pour jouer !");
        config.set("messages.wrong-world", "§c❌ Le jeu Loup-Garou n'est disponible que dans le monde '{world}' !");

        // Configuration du scoreboard
        config.set("scoreboard.enabled", true);
        config.set("scoreboard.title", "§6§l🐺 LOUP-GAROU 🐺");
        config.set("scoreboard.update-interval", 20);
        config.set("scoreboard.only-in-region", true);

        // Configuration des sons
        config.set("sounds.enabled", true);
        config.set("sounds.volume", 1.0);
        config.set("sounds.pitch", 1.0);

        // Configuration des statistiques
        config.set("stats.enabled", true);
        config.set("stats.save-interval", 300);

        // Configuration de debug
        config.set("debug.enabled", false);
        config.set("debug.verbose", false);

        // Configuration du vérificateur de mises à jour
        config.set("update-checker.enabled", true);
        config.set("update-checker.check-interval", 3600); // En secondes (1 heure)
        config.set("update-checker.notify-admins", true); // Notifier les admins à la connexion
    }

    private boolean addMissingValues() {
        boolean modified = false;

        // Vérifier et ajouter les valeurs manquantes
        if (!config.contains("spawn.world")) {
            config.set("spawn.world", "minijeux");
            modified = true;
        }
        if (!config.contains("spawn.x")) {
            config.set("spawn.x", 0.5);
            modified = true;
        }
        if (!config.contains("spawn.y")) {
            config.set("spawn.y", 100.0);
            modified = true;
        }
        if (!config.contains("spawn.z")) {
            config.set("spawn.z", 0.5);
            modified = true;
        }
        if (!config.contains("spawn.yaw")) {
            config.set("spawn.yaw", 0.0f);
            modified = true;
        }
        if (!config.contains("spawn.pitch")) {
            config.set("spawn.pitch", 0.0f);
            modified = true;
        }
        if (!config.contains("worldguard.enabled")) {
            config.set("worldguard.enabled", true);
            modified = true;
        }
        if (!config.contains("worldguard.region-name")) {
            config.set("worldguard.region-name", "loupgarou");
            modified = true;
        }
        if (!config.contains("worldguard.check-on-join")) {
            config.set("worldguard.check-on-join", true);
            modified = true;
        }
        if (!config.contains("worldguard.world-name")) {
            config.set("worldguard.world-name", "minijeux");
            modified = true;
        }
        if (!config.contains("scoreboard.only-in-region")) {
            config.set("scoreboard.only-in-region", true);
            modified = true;
        }
        if (!config.contains("messages.not-in-region")) {
            config.set("messages.not-in-region", "§c❌ Vous devez être dans la zone 'loupgarou' pour jouer !");
            modified = true;
        }
        if (!config.contains("messages.wrong-world")) {
            config.set("messages.wrong-world", "§c❌ Le jeu Loup-Garou n'est disponible que dans le monde '{world}' !");
            modified = true;
        }
        if (!config.contains("update-checker.enabled")) {
            config.set("update-checker.enabled", true);
            modified = true;
        }
        if (!config.contains("update-checker.check-interval")) {
            config.set("update-checker.check-interval", 3600);
            modified = true;
        }
        if (!config.contains("update-checker.notify-admins")) {
            config.set("update-checker.notify-admins", true);
            modified = true;
        }

        return modified;
    }

    public void loadConfig() {
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("Configuration chargée depuis config.yml");
        } else {
            plugin.getLogger().warning("Fichier de configuration non trouvé, création en cours...");
            createDefaultConfig();
        }
    }

    public void saveConfig() {
        try {
            if (config != null && configFile != null) {
                // Ajouter des commentaires en en-tête
                String header = "# ================================\n" +
                        "# Configuration du plugin Loup-Garou\n" +
                        "# ================================\n" +
                        "# \n" +
                        "# spawn: Configuration du point de spawn du jeu\n" +
                        "# worldguard: Intégration avec WorldGuard (nécessite le plugin WorldGuard)\n" +
                        "# game: Paramètres de durée et nombre de joueurs\n" +
                        "# roles: Configuration des rôles spéciaux\n" +
                        "# \n" +
                        "# ⚠️ Redémarrez le serveur ou utilisez /lg reload après modification\n" +
                        "# ================================\n";

                config.options().header(header);
                config.save(configFile);
                plugin.getLogger().info("Configuration sauvegardée !");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde de la configuration : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("Configuration rechargée !");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // Getters pour la configuration du spawn
    public String getSpawnWorld() {
        return config.getString("spawn.world", "minijeux");
    }

    public double getSpawnX() {
        return config.getDouble("spawn.x", 0.5);
    }

    public double getSpawnY() {
        return config.getDouble("spawn.y", 100.0);
    }

    public double getSpawnZ() {
        return config.getDouble("spawn.z", 0.5);
    }

    public float getSpawnYaw() {
        return (float) config.getDouble("spawn.yaw", 0.0);
    }

    public float getSpawnPitch() {
        return (float) config.getDouble("spawn.pitch", 0.0);
    }

    public Location getSpawnLocation() {
        String worldName = getSpawnWorld();
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Le monde '" + worldName + "' n'existe pas ! Utilisation du monde par défaut.");
            world = Bukkit.getWorlds().get(0);
        }

        return new Location(world, getSpawnX(), getSpawnY(), getSpawnZ(), getSpawnYaw(), getSpawnPitch());
    }

    // Getters pour WorldGuard
    public boolean isWorldGuardEnabled() {
        return config.getBoolean("worldguard.enabled", true);
    }

    public String getRegionName() {
        return config.getString("worldguard.region-name", "loupgarou");
    }

    public boolean shouldCheckOnJoin() {
        return config.getBoolean("worldguard.check-on-join", true);
    }

    public String getWorldGuardWorld() {
        return config.getString("worldguard.world-name", "minijeux");
    }

    // Getters existants
    public int getMinPlayers() {
        return config.getInt("game.min-players", 4);
    }

    public int getMaxPlayers() {
        return config.getInt("game.max-players", 20);
    }

    public int getNightDuration() {
        return config.getInt("game.night-duration", 60);
    }

    public int getDayDuration() {
        return config.getInt("game.day-duration", 120);
    }

    public int getVoteDuration() {
        return config.getInt("game.vote-duration", 60);
    }

    public boolean isAutoRestart() {
        return config.getBoolean("game.auto-restart", true);
    }

    public int getRestartDelay() {
        return config.getInt("game.restart-delay", 30);
    }

    public double getWerewolfRatio() {
        return config.getDouble("roles.werewolf.ratio", 0.25);
    }

    public int getSeerMinPlayers() {
        return config.getInt("roles.seer.min-players", 6);
    }

    public int getGuardMinPlayers() {
        return config.getInt("roles.guard.min-players", 8);
    }

    public int getWitchMinPlayers() {
        return config.getInt("roles.witch.min-players", 10);
    }

    public String getMessagePrefix() {
        return config.getString("messages.prefix", "§7[§6Loup-Garou§7]");
    }

    public String getNotInRegionMessage() {
        return config.getString("messages.not-in-region", "§c❌ Vous devez être dans la zone 'loupgarou' pour jouer !");
    }

    public String getWrongWorldMessage() {
        return config.getString("messages.wrong-world", "§c❌ Le jeu Loup-Garou n'est disponible que dans le monde '{world}' !")
                .replace("{world}", getWorldGuardWorld());
    }

    public boolean isScoreboardEnabled() {
        return config.getBoolean("scoreboard.enabled", true);
    }

    public boolean isScoreboardOnlyInRegion() {
        return config.getBoolean("scoreboard.only-in-region", true);
    }

    public String getScoreboardTitle() {
        return config.getString("scoreboard.title", "§6§l🐺 LOUP-GAROU 🐺");
    }

    public int getScoreboardUpdateInterval() {
        return config.getInt("scoreboard.update-interval", 20);
    }

    public boolean areSoundsEnabled() {
        return config.getBoolean("sounds.enabled", true);
    }

    public float getSoundVolume() {
        return (float) config.getDouble("sounds.volume", 1.0);
    }

    public float getSoundPitch() {
        return (float) config.getDouble("sounds.pitch", 1.0);
    }

    public boolean areStatsEnabled() {
        return config.getBoolean("stats.enabled", true);
    }

    public int getStatsSaveInterval() {
        return config.getInt("stats.save-interval", 300);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean isVerboseDebug() {
        return config.getBoolean("debug.verbose", false);
    }

    public boolean isMayorEnabled() {
        return config.getBoolean("game.enable-mayor", true);
    }

    public boolean isBlindnessAtNightEnabled() {
        return config.getBoolean("game.blindness-at-night", true);
    }

    public boolean isFreezingAtNightEnabled() {
        return config.getBoolean("game.freeze-players-at-night", false);
    }
}