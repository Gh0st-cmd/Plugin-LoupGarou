package org.gh0st.loupGarou;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

/**
 * Classe de gestion de l'intégration avec WorldGuard
 * Permet de vérifier si un joueur est dans une région spécifique
 */
public class WorldGuardIntegration {

    private final LoupGarouPlugin plugin;
    private boolean worldGuardAvailable = false;

    public WorldGuardIntegration(LoupGarouPlugin plugin) {
        this.plugin = plugin;
        checkWorldGuard();
    }

    /**
     * Vérifie si WorldGuard est disponible sur le serveur
     */
    private void checkWorldGuard() {
        Plugin worldGuardPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");

        if (worldGuardPlugin != null && worldGuardPlugin.isEnabled()) {
            worldGuardAvailable = true;
            plugin.getLogger().info("✅ WorldGuard détecté et activé !");
        } else {
            worldGuardAvailable = false;
            plugin.getLogger().warning("⚠️ WorldGuard non détecté. Les fonctions de région sont désactivées.");
            plugin.getLogger().warning("⚠️ Installez WorldGuard pour utiliser la restriction par région.");
        }
    }

    /**
     * Vérifie si WorldGuard est disponible
     */
    public boolean isWorldGuardAvailable() {
        return worldGuardAvailable;
    }

    /**
     * Vérifie si un joueur est dans une région spécifique
     * @param player Le joueur à vérifier
     * @param regionName Le nom de la région
     * @return true si le joueur est dans la région
     */
    public boolean isPlayerInRegion(Player player, String regionName) {
        if (!worldGuardAvailable || !plugin.getConfigManager().isWorldGuardEnabled()) {
            return true; // Si WorldGuard n'est pas disponible, on considère que le joueur est "dans la région"
        }

        try {
            Location loc = player.getLocation();

            // Vérifier le monde
            if (!loc.getWorld().getName().equals(plugin.getConfigManager().getWorldGuardWorld())) {
                return false;
            }

            // Obtenir les régions à la position du joueur
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            com.sk89q.worldedit.util.Location wgLoc = BukkitAdapter.adapt(loc);
            ApplicableRegionSet regions = query.getApplicableRegions(wgLoc);

            // Vérifier si la région spécifiée est présente
            for (ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase(regionName)) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la vérification de la région : " + e.getMessage());
            return true; // En cas d'erreur, on laisse passer
        }
    }

    /**
     * Vérifie si un joueur est dans la bonne région pour jouer
     * @param player Le joueur à vérifier
     * @return true si le joueur peut jouer (dans la région ou WorldGuard désactivé)
     */
    public boolean canPlayerPlay(Player player) {
        if (!plugin.getConfigManager().isWorldGuardEnabled()) {
            return true;
        }

        String regionName = plugin.getConfigManager().getRegionName();
        return isPlayerInRegion(player, regionName);
    }

    /**
     * Vérifie si le joueur est dans le bon monde
     * @param player Le joueur à vérifier
     * @return true si le joueur est dans le bon monde
     */
    public boolean isPlayerInCorrectWorld(Player player) {
        String requiredWorld = plugin.getConfigManager().getWorldGuardWorld();
        return player.getWorld().getName().equals(requiredWorld);
    }

    /**
     * Envoie un message d'erreur au joueur s'il n'est pas dans la bonne zone
     * @param player Le joueur
     */
    public void sendNotInRegionMessage(Player player) {
        if (!isPlayerInCorrectWorld(player)) {
            player.sendMessage(plugin.getConfigManager().getMessagePrefix() + " " +
                    plugin.getConfigManager().getWrongWorldMessage());
        } else if (!canPlayerPlay(player)) {
            player.sendMessage(plugin.getConfigManager().getMessagePrefix() + " " +
                    plugin.getConfigManager().getNotInRegionMessage());
        }
    }
}