package org.gh0st.loupGarou;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;

/**
 * Classe pour vérifier les mises à jour du plugin depuis GitHub
 */
public class UpdateChecker {

    private final LoupGarouPlugin plugin;
    private final String currentVersion;
    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable = false;
    private boolean checkFailed = false;

    // URL de l'API GitHub pour les releases
    // Format : https://api.github.com/repos/USERNAME/REPO/releases/latest
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Gh0st-cmd/Plugin-LoupGarou/releases/latest";

    public UpdateChecker(LoupGarouPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }

    /**
     * Vérifie les mises à jour de manière asynchrone (le cœur de la logique)
     */
    public void checkForUpdates() {
        if (!plugin.getConfigManager().getConfig().getBoolean("update-checker.enabled", true)) {
            plugin.getLogger().info("Vérification des mises à jour désactivée dans la configuration.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    plugin.getLogger().info("Vérification des mises à jour...");

                    URI uri = new URI(GITHUB_API_URL);
                    URL url = uri.toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    int responseCode = connection.getResponseCode();

                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream())
                        );
                        StringBuilder response = new StringBuilder();
                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        // Parser la réponse JSON
                        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                        latestVersion = jsonObject.get("tag_name").getAsString().replace("v", "");
                        downloadUrl = jsonObject.get("html_url").getAsString();

                        // Comparer les versions
                        if (isNewerVersion(latestVersion, currentVersion)) {
                            updateAvailable = true;
                            checkFailed = false;

                            // Notifier dans la console (doit être fait de manière synchrone, mais l'appel de log est sûr)
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    notifyConsole();
                                }
                            }.runTask(plugin);

                        } else {
                            updateAvailable = false;
                            checkFailed = false;
                            plugin.getLogger().info("✅ Le plugin est à jour ! (Version: " + currentVersion + ")");
                        }

                    } else {
                        checkFailed = true;
                        plugin.getLogger().warning("⚠️ Impossible de vérifier les mises à jour (Code HTTP: " + responseCode + ")");
                    }

                } catch (Exception e) {
                    checkFailed = true;
                    plugin.getLogger().log(Level.WARNING, "❌ Erreur lors de la vérification des mises à jour: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Compare deux versions (format: X.Y.Z)
     * @return true si newVersion est plus récente que currentVersion
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        String[] newParts = newVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        int length = Math.max(newParts.length, currentParts.length);

        for (int i = 0; i < length; i++) {
            int newPart = i < newParts.length ? parseVersionPart(newParts[i]) : 0;
            int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;

            if (newPart > currentPart) {
                return true;
            } else if (newPart < currentPart) {
                return false;
            }
        }

        return false;
    }

    /**
     * Parse une partie de version (gère les versions comme "1.0.0-SNAPSHOT")
     */
    private int parseVersionPart(String part) {
        try {
            // Enlever tout ce qui n'est pas un nombre
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Notifie dans la console qu'une mise à jour est disponible
     */
    private void notifyConsole() {
        plugin.getLogger().info("╔════════════════════════════════════════════╗");
        plugin.getLogger().info("║                                            ║");
        plugin.getLogger().info("║  🆕 NOUVELLE VERSION DISPONIBLE ! 🆕         ║");
        plugin.getLogger().info("║                                           ║");
        plugin.getLogger().info("║  Version actuelle : " + String.format("%-20s", currentVersion) + "  ║");
        plugin.getLogger().info("║  Nouvelle version : " + String.format("%-20s", latestVersion) + "  ║");
        plugin.getLogger().info("║                                           ║");
        plugin.getLogger().info("║  📥 Télécharger sur GitHub :                ║");
        plugin.getLogger().info("║  " + String.format("%-42s", downloadUrl) + "║");
        plugin.getLogger().info("║                                            ║");
        plugin.getLogger().info("╚════════════════════════════════════════════╝");
    }

    /**
     * Notifie un joueur qu'une mise à jour est disponible
     */
    public void notifyPlayer(Player player) {
        if (!updateAvailable) {
            return;
        }

        if (!player.hasPermission("loupgarou.admin")) {
            return;
        }

        player.sendMessage("");
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l    MISE À JOUR DISPONIBLE !     ");
        player.sendMessage("§6§l╚══════════════════════════════╝");
        player.sendMessage("");
        player.sendMessage("§e📦 Plugin Loup-Garou");
        player.sendMessage("§7   Version actuelle : §f" + currentVersion);
        player.sendMessage("§a   Nouvelle version : §f" + latestVersion);
        player.sendMessage("");
        player.sendMessage("§7   Cliquez pour ouvrir :");

        // Message cliquable
        net.kyori.adventure.text.Component downloadMessage = net.kyori.adventure.text.Component.text()
                .append(net.kyori.adventure.text.Component.text("   [", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text("Télécharger", net.kyori.adventure.text.format.NamedTextColor.GREEN)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.UNDERLINED)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(downloadUrl))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                net.kyori.adventure.text.Component.text("Cliquez pour ouvrir GitHub", net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                        )))
                .append(net.kyori.adventure.text.Component.text("]", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                .build();

        player.sendMessage(downloadMessage);
        player.sendMessage("");
    }

    /**
     * Envoie un message simple au joueur
     */
    public void notifyPlayerSimple(Player player) {
        if (!updateAvailable) {
            return;
        }

        if (!player.hasPermission("loupgarou.admin")) {
            return;
        }

        player.sendMessage("§6§l[Loup-Garou] §e🆕 Une nouvelle version est disponible ! §7(v" + latestVersion + ")");
        player.sendMessage("§6§l[Loup-Garou] §7Téléchargez-la sur : §b" + downloadUrl);
    }

    /**
     * Vérifie périodiquement les mises à jour (toutes les heures).
     * Effectue la première vérification immédiatement.
     */
    public void startPeriodicCheck() {
        if (!plugin.getConfigManager().getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        long interval = plugin.getConfigManager().getConfig().getLong("update-checker.check-interval", 3600) * 20L; // Convertir en ticks

        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
            // FIX : Passage de 20L à 0L pour exécuter la première vérification immédiatement
        }.runTaskTimerAsynchronously(plugin, 0L, interval);
    }

    // Getters
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public boolean hasCheckFailed() {
        return checkFailed;
    }
}
