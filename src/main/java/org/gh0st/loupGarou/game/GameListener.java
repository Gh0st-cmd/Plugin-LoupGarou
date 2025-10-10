package org.gh0st.loupGarou.game;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.gh0st.loupGarou.*;
import org.gh0st.loupGarou.role.PlayerRole;
import org.gh0st.loupGarou.utils.UpdateChecker;
import org.gh0st.loupGarou.utils.Utils;
import org.gh0st.loupGarou.utils.extern.WorldGuardIntegration;

public class GameListener implements Listener {

    private final LoupGarouPlugin plugin;

    public GameListener(LoupGarouPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        WorldGuardIntegration wg = plugin.getWorldGuardIntegration();

        if (gm.isGameRunning()) {
            // Message d'accueil personnalisé
            player.sendMessage("§6" + "=".repeat(50));
            player.sendMessage(Utils.centerText("§6§l🐺 LOUP-GAROU 🐺", 50));
            player.sendMessage("§6" + "=".repeat(50));
            player.sendMessage("§e🎮 Une partie est en cours !");
            player.sendMessage("§7⏳ Vous pourrez participer à la prochaine partie.");
            player.sendMessage("§e💡 Utilisez §f/lg aide §epour voir les commandes");
            player.sendMessage("§6" + "=".repeat(50));

            // NE PAS changer le mode de jeu si le joueur n'est pas dans la partie
            if (!gm.getPlayers().containsKey(player.getUniqueId())) {
                // Le joueur n'est pas dans la partie, ne rien faire
                event.setJoinMessage("§7[§a+§7] §e" + player.getName() + " §7observe la partie en cours");
            }
        } else {
            // Message d'accueil normal
            event.setJoinMessage("§7[§a+§7] §e" + player.getName() + " §7a rejoint le serveur");

            // Vérifier si le joueur est dans la bonne région (uniquement si configuré)
            if (plugin.getConfigManager().shouldCheckOnJoin() && wg.isWorldGuardAvailable()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (wg.canPlayerPlay(player)) {
                        player.sendMessage("§a👋 Bienvenue dans la zone Loup-Garou !");
                        player.sendMessage("§e💡 Utilisez §f/lg aide §epour commencer");

                        // Afficher le scoreboard seulement si dans la région
                        if (plugin.getConfigManager().isScoreboardOnlyInRegion()) {
                            plugin.getScoreboardManager().updateScoreboard();
                        }
                    }
                }, 20L);
            } else {
                player.sendMessage("§a👋 Bienvenue ! Utilisez §f/lg aide §apour commencer");

                // Mettre à jour le scoreboard
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getScoreboardManager().updateScoreboard();
                }, 20L);
            }
        }

        // Vérifier les mises à jour pour les admins
        if (player.hasPermission("loupgarou.admin")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                UpdateChecker checker = plugin.getUpdateChecker();
                if (checker != null && checker.isUpdateAvailable()) {
                    checker.notifyPlayer(player);
                }
            }, 60L); // Notifier après 3 secondes
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning() && gm.getPlayers().containsKey(player.getUniqueId())) {
            PlayerRole role = gm.getPlayers().get(player.getUniqueId());

            event.setQuitMessage("§7[§c-§7] §e" + player.getName() + " §7a quitté la partie");

            // Informer si c'est un rôle important
            if (role != null && role != PlayerRole.VILLAGER) {
                String roleName = role.getDisplayName();
                Bukkit.broadcastMessage("§c⚠️ Un joueur avec un rôle important a quitté !");

                // Informer les admins du rôle exact
                for (Player admin : Bukkit.getOnlinePlayers()) {
                    if (admin.hasPermission("loupgarou.admin")) {
                        admin.sendMessage("§7[Admin] " + player.getName() + " était : " + roleName);
                    }
                }
            }

            // Vérifier les conditions de victoire après un délai
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (gm.isGameRunning()) {
                    gm.checkWinConditionsAfterLeave();
                    plugin.getScoreboardManager().updateScoreboard();
                }
            }, 20L); // 1 seconde de délai
        } else {
            event.setQuitMessage("§7[§c-§7] §e" + player.getName() + " §7a quitté le serveur");
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        // NE PAS TOUCHER au chat si aucune partie n'est en cours
        // Cela permet à LuckPerms et autres plugins de gérer les préfixes
        if (!gm.isGameRunning()) {
            return; // Laisser le format par défaut (LuckPerms, etc.)
        }

        PlayerRole role = gm.getPlayers().get(player.getUniqueId());
        if (role == null) {
            // Joueur non inscrit dans la partie - Garder le format par défaut
            return;
        }

        String message = event.getMessage();

        // Chat des morts (avec ! au début)
        if (!gm.isPlayerAlive(player)) {
            event.setCancelled(true);

            String deadMessage;
            if (message.startsWith("!")) {
                deadMessage = "§8💀 [Morts] §7" + player.getName() + " §8» §7" + message.substring(1);
            } else {
                deadMessage = "§8💀 [Morts] §7" + player.getName() + " §8» §7" + message;
            }

            // Envoyer aux morts et spectateurs
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!gm.isPlayerAlive(p) || p.hasPermission("loupgarou.admin")) {
                    p.sendMessage(deadMessage);
                }
            }
            return;
        }

        // Chat des loups-garous pendant la nuit (avec ! au début)
        if (gm.getCurrentState() == GameManager.GameState.NIGHT &&
                role == PlayerRole.WEREWOLF && message.startsWith("!")) {

            event.setCancelled(true);
            String werewolfMessage = "§c🐺 [Loups] §c" + player.getName() + " §8» §c" + message.substring(1);

            // Envoyer seulement aux loups-garous vivants
            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerRole pRole = gm.getPlayers().get(p.getUniqueId());
                if (pRole == PlayerRole.WEREWOLF && gm.isPlayerAlive(p)) {
                    p.sendMessage(werewolfMessage);
                }
            }
            return;
        }

        // Chat normal pendant le jeu avec indication du statut
        String prefix = gm.isPlayerAlive(player) ? "§a👤" : "§8💀";

        // Format selon la phase
        switch (gm.getCurrentState()) {
            case NIGHT:
                if (gm.isPlayerAlive(player)) {
                    event.setFormat(prefix + " §7%s §8» §7%s §8(La nuit, chuchotez...)");
                } else {
                    event.setFormat("§8💀 %s §8» %s");
                }
                break;
            case DAY:
                event.setFormat(prefix + " §e%s §8» §f%s");
                break;
            case VOTE:
                event.setFormat(prefix + " §c%s §8» §f%s §7(Phase de vote)");
                break;
            default:
                event.setFormat(prefix + " §f%s §8» §f%s");
        }
    }

    // Empêcher les dégâts pendant le jeu (plus efficace que PlayerDeathEvent)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning()) {
            // Annuler tous les dégâts pendant une partie
            event.setCancelled(true);

            // Message d'information pour le joueur
            if (event.getCause() != EntityDamageEvent.DamageCause.CUSTOM) {
                player.sendMessage("§c⚠️ Vous ne pouvez pas subir de dégâts pendant une partie de Loup-Garou !");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning()) {
            Player player = event.getEntity();

            // Empêcher la mort et remettre la santé au maximum
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    player.setSaturation(20);
                    player.setFireTicks(0);

                    // Ne pas faire tomber d'objets
                    event.getDrops().clear();
                    event.setDroppedExp(0);

                    // Message d'information
                    player.sendMessage("§c⚠️ Vous ne pouvez pas mourir pendant une partie de Loup-Garou !");
                    player.sendMessage("§e💡 Seules les actions de jeu peuvent vous éliminer !");
                }
            });

            // Empêcher le message de mort
            event.setDeathMessage("");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning()) {
            Player player = event.getPlayer();

            // Téléporter au spawn de la partie si nécessaire
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    // Si le joueur n'est pas vivant dans le jeu, le mettre en spectateur
                    if (!gm.isPlayerAlive(player) && gm.getPlayers().containsKey(player.getUniqueId())) {
                        player.setGameMode(GameMode.SPECTATOR);
                        player.sendMessage("§7👻 Vous êtes en mode spectateur.");
                    } else {
                        // Sinon, le remettre en mode aventure
                        player.setGameMode(GameMode.ADVENTURE);
                    }

                    // Remettre la santé complète
                    player.setHealth(player.getMaxHealth());
                    player.setFoodLevel(20);
                    player.setSaturation(20);
                }
            }, 5L); // Petit délai pour éviter les conflits
        }
    }

    // Événement pour gérer les tentatives de changement de gamemode pendant le jeu
    @EventHandler(priority = EventPriority.HIGH)
    public void onGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning() && gm.getPlayers().containsKey(player.getUniqueId())) {
            GameMode newMode = event.getNewGameMode();

            // Autoriser seulement les changements logiques
            if (gm.isPlayerAlive(player)) {
                // Joueur vivant : Adventure seulement
                if (newMode != GameMode.ADVENTURE && !player.hasPermission("loupgarou.admin")) {
                    event.setCancelled(true);
                    player.sendMessage("§c⚠️ Vous ne pouvez pas changer de mode de jeu pendant une partie !");
                }
            } else {
                // Joueur mort : Spectator seulement
                if (newMode != GameMode.SPECTATOR && !player.hasPermission("loupgarou.admin")) {
                    event.setCancelled(true);
                    player.sendMessage("§c⚠️ Les joueurs morts doivent rester en mode spectateur !");
                }
            }
        }
    }

    // Empêcher les interactions avec les blocs pendant le jeu
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning() && !event.getPlayer().hasPermission("loupgarou.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c⚠️ Vous ne pouvez pas casser de blocs pendant une partie !");
        }
    }

    @EventHandler
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning() && !event.getPlayer().hasPermission("loupgarou.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c⚠️ Vous ne pouvez pas placer de blocs pendant une partie !");
        }
    }

    // Empêcher l'utilisation d'objets non autorisés
    @EventHandler
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        GameManager gm = plugin.getGameManager();
        Player player = event.getPlayer();

        if (gm.isGameRunning() && gm.getPlayers().containsKey(player.getUniqueId())) {
            // Autoriser seulement les interactions de base (pas d'ouverture de coffres, etc.)
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                org.bukkit.Material material = event.getClickedBlock().getType();

                // Bloquer l'interaction avec certains blocs
                if (material == org.bukkit.Material.CHEST ||
                        material == org.bukkit.Material.BARREL ||
                        material == org.bukkit.Material.FURNACE ||
                        material == org.bukkit.Material.CRAFTING_TABLE) {

                    if (!player.hasPermission("loupgarou.admin")) {
                        event.setCancelled(true);
                        player.sendMessage("§c⚠️ Cette interaction n'est pas autorisée pendant une partie !");
                    }
                }
            }
        }
    }

    // Empêcher la perte/récupération d'objets
    @EventHandler
    public void onItemDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning() && !event.getPlayer().hasPermission("loupgarou.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c⚠️ Vous ne pouvez pas jeter d'objets pendant une partie !");
        }
    }

    @EventHandler
    public void onItemPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        GameManager gm = plugin.getGameManager();
        Player player = (Player) event.getEntity();

        if (gm.isGameRunning() && !player.hasPermission("loupgarou.admin")) {
            event.setCancelled(true);
        }
    }
}