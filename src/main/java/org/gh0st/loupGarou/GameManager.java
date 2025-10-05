package org.gh0st.loupGarou;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class GameManager {

    private final LoupGarouPlugin plugin;
    private final Map<UUID, PlayerRole> players = new HashMap<>();
    private final Map<UUID, UUID> votes = new HashMap<>();
    private final Map<UUID, Boolean> witchHealUsed = new HashMap<>();
    private final Map<UUID, Boolean> witchPoisonUsed = new HashMap<>();
    private final Map<UUID, Boolean> guardSelfProtected = new HashMap<>();
    private final Map<UUID, Boolean> seerUsedPower = new HashMap<>();
    private final Map<UUID, Boolean> guardUsedPower = new HashMap<>();
    private GameState currentState = GameState.WAITING;
    private BukkitTask gameTask;
    private int dayNumber = 0;
    private UUID protectedPlayer;
    private UUID witchHealTarget;
    private UUID witchPoisonTarget;
    private long gameStartTime = 0; // ← AJOUT pour bStats

    public GameManager(LoupGarouPlugin plugin) {
        this.plugin = plugin;
    }

    public enum GameState {
        WAITING("§7En attente"),
        STARTING("§eCommencement"),
        NIGHT("§1🌙 Nuit"),
        DAY("§e☀️ Jour"),
        VOTE("§c🗳️ Vote"),
        FINISHED("§aTerminé");

        private final String displayName;

        GameState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public boolean startGame() {
        if (currentState != GameState.WAITING) {
            broadcastMessage("§c❌ Une partie est déjà en cours !");
            return false;
        }

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.size() < plugin.getConfigManager().getMinPlayers()) {
            broadcastMessage(Messages.format(Messages.NOT_ENOUGH_PLAYERS, plugin.getConfigManager().getMinPlayers()));
            return false;
        }

        // Vérifier que les joueurs sont dans la bonne région/monde
        WorldGuardIntegration wg = plugin.getWorldGuardIntegration();
        if (plugin.getConfigManager().isWorldGuardEnabled() && wg.isWorldGuardAvailable()) {
            List<Player> invalidPlayers = new ArrayList<>();

            for (Player player : onlinePlayers) {
                if (!wg.isPlayerInCorrectWorld(player)) {
                    invalidPlayers.add(player);
                    player.sendMessage(plugin.getConfigManager().getMessagePrefix() + " " +
                            plugin.getConfigManager().getWrongWorldMessage());
                } else if (!wg.canPlayerPlay(player)) {
                    invalidPlayers.add(player);
                    player.sendMessage(plugin.getConfigManager().getMessagePrefix() + " " +
                            plugin.getConfigManager().getNotInRegionMessage());
                }
            }

            if (!invalidPlayers.isEmpty()) {
                broadcastMessage("§c❌ Certains joueurs ne sont pas dans la zone de jeu !");
                broadcastMessage("§e💡 Tous les joueurs doivent être dans le monde '" +
                        plugin.getConfigManager().getWorldGuardWorld() +
                        "' et dans la région '" +
                        plugin.getConfigManager().getRegionName() + "'");
                return false;
            }
        }

        currentState = GameState.STARTING;
        gameStartTime = System.currentTimeMillis(); // ← AJOUT pour bStats

        // Reset des données
        resetGameData();

        // Préparation de la partie
        assignRoles();
        sendRoleMessages();
        teleportPlayersToGame();

        // Annonces
        broadcastMessage(Messages.GAME_STARTING);
        broadcastMessage(Messages.ROLES_DISTRIBUTING);
        SoundManager.playGameStart();

        // Démarrer la première nuit après 10 secondes
        Bukkit.getScheduler().runTaskLater(plugin, () -> startNight(), 200L);

        // Mettre à jour le scoreboard
        plugin.getScoreboardManager().updateScoreboard();

        return true;
    }

    private void resetGameData() {
        players.clear();
        votes.clear();
        witchHealUsed.clear();
        witchPoisonUsed.clear();
        guardSelfProtected.clear();
        seerUsedPower.clear();
        guardUsedPower.clear();
        dayNumber = 0;
        protectedPlayer = null;
        witchHealTarget = null;
        witchPoisonTarget = null;

        // Nettoyer les équipes du scoreboard
        plugin.getScoreboardManager().removeAllTeams();
    }

    private void assignRoles() {
        List<Player> playerList = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(playerList);

        int playerCount = playerList.size();
        int werewolfCount = Math.max(1, (int) (playerCount * plugin.getConfigManager().getWerewolfRatio()));

        int currentIndex = 0;

        // Attribution des loups-garous
        for (int i = 0; i < werewolfCount && currentIndex < playerList.size(); i++) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.WEREWOLF);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.WEREWOLF);

            // ← AJOUT pour bStats
            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.WEREWOLF);
            }
        }

        // Attribution des rôles spéciaux selon le nombre de joueurs
        if (playerCount >= plugin.getConfigManager().getSeerMinPlayers() && currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.SEER);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.SEER);

            // ← AJOUT pour bStats
            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.SEER);
            }
        }

        if (playerCount >= plugin.getConfigManager().getGuardMinPlayers() && currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.GUARD);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.GUARD);

            // ← AJOUT pour bStats
            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.GUARD);
            }
        }

        if (playerCount >= plugin.getConfigManager().getWitchMinPlayers() && currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.WITCH);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.WITCH);

            // ← AJOUT pour bStats
            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.WITCH);
            }
        }

        // Le reste devient villageois
        while (currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.VILLAGER);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.VILLAGER);

            // ← AJOUT pour bStats
            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.VILLAGER);
            }
        }
    }

    private void sendRoleMessages() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerRole role = players.get(player.getUniqueId());
            if (role != null) {
                player.sendMessage("§6" + "=".repeat(50));
                player.sendMessage(Utils.centerText("§6§l🎭 VOTRE RÔLE 🎭", 50));
                player.sendMessage("§6" + "=".repeat(50));
                player.sendMessage("");
                player.sendMessage("§e🎭 Vous êtes : " + role.getDisplayName());
                player.sendMessage("");
                player.sendMessage("§f📖 Description :");
                player.sendMessage("§f" + role.getDescription());
                player.sendMessage("");
                player.sendMessage("§6" + "=".repeat(50));

                // Son et titre selon le rôle
                SoundManager.playRoleReveal(player, role);
                player.sendTitle(role.getDisplayName(), "§fC'est votre rôle !", 20, 80, 20);
            }
        }
    }

    private void startNight() {
        dayNumber++;
        currentState = GameState.NIGHT;
        votes.clear();
        protectedPlayer = null;
        witchHealTarget = null;
        witchPoisonTarget = null;

        // Reset des pouvoirs utilisés cette nuit
        seerUsedPower.clear();
        guardUsedPower.clear();

        broadcastMessage(Messages.format(Messages.NIGHT_START, dayNumber));
        broadcastMessage(Messages.NIGHT_DESCRIPTION);

        SoundManager.playNightStart();

        // Effet de nuit SEULEMENT pour les joueurs dans la partie
        setTimeForPlayers(18000);

        // Instructions pour chaque rôle
        sendNightInstructions();

        // Timer pour la phase de nuit
        int nightDuration = plugin.getConfigManager().getNightDuration();
        broadcastMessage("§9⏰ Phase de nuit : " + Utils.formatTime(nightDuration));

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                processNightActions();
                startDay();
            }
        }.runTaskLater(plugin, nightDuration * 20L);

        plugin.getScoreboardManager().updateScoreboard();
    }

    private void sendNightInstructions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isPlayerAlive(player)) continue;

            PlayerRole role = players.get(player.getUniqueId());
            if (role == null) continue;

            switch (role) {
                case WEREWOLF:
                    player.sendMessage("§c🐺 === PHASE DES LOUPS-GAROUS ===");
                    player.sendMessage("§c💀 Choisissez votre victime : /lg tuer <joueur>");
                    showAliveVillagers(player);
                    break;

                case SEER:
                    player.sendMessage("§5🔮 === PHASE DE LA VOYANTE ===");
                    player.sendMessage("§5👁️ Découvrez un rôle : /lg voir <joueur>");
                    showAlivePlayers(player);
                    break;

                case GUARD:
                    player.sendMessage("§b🛡️ === PHASE DU GARDE ===");
                    player.sendMessage("§b🛡️ Protégez quelqu'un : /lg proteger <joueur>");
                    if (!hasGuardProtectedHimself(player)) {
                        player.sendMessage("§b💡 Vous pouvez vous protéger une fois par partie");
                    }
                    showAlivePlayers(player);
                    break;

                case WITCH:
                    player.sendMessage("§d🧪 === PHASE DE LA SORCIÈRE ===");
                    if (!hasWitchUsedHealPotion(player)) {
                        player.sendMessage("§d💚 Soigner : /lg soigner <joueur>");
                    }
                    if (!hasWitchUsedPoisonPotion(player)) {
                        player.sendMessage("§d☠️ Empoisonner : /lg empoisonner <joueur>");
                    }
                    showAlivePlayers(player);
                    break;

                case VILLAGER:
                    player.sendMessage("§a😴 Vous dormez paisiblement...");
                    player.sendMessage("§7💭 Observez le chat pour voir ce qui se passe...");
                    break;
            }
        }
    }

    private void processNightActions() {
        broadcastMessage("§9🌅 L'aube se lève... Découvrons ce qui s'est passé cette nuit.");

        // Traiter les votes des loups-garous
        Map<UUID, Integer> killVotes = new HashMap<>();
        for (UUID voterId : votes.keySet()) {
            if (players.get(voterId) == PlayerRole.WEREWOLF) {
                UUID target = votes.get(voterId);
                killVotes.put(target, killVotes.getOrDefault(target, 0) + 1);
            }
        }

        // Trouver la victime principale
        UUID victim = null;
        int maxVotes = 0;
        for (Map.Entry<UUID, Integer> entry : killVotes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                victim = entry.getKey();
            }
        }

        // Résoudre les actions de nuit
        boolean someoneProtected = false;
        boolean someoneHealed = false;

        if (victim != null) {
            Player victimPlayer = Bukkit.getPlayer(victim);
            if (victimPlayer != null && isPlayerAlive(victimPlayer)) {

                // Vérifier la protection du garde
                boolean isProtected = victim.equals(protectedPlayer);

                // Vérifier si la sorcière guérit
                boolean isHealed = victim.equals(witchHealTarget);

                if (isProtected) {
                    someoneProtected = true;
                } else if (isHealed) {
                    someoneHealed = true;
                } else {
                    eliminatePlayer(victimPlayer, "dévoré par les loups-garous");
                }
            }
        }

        // Traiter le poison de la sorcière
        if (witchPoisonTarget != null) {
            Player poisonedPlayer = Bukkit.getPlayer(witchPoisonTarget);
            if (poisonedPlayer != null && isPlayerAlive(poisonedPlayer)) {
                eliminatePlayer(poisonedPlayer, "empoisonné par la sorcière");
            }
        }

        // Messages informatifs
        if (someoneProtected) {
            broadcastMessage(Messages.PROTECTED_SOMEONE);
        }
        if (someoneHealed) {
            broadcastMessage(Messages.WITCH_SAVED);
        }
        if (victim == null && witchPoisonTarget == null) {
            broadcastMessage(Messages.QUIET_NIGHT);
        }
    }

    private void startDay() {
        currentState = GameState.DAY;
        votes.clear();

        broadcastMessage(Messages.format(Messages.DAY_START, dayNumber));
        broadcastMessage(Messages.DAY_DESCRIPTION);

        SoundManager.playDayStart();

        // Effet de jour SEULEMENT pour les joueurs dans la partie
        setTimeForPlayers(6000);

        // Vérifier les conditions de victoire
        if (checkWinConditions()) {
            return;
        }

        showGameStatus();

        // Phase de discussion
        int dayDuration = plugin.getConfigManager().getDayDuration();
        broadcastMessage("§e💬 Phase de discussion ! (" + Utils.formatTime(dayDuration) + ")");
        broadcastMessage("§e🗣️ C'est le moment de débattre et de trouver les coupables !");

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                startVotePhase();
            }
        }.runTaskLater(plugin, dayDuration * 20L);

        plugin.getScoreboardManager().updateScoreboard();
    }

    private void startVotePhase() {
        currentState = GameState.VOTE;
        votes.clear();

        broadcastMessage(Messages.VOTE_START);
        broadcastMessage(Messages.VOTE_DESCRIPTION);
        broadcastMessage("§e💡 Utilisez : §f/vote <joueur> §epour voter");

        SoundManager.playVoteStart();
        showAlivePlayers();

        // Timer pour les votes
        int voteDuration = plugin.getConfigManager().getVoteDuration();
        broadcastMessage("§c⏰ Vous avez " + Utils.formatTime(voteDuration) + " pour voter !");

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                processVotes();
            }
        }.runTaskLater(plugin, voteDuration * 20L);

        plugin.getScoreboardManager().updateScoreboard();
    }

    private void processVotes() {
        Map<UUID, Integer> voteCount = new HashMap<>();
        Set<UUID> voters = new HashSet<>();

        // Compter les votes valides
        for (Map.Entry<UUID, UUID> vote : votes.entrySet()) {
            UUID voter = vote.getKey();
            UUID target = vote.getValue();

            Player voterPlayer = Bukkit.getPlayer(voter);
            Player targetPlayer = Bukkit.getPlayer(target);

            if (isPlayerAlive(voterPlayer) && isPlayerAlive(targetPlayer)) {
                voteCount.put(target, voteCount.getOrDefault(target, 0) + 1);
                voters.add(voter);
            }
        }

        broadcastMessage("§6📊 === RÉSULTATS DES VOTES ===");

        if (voteCount.isEmpty()) {
            broadcastMessage("§7😴 Aucun vote valide ! Personne n'est éliminé.");
        } else {
            // Afficher tous les résultats
            List<Map.Entry<UUID, Integer>> sortedVotes = new ArrayList<>(voteCount.entrySet());
            sortedVotes.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<UUID, Integer> entry : sortedVotes) {
                Player target = Bukkit.getPlayer(entry.getKey());
                if (target != null) {
                    String plural = entry.getValue() > 1 ? "s" : "";
                    broadcastMessage("§e" + target.getName() + " : §f" + entry.getValue() + " vote" + plural);
                }
            }

            // Éliminer le joueur avec le plus de votes
            if (!sortedVotes.isEmpty()) {
                UUID mostVoted = sortedVotes.get(0).getKey();
                int maxVotes = sortedVotes.get(0).getValue();

                // Vérifier s'il y a égalité
                long playersWithMaxVotes = sortedVotes.stream()
                        .mapToInt(Map.Entry::getValue)
                        .filter(votes -> votes == maxVotes)
                        .count();

                if (playersWithMaxVotes > 1) {
                    broadcastMessage("§c⚖️ Égalité ! Personne n'est éliminé.");
                } else {
                    Player eliminatedPlayer = Bukkit.getPlayer(mostVoted);
                    if (eliminatedPlayer != null) {
                        eliminatePlayer(eliminatedPlayer, "éliminé par vote du village");
                    }
                }
            }
        }

        broadcastMessage("§e👥 Total des votants : " + voters.size() + "/" + getAlivePlayersCount());

        // Vérifier les conditions de victoire
        if (!checkWinConditions()) {
            // Continuer avec une nouvelle nuit
            Bukkit.getScheduler().runTaskLater(plugin, this::startNight, 100L);
        }
    }

    private void eliminatePlayer(Player player, String reason) {
        PlayerRole role = players.get(player.getUniqueId());

        broadcastMessage("§c💀 " + player.getName() + " a été " + reason + " !");
        broadcastMessage("§6🎭 " + player.getName() + " était : " + (role != null ? role.getDisplayName() : "§aVillageois"));

        // Effets visuels et sonores
        SoundManager.playElimination();
        player.setGameMode(GameMode.SPECTATOR);

        // Titre dramatique pour tous
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§c💀 " + player.getName(), "§6a été " + reason, 10, 60, 20);
        }

        // Statistiques
        plugin.getStatsManager().addDeath(player, reason);

        // Message privé au joueur éliminé
        player.sendMessage("§c💀 Vous avez été éliminé ! Vous pouvez maintenant observer la partie.");
        player.sendMessage("§7💬 Chattez avec les autres morts ou observez la suite...");
    }

    private boolean checkWinConditions() {
        int werewolves = 0;
        int villagers = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(player)) {
                PlayerRole role = players.get(player.getUniqueId());
                if (role != null) {
                    if (role == PlayerRole.WEREWOLF) {
                        werewolves++;
                    } else {
                        villagers++;
                    }
                }
            }
        }

        if (werewolves == 0) {
            endGame(
                    Messages.VILLAGE_VICTORY,
                    Messages.VILLAGE_VICTORY_REASON,
                    "village"
            );
            return true;
        } else if (werewolves >= villagers) {
            endGame(
                    Messages.WEREWOLF_VICTORY,
                    Messages.WEREWOLF_VICTORY_REASON,
                    "werewolves"
            );
            return true;
        }

        return false;
    }

    private void endGame(String title, String subtitle, String winningTeam) {
        currentState = GameState.FINISHED;

        if (gameTask != null) {
            gameTask.cancel();
        }

        // ← AJOUT pour bStats : Enregistrer les statistiques de la partie
        long gameDuration = (System.currentTimeMillis() - gameStartTime) / 1000; // en secondes
        int playerCount = players.size();

        if (plugin.getBStatsManager() != null) {
            plugin.getBStatsManager().recordGame(playerCount, gameDuration, winningTeam);
            plugin.getLogger().info("📊 Partie enregistrée dans bStats : " +
                    playerCount + " joueurs, " + (gameDuration / 60) + " minutes, gagnant: " + winningTeam);
        }

        // Annonce de fin
        broadcastMessage("§6" + "=".repeat(60));
        broadcastMessage(Utils.centerText(title, 60));
        broadcastMessage(Utils.centerText(subtitle, 60));
        broadcastMessage("§6" + "=".repeat(60));

        // Révéler tous les rôles
        broadcastMessage("");
        broadcastMessage("§e📋 === RÉVÉLATION DE TOUS LES RÔLES ===");
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerRole role = players.get(player.getUniqueId());
            if (role != null) {
                String status = isPlayerAlive(player) ? "§a✅" : "§c💀";
                broadcastMessage(status + " §f" + player.getName() + " §7: " + role.getDisplayName());
            }
        }

        // Enregistrer les statistiques
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerRole role = players.get(player.getUniqueId());
            if (role != null) {
                plugin.getStatsManager().addGamePlayed(player);

                // Ajouter victoire si dans l'équipe gagnante
                String playerTeam = role.getTeam();
                if (playerTeam.equals(winningTeam)) {
                    plugin.getStatsManager().addWin(player, role, winningTeam);
                }
            }
        }

        // Effets visuels pour tous
        SoundManager.playVictory();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, 20, 100, 40);
            player.setGameMode(GameMode.SURVIVAL);
            player.resetPlayerTime();
        }

        broadcastMessage("");
        broadcastMessage("§a🔄 Nouvelle partie dans " + plugin.getConfigManager().getRestartDelay() + " secondes...");

        // Reset automatique
        if (plugin.getConfigManager().isAutoRestart()) {
            Bukkit.getScheduler().runTaskLater(plugin, this::resetGame, plugin.getConfigManager().getRestartDelay() * 20L);
        }
    }

    public void stopGame() {
        if (gameTask != null) {
            gameTask.cancel();
        }
        resetGame();
        broadcastMessage("§c⛔ La partie a été arrêtée par un administrateur.");
    }

    private void resetGame() {
        currentState = GameState.WAITING;
        resetGameData();

        // Remettre les joueurs en mode normal
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.resetPlayerTime();
        }

        broadcastMessage("§a🔄 Partie réinitialisée ! Utilisez /lg start pour commencer une nouvelle partie.");
        plugin.getScoreboardManager().updateScoreboard();
    }

    private void setupGameEnvironment() {
        // Configurer l'environnement de jeu
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.getInventory().clear();
        }
    }

    private void teleportPlayersToGame() {
        // Téléporter tous les joueurs au spawn configuré
        Location gameLocation = plugin.getConfigManager().getSpawnLocation();

        if (gameLocation == null) {
            plugin.getLogger().warning("⚠️ Impossible de récupérer le spawn configuré, utilisation du spawn par défaut");
            World world = Bukkit.getWorld(plugin.getConfigManager().getSpawnWorld());
            if (world == null) {
                world = Bukkit.getWorlds().get(0);
            }
            gameLocation = world.getSpawnLocation();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(gameLocation);
            player.setGameMode(GameMode.ADVENTURE);
        }

        plugin.getLogger().info("Joueurs téléportés au spawn: " + gameLocation.getWorld().getName() +
                " (" + gameLocation.getBlockX() + ", " + gameLocation.getBlockY() + ", " + gameLocation.getBlockZ() + ")");
    }

    private void showGameStatus() {
        broadcastMessage("§a👥 Joueurs vivants : " + getAlivePlayersCount());
        showAlivePlayers();

        // Statistiques des équipes
        int werewolves = 0, villagers = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(player)) {
                PlayerRole role = players.get(player.getUniqueId());
                if (role == PlayerRole.WEREWOLF) {
                    werewolves++;
                } else if (role != null) {
                    villagers++;
                }
            }
        }

        broadcastMessage("§c🐺 Loups restants : " + werewolves + " §7| §a👥 Village : " + villagers);
    }

    // Méthodes utilitaires
    private void broadcastMessage(String message) {
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessagePrefix() + " " + message);
    }

    public boolean isPlayerAlive(Player player) {
        return player != null && player.isOnline() &&
                player.getGameMode() != GameMode.SPECTATOR &&
                players.containsKey(player.getUniqueId());
    }

    public int getAlivePlayersCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(player)) {
                count++;
            }
        }
        return count;
    }

    private void showAlivePlayers() {
        showAlivePlayers(null);
    }

    private void showAlivePlayers(Player target) {
        StringBuilder alive = new StringBuilder("§a👥 Joueurs vivants : ");
        boolean first = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(player)) {
                if (!first) alive.append("§f, ");
                alive.append("§e").append(player.getName());
                first = false;
            }
        }

        if (target != null) {
            target.sendMessage(alive.toString());
        } else {
            broadcastMessage(alive.toString());
        }
    }

    private void showAliveVillagers(Player werewolf) {
        werewolf.sendMessage("§c🎯 Cibles possibles :");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(player) && players.get(player.getUniqueId()) != PlayerRole.WEREWOLF) {
                werewolf.sendMessage("§e- " + player.getName());
            }
        }
    }

    // Méthodes pour gérer les actions spéciales des rôles
    public void setProtectedPlayer(UUID playerId) {
        this.protectedPlayer = playerId;
    }

    public void setWitchHealTarget(UUID playerId) {
        this.witchHealTarget = playerId;
    }

    public void setWitchPoisonTarget(UUID playerId) {
        this.witchPoisonTarget = playerId;
    }

    public void setWitchUsedHealPotion(Player witch) {
        witchHealUsed.put(witch.getUniqueId(), true);
    }

    public void setWitchUsedPoisonPotion(Player witch) {
        witchPoisonUsed.put(witch.getUniqueId(), true);
    }

    public void setGuardProtectedHimself(Player guard) {
        guardSelfProtected.put(guard.getUniqueId(), true);
    }

    public void setSeerUsedPower(Player seer) {
        seerUsedPower.put(seer.getUniqueId(), true);
    }

    public void setGuardUsedPower(Player guard) {
        guardUsedPower.put(guard.getUniqueId(), true);
    }

    public boolean hasWitchUsedHealPotion(Player witch) {
        return witchHealUsed.getOrDefault(witch.getUniqueId(), false);
    }

    public boolean hasWitchUsedPoisonPotion(Player witch) {
        return witchPoisonUsed.getOrDefault(witch.getUniqueId(), false);
    }

    public boolean hasGuardProtectedHimself(Player guard) {
        return guardSelfProtected.getOrDefault(guard.getUniqueId(), false);
    }

    public boolean hasSeerUsedPower(Player seer) {
        return seerUsedPower.getOrDefault(seer.getUniqueId(), false);
    }

    public boolean hasGuardUsedPower(Player guard) {
        return guardUsedPower.getOrDefault(guard.getUniqueId(), false);
    }

    public void checkWinConditionsAfterLeave() {
        if (isGameRunning()) {
            checkWinConditions();
        }
    }

    public boolean addVote(UUID voter, UUID target) {
        if (!isPlayerAlive(Bukkit.getPlayer(voter)) || !isPlayerAlive(Bukkit.getPlayer(target))) {
            return false;
        }
        votes.put(voter, target);
        return true;
    }

    // Getters
    public GameState getCurrentState() {
        return currentState;
    }

    public boolean isGameRunning() {
        return currentState != GameState.WAITING && currentState != GameState.FINISHED;
    }

    public Map<UUID, PlayerRole> getPlayers() {
        return new HashMap<>(players);
    }

    public int getDayNumber() {
        return dayNumber;
    }

    /**
     * Change le temps uniquement pour les joueurs en partie
     */
    private void setTimeForPlayers(long time) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.containsKey(player.getUniqueId())) {
                player.setPlayerTime(time, false);
            }
        }
    }
}