package org.gh0st.loupGarou.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.gh0st.loupGarou.*;
import org.gh0st.loupGarou.config.Messages;
import org.gh0st.loupGarou.game.GameManager;
import org.gh0st.loupGarou.role.PlayerRole;
import org.gh0st.loupGarou.utils.UpdateChecker;
import org.gh0st.loupGarou.utils.Utils;
import org.gh0st.loupGarou.utils.extern.BStatsManager;

import java.util.*;

/**
 * Commande principale du plugin Loup-Garou.
 *
 * <p>
 * Cette classe gère toutes les commandes commençant par <code>/lg</code> et
 * redirige vers les sous-commandes appropriées. Elle fournit également la
 * complétion automatique pour les commandes du plugin.
 *
 * <p>
 * Les ajouts récents incluent la gestion du transfert de rôle de maire. Lorsqu'un
 * maire meurt, il peut désigner un successeur via <code>/lg maire &lt;joueur&gt;</code>. Cette
 * commande est réservée au maire décédé et ne fonctionne que si le joueur
 * concerné est toujours considéré comme le maire par le {@link GameManager}.
 */
public class LoupGarouCommand implements CommandExecutor, TabCompleter {

    private final LoupGarouPlugin plugin;

    public LoupGarouCommand(LoupGarouPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && plugin.getBStatsManager() != null) {
            plugin.getBStatsManager().recordCommandUsage(args[0].toLowerCase());
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
            case "commencer":
                return handleStart(sender);

            case "stop":
            case "arreter":
                return handleStop(sender);

            case "status":
            case "statut":
                return handleStatus(sender);

            case "stats":
            case "statistiques":
                return handleStats(sender);

            case "reload":
            case "recharger":
                return handleReload(sender);

            case "setspawn":
                return handleSetSpawn(sender);

            case "update":
            case "version":
                return handleUpdate(sender);

            case "bstats":
                return handleBStats(sender);

            case "kill":
            case "tuer":
                return handleKill(sender, args);

            case "see":
            case "voir":
                return handleSee(sender, args);

            case "guard":
            case "proteger":
                return handleGuard(sender, args);

            case "heal":
            case "soigner":
                return handleHeal(sender, args);

            case "poison":
            case "empoisonner":
                return handlePoison(sender, args);

            case "list":
            case "liste":
                return handleList(sender);

            case "help":
            case "aide":
                showHelp(sender);
                return true;

            // ← NOUVEAU : désignation du nouveau maire
            case "maire":
            case "mayor":
                return handleMayor(sender, args);

            default:
                sender.sendMessage("§c❌ Commande inconnue. Utilisez /lg aide pour voir les commandes.");
                return true;
        }
    }

    /**
     * Démarre la partie si toutes les conditions sont réunies.
     */
    private boolean handleStart(CommandSender sender) {
        if (!sender.hasPermission("loupgarou.admin")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        GameManager gm = plugin.getGameManager();
        if (gm.getCurrentState() != GameManager.GameState.WAITING) {
            sender.sendMessage("§c❌ Une partie est déjà en cours ! Utilisez /lg stop pour l'arrêter.");
            return true;
        }

        if (gm.startGame()) {
            sender.sendMessage("§a✅ Partie démarrée avec succès !");
        } else {
            sender.sendMessage("§c❌ Impossible de démarrer la partie.");
        }
        return true;
    }

    /**
     * Arrête la partie en cours.
     */
    private boolean handleStop(CommandSender sender) {
        if (!sender.hasPermission("loupgarou.admin")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        GameManager gm = plugin.getGameManager();
        if (!gm.isGameRunning()) {
            sender.sendMessage("§c❌ Aucune partie en cours.");
            return true;
        }

        gm.stopGame();
        sender.sendMessage("§a✅ Partie arrêtée.");
        return true;
    }

    /**
     * Affiche le statut de la partie au joueur.
     */
    private boolean handleStatus(CommandSender sender) {
        GameManager gm = plugin.getGameManager();
        sender.sendMessage("§6📊 === STATUT DE LA PARTIE ===");
        sender.sendMessage("§e🎮 État : " + gm.getCurrentState().getDisplayName());
        sender.sendMessage("§e👥 Joueurs connectés : " + Bukkit.getOnlinePlayers().size());

        if (gm.isGameRunning()) {
            sender.sendMessage("§e🌅 Jour actuel : " + gm.getDayNumber());
            sender.sendMessage("§e👤 Joueurs vivants : " + gm.getAlivePlayersCount());
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerRole role = gm.getPlayers().get(player.getUniqueId());
            if (role != null) {
                sender.sendMessage("§e🎭 Votre rôle : " + role.getDisplayName());
            }
        }
        return true;
    }

    /**
     * Affiche les statistiques du joueur.
     */
    private boolean handleStats(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        plugin.getStatsManager().showStats(player);
        return true;
    }

    /**
     * Recharge la configuration du plugin.
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("loupgarou.admin")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        GameManager gm = plugin.getGameManager();
        if (gm.isGameRunning()) {
            sender.sendMessage("§c❌ Impossible de recharger la configuration pendant une partie !");
            sender.sendMessage("§e💡 Arrêtez d'abord la partie avec /lg stop");
            return true;
        }

        sender.sendMessage("§e⏳ Rechargement de la configuration...");

        try {
            // Recharger la configuration
            plugin.getConfigManager().reloadConfig();

            // Redémarrer la tâche du scoreboard avec les nouveaux paramètres
            plugin.restartScoreboardUpdateTask();

            // Mettre à jour le scoreboard
            plugin.getScoreboardManager().updateScoreboard();

            sender.sendMessage("§a✅ Configuration rechargée avec succès !");
            sender.sendMessage("§7Monde: §f" + plugin.getConfigManager().getSpawnWorld());
            sender.sendMessage("§7Région: §f" + plugin.getConfigManager().getRegionName());
            sender.sendMessage("§7WorldGuard: " + (plugin.getConfigManager().isWorldGuardEnabled() ? "§a✅ Activé" : "§c❌ Désactivé"));

        } catch (Exception e) {
            sender.sendMessage("§c❌ Erreur lors du rechargement : " + e.getMessage());
            plugin.getLogger().severe("Erreur lors du rechargement de la configuration :");
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Définit le point de spawn utilisé pour les parties.
     */
    private boolean handleSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!sender.hasPermission("loupgarou.admin")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        // Vérifier que le monde existe
        if (loc.getWorld() == null) {
            player.sendMessage("§c❌ Erreur : monde invalide !");
            return true;
        }

        // Sauvegarder la position dans la config
        plugin.getConfigManager().getConfig().set("spawn.world", loc.getWorld().getName());
        plugin.getConfigManager().getConfig().set("spawn.x", loc.getX());
        plugin.getConfigManager().getConfig().set("spawn.y", loc.getY());
        plugin.getConfigManager().getConfig().set("spawn.z", loc.getZ());
        plugin.getConfigManager().getConfig().set("spawn.yaw", (double) loc.getYaw());
        plugin.getConfigManager().getConfig().set("spawn.pitch", (double) loc.getPitch());
        plugin.getConfigManager().saveConfig();

        player.sendMessage("§a✅ Point de spawn défini à votre position actuelle !");
        player.sendMessage("§7Monde: §f" + loc.getWorld().getName());
        player.sendMessage("§7Coordonnées: §f" + String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ()));
        player.sendMessage("§e💡 Les joueurs seront téléportés ici au début des parties");

        return true;
    }

    /**
     * Vérifie les mises à jour du plugin.
     */
    private boolean handleUpdate(CommandSender sender) {
        if (!sender.hasPermission("loupgarou.admin")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        UpdateChecker checker = plugin.getUpdateChecker();

        if (checker == null) {
            sender.sendMessage("§c❌ Le vérificateur de mises à jour n'est pas disponible.");
            return true;
        }

        sender.sendMessage("§e⏳ Vérification des mises à jour...");

        // Vérifier de manière asynchrone
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            checker.checkForUpdates();

            // Attendre un peu que la vérification se termine
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (checker.isUpdateAvailable()) {
                    sender.sendMessage("§a✅ Une nouvelle version est disponible !");
                    sender.sendMessage("§7   Version actuelle : §f" + checker.getCurrentVersion());
                    sender.sendMessage("§a   Nouvelle version : §f" + checker.getLatestVersion());
                    sender.sendMessage("§e📥 Télécharger : §b" + checker.getDownloadUrl());

                    if (sender instanceof Player) {
                        checker.notifyPlayer((Player) sender);
                    }
                } else if (checker.hasCheckFailed()) {
                    sender.sendMessage("§c❌ Impossible de vérifier les mises à jour.");
                    sender.sendMessage("§7Vérifiez votre connexion internet ou réessayez plus tard.");
                } else {
                    sender.sendMessage("§a✅ Vous utilisez la dernière version ! §7(v" + checker.getCurrentVersion() + ")");
                }
            }, 40L); // Attendre 2 secondes
        });

        return true;
    }

    /**
     * Affiche les statistiques bStats.
     */
    private boolean handleBStats(CommandSender sender) {
        if (!sender.hasPermission("loupgarou.admin")) {
            sender.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        BStatsManager bStats = plugin.getBStatsManager();

        if (bStats == null || !bStats.isEnabled()) {
            sender.sendMessage("§c❌ bStats n'est pas activé ou disponible.");
            sender.sendMessage("§e💡 Vérifiez que le plugin a été compilé avec bStats.");
            return true;
        }

        sender.sendMessage("§6" + "=".repeat(50));
        sender.sendMessage(Utils.centerText("§6§l📊 STATISTIQUES BSTATS 📊", 50));
        sender.sendMessage("§6" + "=".repeat(50));
        sender.sendMessage("");
        sender.sendMessage("§a✅ bStats est actif et fonctionnel !");
        sender.sendMessage("");
        sender.sendMessage("§e📊 Consultez les statistiques complètes sur :");
        sender.sendMessage("§b   https://bstats.org/plugin/bukkit/loup-garou");
        sender.sendMessage("");
        sender.sendMessage("§7📈 Les statistiques incluent :");
        sender.sendMessage("§7  • Nombre de serveurs utilisant le plugin");
        sender.sendMessage("§7  • Nombre de parties jouées");
        sender.sendMessage("§7  • Rôles les plus populaires");
        sender.sendMessage("§7  • Taux de victoire Village vs Loups");
        sender.sendMessage("§7  • Durée moyenne des parties");
        sender.sendMessage("§7  • Commandes les plus utilisées");
        sender.sendMessage("§7  • Et bien plus...");
        sender.sendMessage("");
        sender.sendMessage("§e💡 Les données sont anonymes et conformes au RGPD");
        sender.sendMessage("§7   Les joueurs peuvent désactiver bStats dans");
        sender.sendMessage("§7   plugins/bStats/config.yml");
        sender.sendMessage("§6" + "=".repeat(50));

        // Afficher les stats de session actuelle en console
        sender.sendMessage("");
        sender.sendMessage("§e📊 Statistiques de cette session :");
        bStats.printSessionStats();

        return true;
    }

    /**
     * Gère la commande du loup-garou pour tuer une cible pendant la nuit.
     */
    private boolean handleKill(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();

        PlayerRole role = gm.getPlayers().get(player.getUniqueId());
        if (role != PlayerRole.WEREWOLF) {
            player.sendMessage("§c❌ Seuls les loups-garous peuvent utiliser cette commande.");
            return true;
        }

        if (gm.getCurrentState() != GameManager.GameState.NIGHT) {
            player.sendMessage("§c❌ Vous ne pouvez tuer que pendant la nuit.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c❌ Usage : /lg tuer <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (!gm.isPlayerAlive(target)) {
            player.sendMessage("§c❌ Ce joueur est déjà mort.");
            return true;
        }

        if (gm.getPlayers().get(target.getUniqueId()) == PlayerRole.WEREWOLF) {
            player.sendMessage("§c❌ Vous ne pouvez pas tuer un autre loup-garou.");
            return true;
        }

        if (gm.addVote(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage("§c🐺 Vous avez choisi de tuer " + target.getName() + " !");

            // Informer les autres loups-garous
            for (Player werewolf : Bukkit.getOnlinePlayers()) {
                PlayerRole werewolfRole = gm.getPlayers().get(werewolf.getUniqueId());
                if (werewolfRole == PlayerRole.WEREWOLF && !werewolf.equals(player) && gm.isPlayerAlive(werewolf)) {
                    werewolf.sendMessage("§c🐺 " + player.getName() + " a voté pour tuer " + target.getName());
                }
            }
        } else {
            player.sendMessage("§c❌ Impossible de voter pour ce joueur.");
        }

        return true;
    }

    /**
     * Gère la commande de la voyante permettant de voir le rôle d'un joueur.
     */
    private boolean handleSee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();

        PlayerRole role = gm.getPlayers().get(player.getUniqueId());
        if (role != PlayerRole.SEER) {
            player.sendMessage("§c❌ Seule la voyante peut utiliser cette commande.");
            return true;
        }

        if (gm.getCurrentState() != GameManager.GameState.NIGHT) {
            player.sendMessage("§c❌ Vous ne pouvez voir que pendant la nuit.");
            return true;
        }

        if (gm.hasSeerUsedPower(player)) {
            player.sendMessage("§c❌ Vous avez déjà utilisé votre pouvoir cette nuit.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c❌ Usage : /lg voir <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§c❌ Vous ne pouvez pas vous voir vous-même.");
            return true;
        }

        if (!gm.isPlayerAlive(target)) {
            player.sendMessage("§c❌ Vous ne pouvez voir que les joueurs vivants.");
            return true;
        }

        PlayerRole targetRole = gm.getPlayers().get(target.getUniqueId());
        if (targetRole != null) {
            player.sendMessage("§5🔮 Vision révélée : " + target.getName() + " est " + targetRole.getDisplayName());
            gm.setSeerUsedPower(player);
        } else {
            player.sendMessage("§c❌ Ce joueur ne participe pas à la partie.");
        }

        return true;
    }

    /**
     * Gère la commande du garde pour protéger un joueur.
     */
    private boolean handleGuard(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();

        PlayerRole role = gm.getPlayers().get(player.getUniqueId());
        if (role != PlayerRole.GUARD) {
            player.sendMessage("§c❌ Seul le garde peut utiliser cette commande.");
            return true;
        }

        if (gm.getCurrentState() != GameManager.GameState.NIGHT) {
            player.sendMessage("§c❌ Vous ne pouvez protéger que pendant la nuit.");
            return true;
        }

        if (gm.hasGuardUsedPower(player)) {
            player.sendMessage("§c❌ Vous avez déjà utilisé votre pouvoir cette nuit.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c❌ Usage : /lg proteger <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (!gm.isPlayerAlive(target)) {
            player.sendMessage("§c❌ Vous ne pouvez protéger que les joueurs vivants.");
            return true;
        }

        if (target.equals(player) && gm.hasGuardProtectedHimself(player)) {
            player.sendMessage("§c❌ Vous ne pouvez vous protéger qu'une seule fois par partie.");
            return true;
        }

        gm.setProtectedPlayer(target.getUniqueId());
        gm.setGuardUsedPower(player);

        if (target.equals(player)) {
            gm.setGuardProtectedHimself(player);
            player.sendMessage("§b🛡️ Vous vous protégez cette nuit !");
        } else {
            player.sendMessage("§b🛡️ Vous protégez " + target.getName() + " cette nuit !");
        }

        return true;
    }

    /**
     * Gère la commande de la sorcière pour soigner un joueur.
     */
    private boolean handleHeal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();

        PlayerRole role = gm.getPlayers().get(player.getUniqueId());
        if (role != PlayerRole.WITCH) {
            player.sendMessage("§c❌ Seule la sorcière peut utiliser cette commande.");
            return true;
        }

        if (gm.getCurrentState() != GameManager.GameState.NIGHT) {
            player.sendMessage("§c❌ Vous ne pouvez soigner que pendant la nuit.");
            return true;
        }

        if (gm.hasWitchUsedHealPotion(player)) {
            player.sendMessage("§c❌ Vous avez déjà utilisé votre potion de soin !");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c❌ Usage : /lg soigner <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        gm.setWitchHealTarget(target.getUniqueId());
        gm.setWitchUsedHealPotion(player);

        player.sendMessage("§d🧪 Vous avez utilisé votre potion de soin sur " + target.getName() + " !");

        return true;
    }

    /**
     * Gère la commande de la sorcière pour empoisonner un joueur.
     */
    private boolean handlePoison(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();

        PlayerRole role = gm.getPlayers().get(player.getUniqueId());
        if (role != PlayerRole.WITCH) {
            player.sendMessage("§c❌ Seule la sorcière peut utiliser cette commande.");
            return true;
        }

        if (gm.getCurrentState() != GameManager.GameState.NIGHT) {
            player.sendMessage("§c❌ Vous ne pouvez empoisonner que pendant la nuit.");
            return true;
        }

        if (gm.hasWitchUsedPoisonPotion(player)) {
            player.sendMessage("§c❌ Vous avez déjà utilisé votre potion de poison !");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c❌ Usage : /lg empoisonner <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§c❌ Vous ne pouvez pas vous empoisonner vous-même.");
            return true;
        }

        if (!gm.isPlayerAlive(target)) {
            player.sendMessage("§c❌ Vous ne pouvez empoisonner que les joueurs vivants.");
            return true;
        }

        gm.setWitchPoisonTarget(target.getUniqueId());
        gm.setWitchUsedPoisonPotion(player);

        player.sendMessage("§d☠️ Vous avez empoisonné " + target.getName() + " !");

        return true;
    }

    /**
     * Affiche la liste des joueurs (connectés ou en partie).
     */
    private boolean handleList(CommandSender sender) {
        GameManager gm = plugin.getGameManager();
        sender.sendMessage("§6📋 === LISTE DES JOUEURS ===");

        if (!gm.isGameRunning()) {
            sender.sendMessage("§e👥 Joueurs connectés (" + Bukkit.getOnlinePlayers().size() + ") :");
            for (Player player : Bukkit.getOnlinePlayers()) {
                sender.sendMessage("§a- " + player.getName());
            }
            return true;
        }

        int alive = 0;
        int dead = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerRole role = gm.getPlayers().get(player.getUniqueId());
            if (role != null) {
                boolean isAlive = gm.isPlayerAlive(player);
                String status = isAlive ? "§a✅ Vivant" : "§c💀 Mort";

                if (sender.hasPermission("loupgarou.admin")) {
                    sender.sendMessage("§e" + player.getName() + " §7- " + role.getDisplayName() + " §7- " + status);
                } else {
                    sender.sendMessage("§e" + player.getName() + " §7- " + status);
                }

                if (isAlive) alive++;
                else dead++;
            }
        }

        sender.sendMessage("");
        sender.sendMessage("§a👥 Vivants : " + alive + " §7| §c💀 Morts : " + dead);
        return true;
    }

    /**
     * Gère la désignation du nouveau maire par l'ancien maire décédé.
     *
     * @param sender l'expéditeur de la commande
     * @param args   arguments de la commande (/lg maire <joueur>)
     * @return toujours vrai pour indiquer que la commande a été traitée
     */
    private boolean handleMayor(CommandSender sender, String[] args) {
        // Seul un joueur peut désigner un successeur
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();

        // Vérifier que ce joueur est l'actuel maire enregistré
        if (!player.getUniqueId().equals(gm.getMayor())) {
            sender.sendMessage("§c❌ Vous n'êtes pas le maire actuel !");
            return true;
        }

        // Vérifier qu'il est bien mort (sinon il n'a pas à choisir de successeur)
        if (gm.isPlayerAlive(player)) {
            sender.sendMessage("§c❌ Vous devez être éliminé pour désigner un successeur.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c❌ Usage : /lg maire <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        // La cible doit être vivante pour devenir maire
        if (!gm.isPlayerAlive(target)) {
            sender.sendMessage("§c❌ Vous devez choisir un joueur vivant pour devenir maire.");
            return true;
        }

        // Nommer le nouveau maire
        gm.setNewMayor(target, false);
        sender.sendMessage("§6👑 Vous avez désigné §e" + target.getName() + " §6comme nouveau maire.");
        return true;
    }

    /**
     * Affiche l'aide des commandes disponibles.
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6" + "=".repeat(50));
        sender.sendMessage(Utils.centerText("§6§l🐺 AIDE LOUP-GAROU 🐺", 50));
        sender.sendMessage("§6" + "=".repeat(50));

        if (sender.hasPermission("loupgarou.admin")) {
            sender.sendMessage("§e📋 §lCommandes d'administration :");
            sender.sendMessage("§f/lg start §7- Démarrer une partie");
            sender.sendMessage("§f/lg stop §7- Arrêter la partie en cours");
            sender.sendMessage("§f/lg reload §7- Recharger la configuration");
            sender.sendMessage("§f/lg setspawn §7- Définir le spawn de jeu");
            sender.sendMessage("§f/lg update §7- Vérifier les mises à jour");
            sender.sendMessage("§f/lg bstats §7- Voir les statistiques du plugin");
            sender.sendMessage("§f/lg statut §7- Voir le statut de la partie");
            sender.sendMessage("§f/lg liste §7- Voir tous les joueurs et leurs rôles");
            sender.sendMessage("");
        }

        sender.sendMessage("§e🎮 §lCommandes de jeu :");
        sender.sendMessage("§f/vote <joueur> §7- Voter pour éliminer un joueur");
        sender.sendMessage("§f/lg statut §7- Voir votre statut et rôle");
        sender.sendMessage("§f/lg liste §7- Voir la liste des joueurs");
        sender.sendMessage("§f/lg stats §7- Voir vos statistiques");

        sender.sendMessage("");
        sender.sendMessage("§e🎭 §lCommandes spéciales (selon votre rôle) :");
        sender.sendMessage("§c/lg tuer <joueur> §7- (Loup-Garou) Choisir une victime");
        sender.sendMessage("§5/lg voir <joueur> §7- (Voyante) Révéler un rôle");
        sender.sendMessage("§b/lg proteger <joueur> §7- (Garde) Protéger un joueur");
        sender.sendMessage("§d/lg soigner <joueur> §7- (Sorcière) Ressusciter");
        sender.sendMessage("§d/lg empoisonner <joueur> §7- (Sorcière) Tuer");
        sender.sendMessage("§6/lg maire <joueur> §7- (Maire décédé) Choisir un successeur");

        sender.sendMessage("§6" + "=".repeat(50));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = Arrays.asList(
                    "start", "stop", "statut", "stats", "reload", "setspawn",
                    "kill", "tuer", "voir", "see", "proteger", "guard",
                    "soigner", "heal", "empoisonner", "poison",
                    "liste", "list", "aide", "help", "update",
                    "bstats",
                    // ← NOUVEAU : commandes liées au maire
                    "maire", "mayor"
            );

            String prefix = args[0].toLowerCase();
            for (String cmd : commands) {
                if (cmd.toLowerCase().startsWith(prefix)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            // Complétion des noms de joueurs pour les commandes qui en ont besoin
            List<String> playerCommands = Arrays.asList(
                    "kill", "tuer", "voir", "see", "proteger", "guard",
                    "soigner", "heal", "empoisonner", "poison",
                    "maire", "mayor"
            );

            if (playerCommands.contains(args[0].toLowerCase())) {
                String prefix = args[1].toLowerCase();
                GameManager gm = plugin.getGameManager();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Pour la commande du maire, seuls les joueurs vivants doivent être proposés
                    if (args[0].equalsIgnoreCase("maire") || args[0].equalsIgnoreCase("mayor")) {
                        if (!gm.isPlayerAlive(player)) {
                            continue;
                        }
                    }
                    if (player.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}