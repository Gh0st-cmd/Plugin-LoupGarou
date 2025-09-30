package org.gh0st.loupGarou;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import java.util.*;

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

        // Vérifier que c'est la phase de vote
        if (gm.getCurrentState() != GameManager.GameState.VOTE) {
            player.sendMessage("§c❌ Vous ne pouvez voter que pendant la phase de vote !");
            return true;
        }

        // Vérifier que le joueur est vivant
        if (!gm.isPlayerAlive(player)) {
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

        if (!gm.isPlayerAlive(target)) {
            player.sendMessage("§c❌ Vous ne pouvez pas voter pour un joueur mort !");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage("§c❌ Vous ne pouvez pas voter pour vous-même !");
            return true;
        }

        if (gm.addVote(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage("§e🗳️ Vous avez voté pour éliminer §c" + target.getName() + " §e!");

            // Message public anonyme
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (gm.isPlayerAlive(p) && !p.equals(player)) {
                    p.sendMessage("§7📊 Un vote a été enregistré...");
                }
            }

            // Son pour confirmer le vote
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } else {
            player.sendMessage("§c❌ Erreur lors de l'enregistrement du vote.");
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

        if (args.length == 1 && gm.getCurrentState() == GameManager.GameState.VOTE) {
            String prefix = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (gm.isPlayerAlive(player) &&
                        !player.equals(sender) &&
                        player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        }

        return completions;
    }
}