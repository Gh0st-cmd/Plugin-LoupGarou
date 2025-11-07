package org.gh0st.loupGarou.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.gh0st.loupGarou.LoupGarouPlugin;
import org.gh0st.loupGarou.config.Messages;
import org.gh0st.loupGarou.game.GameManager;

import java.util.*;

/**
 * Commande de vote /vote. Permet de voter pour éliminer un joueur pendant
 * la phase de vote standard ou de décision du maire. Durant la phase du
 * maire, seul le maire peut voter et les candidats sont limités aux
 * joueurs en égalité. Le GameManager gère les validations supplémentaires.
 */
public class VoteCommand implements CommandExecutor, TabCompleter {

    private final LoupGarouPlugin plugin;

    public VoteCommand(LoupGarouPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();

        // Vérifier que c'est la phase de vote ou de décision du maire
        GameManager.GameState state = gm.getCurrentState();
        if (state != GameManager.GameState.VOTE && state != GameManager.GameState.MAYOR_VOTE) {
            player.sendMessage("§c❌ Vous ne pouvez voter que pendant la phase de vote !");
            return true;
        }

        // Si nous sommes dans la phase normale de vote, uniquement les joueurs vivants peuvent voter.
        // Dans la phase de décision du maire, le vote est réservé au maire (addVote gère cette vérification).
        if (state == GameManager.GameState.VOTE && !gm.isPlayerAlive(player)) {
            player.sendMessage("§c❌ Les joueurs morts ne peuvent pas voter !");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§c❌ Usage : /vote <joueur>");
            player.sendMessage("§e💡 Tapez §f/lg liste §epour voir les joueurs vivants");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        // Pour la phase normale de vote, empêcher de voter pour un mort ou soi-même
        if (state == GameManager.GameState.VOTE) {
            if (!gm.isPlayerAlive(target)) {
                player.sendMessage("§c❌ Vous ne pouvez pas voter pour un joueur mort !");
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage("§c❌ Vous ne pouvez pas voter pour vous-même !");
                return true;
            }
        }

        // Enregistrer le vote via le GameManager. addVote gère les validations (maire, candidats, etc.)
        if (gm.addVote(player.getUniqueId(), target.getUniqueId())) {
            // Message personnalisé selon la phase
            if (state == GameManager.GameState.MAYOR_VOTE) {
                player.sendMessage("§6👑 Vous avez choisi d'éliminer §c" + target.getName() + " §6!");
            } else {
                player.sendMessage("§e🗳️ Vous avez voté pour éliminer §c" + target.getName() + " §e!");
                // Message public anonyme pour informer les autres joueurs vivants
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (gm.isPlayerAlive(p) && !p.equals(player)) {
                        p.sendMessage("§7📊 Un vote a été enregistré...");
                    }
                }
            }
            // Son pour confirmer le vote
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } else {
            // Si le vote n'est pas enregistré, informer le joueur
            if (state == GameManager.GameState.MAYOR_VOTE) {
                player.sendMessage("§c❌ Vous ne pouvez pas voter pour ce joueur. Choisissez un candidat valide.");
            } else {
                player.sendMessage("§c❌ Erreur lors de l'enregistrement du vote.");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        GameManager gm = plugin.getGameManager();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            GameManager.GameState state = gm.getCurrentState();
            // Pendant les phases VOTE et MAYOR_VOTE, lister les joueurs vivants
            if (state == GameManager.GameState.VOTE || state == GameManager.GameState.MAYOR_VOTE) {
                String prefix = args[0].toLowerCase();
                Player senderPlayer = (Player) sender;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (gm.isPlayerAlive(player) && !player.equals(senderPlayer) && player.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            }
        }

        return completions;
    }
}