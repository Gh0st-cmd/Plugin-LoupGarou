package org.gh0st.loupGarou.game;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.gh0st.loupGarou.*;
import org.gh0st.loupGarou.config.Messages;
import org.gh0st.loupGarou.role.PlayerRole;
import org.gh0st.loupGarou.utils.Utils;
import org.gh0st.loupGarou.utils.extern.WorldGuardIntegration;

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
    private final Set<UUID> deadPlayers = new HashSet<>(); // ← NOUVEAU : Suivi des morts
    private UUID mayor = null; // ← NOUVEAU : Le maire
    private Location circleCenter = null; // ← NOUVEAU : Centre du cercle
    private final Map<UUID, Location> originalLocations = new HashMap<>(); // ← NOUVEAU : Positions d'origine

    private GameState currentState = GameState.WAITING;
    private BukkitTask gameTask;
    private BukkitTask phaseTimerTask; // ← NOUVEAU : Pour annuler le timer
    private int dayNumber = 0;
    private UUID protectedPlayer;
    private UUID witchHealTarget;
    private UUID witchPoisonTarget;
    private long gameStartTime = 0;

    public GameManager(LoupGarouPlugin plugin) {
        this.plugin = plugin;
    }

    public enum GameState {
        WAITING("§7En attente"),
        STARTING("§eCommencement"),
        NIGHT("§1🌙 Nuit"),
        DAY("§e☀️ Jour"),
        VOTE("§c🗳️ Vote"),
        MAYOR_VOTE("§6👑 Vote du Maire"),
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

        // Créer la liste des participants
        List<Player> participants = new ArrayList<>();
        WorldGuardIntegration wg = plugin.getWorldGuardIntegration();
        boolean useWorldGuard = wg != null && plugin.getConfigManager().isWorldGuardEnabled() && wg.isWorldGuardAvailable();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (useWorldGuard) {
                // Si WG est activé, on filtre
                if (wg.isPlayerInCorrectWorld(player) && wg.canPlayerPlay(player)) {
                    participants.add(player);
                }
            } else {
                // Sinon on prend tout le monde
                participants.add(player);
            }
        }

        if (participants.size() < plugin.getConfigManager().getMinPlayers()) {
            broadcastMessage(Messages.format(Messages.NOT_ENOUGH_PLAYERS, plugin.getConfigManager().getMinPlayers()));
            // Feedback si WG est activé
            if (useWorldGuard) {
                broadcastMessage("§7💡 (Seuls les joueurs dans la zone WorldGuard sont comptés)");
            }
            return false;
        }

        currentState = GameState.STARTING;
        gameStartTime = System.currentTimeMillis();

        // Reset des données
        resetGameData();

        // Préparation de la partie
        assignRoles(participants); // ← MODIFIÉ : On passe la liste filtrée
        electMayor(); // ← NOUVEAU : Élection du maire
        sendRoleMessages();
        teleportPlayersToGame();

        // Annonces
        broadcastMessage(Messages.GAME_STARTING);
        broadcastMessage(Messages.ROLES_DISTRIBUTING);
        SoundManager.playGameStart(getOnlineGamePlayers());

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
        deadPlayers.clear(); // ← NOUVEAU
        mayor = null; // ← NOUVEAU
        originalLocations.clear(); // ← NOUVEAU
        dayNumber = 0;
        protectedPlayer = null;
        witchHealTarget = null;
        witchPoisonTarget = null;

        // Nettoyer les équipes du scoreboard
        plugin.getScoreboardManager().removeAllTeams();
    }

    // ← NOUVEAU : Élection du maire
    private void electMayor() {
        if (plugin.getConfigManager().getConfig().getBoolean("game.enable-mayor", true)) {
            // On ne prend que les joueurs qui ont un rôle
            List<Player> playerList = new ArrayList<>();
            for (UUID uuid : players.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) playerList.add(p);
            }

            if (!playerList.isEmpty()) {
                Player mayorPlayer = playerList.get(new Random().nextInt(playerList.size()));
                mayor = mayorPlayer.getUniqueId();

                broadcastMessage("§6👑 === ÉLECTION DU MAIRE ===");
                broadcastMessage("§e🎖️ " + mayorPlayer.getName() + " a été élu maire du village !");
                broadcastMessage("§7💡 En cas d'égalité lors des votes, le maire départagera.");

                mayorPlayer.sendMessage("§6" + "=".repeat(50));
                mayorPlayer.sendMessage(Utils.centerText("§6§l👑 VOUS ÊTES LE MAIRE 👑", 50));
                mayorPlayer.sendMessage("§6" + "=".repeat(50));
                mayorPlayer.sendMessage("§e🎖️ Vous avez été élu maire du village !");
                mayorPlayer.sendMessage("§7💡 Votre rôle : " + players.get(mayor).getDisplayName());
                mayorPlayer.sendMessage("§7💡 En cas d'égalité, vous choisirez qui éliminer.");
                mayorPlayer.sendMessage("§6" + "=".repeat(50));

                mayorPlayer.playSound(mayorPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
    }

    private void assignRoles(List<Player> playerList) {
        Collections.shuffle(playerList);

        int playerCount = playerList.size();
        int werewolfCount = Math.max(1, (int) (playerCount * plugin.getConfigManager().getWerewolfRatio()));

        int currentIndex = 0;

        // Attribution des loups-garous
        for (int i = 0; i < werewolfCount && currentIndex < playerList.size(); i++) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.WEREWOLF);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.WEREWOLF);

            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.WEREWOLF);
            }
        }

        // Attribution des rôles spéciaux selon le nombre de joueurs
        if (playerCount >= plugin.getConfigManager().getSeerMinPlayers() && currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.SEER);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.SEER);

            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.SEER);
            }
        }

        if (playerCount >= plugin.getConfigManager().getGuardMinPlayers() && currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.GUARD);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.GUARD);

            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.GUARD);
            }
        }

        if (playerCount >= plugin.getConfigManager().getWitchMinPlayers() && currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.WITCH);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.WITCH);

            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.WITCH);
            }
        }

        // Le reste devient villageois
        while (currentIndex < playerList.size()) {
            Player player = playerList.get(currentIndex++);
            players.put(player.getUniqueId(), PlayerRole.VILLAGER);
            plugin.getScoreboardManager().addPlayerRole(player, PlayerRole.VILLAGER);

            if (plugin.getBStatsManager() != null) {
                plugin.getBStatsManager().recordRoleUsage(PlayerRole.VILLAGER);
            }
        }
    }

    private void sendRoleMessages() {
        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PlayerRole role = players.get(uuid);
                if (role != null) {
                    player.sendMessage("§6" + "=".repeat(50));
                    player.sendMessage(Utils.centerText("§6§l🎭 VOTRE RÔLE 🎭", 50));
                    player.sendMessage("§6" + "=".repeat(50));
                    player.sendMessage("");
                    player.sendMessage("§e🎭 Vous êtes : " + role.getDisplayName());

                    // ← NOUVEAU : Indiquer si c'est le maire
                    if (player.getUniqueId().equals(mayor)) {
                        player.sendMessage("§6👑 Vous êtes également le MAIRE !");
                    }

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

        SoundManager.playNightStart(getOnlineGamePlayers());

        // Effet de nuit SEULEMENT pour les joueurs dans la partie
        setTimeForPlayers(18000);

        // ← NOUVEAU : Appliquer l'effet de cécité si configuré
        if (plugin.getConfigManager().getConfig().getBoolean("game.blindness-at-night", true)) {
            applyBlindnessEffect();
        }

        // ← NOUVEAU : Immobiliser les joueurs si configuré
        if (plugin.getConfigManager().getConfig().getBoolean("game.freeze-players-at-night", false)) {
            freezePlayers();
        }

        // Instructions pour chaque rôle
        sendNightInstructions();

        // Timer pour la phase de nuit
        int nightDuration = plugin.getConfigManager().getNightDuration();
        broadcastMessage("§9⏰ Phase de nuit : " + Utils.formatTime(nightDuration));

        phaseTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                processNightActions();
                startDay();
            }
        }.runTaskLater(plugin, nightDuration * 20L);

        plugin.getScoreboardManager().updateScoreboard();
    }

    // ← NOUVEAU : Appliquer la cécité
    private void applyBlindnessEffect() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(player) && players.containsKey(player.getUniqueId())) {
                PlayerRole role = players.get(player.getUniqueId());

                // Les loups-garous ne sont pas aveuglés
                if (role != PlayerRole.WEREWOLF) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.BLINDNESS,
                            Integer.MAX_VALUE,
                            0,
                            false,
                            false
                    ));
                }
            }
        }
    }

    // ← NOUVEAU : Retirer la cécité
    private void removeBlindnessEffect() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.containsKey(player.getUniqueId())) {
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }
    }

    // ← NOUVEAU : Geler les joueurs en cercle
    private void freezePlayers() {
        Location spawn = plugin.getConfigManager().getSpawnLocation();
        circleCenter = spawn;

        List<Player> alivePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(player) && players.containsKey(player.getUniqueId())) {
                alivePlayers.add(player);
            }
        }

        int playerCount = alivePlayers.size();
        double radius = Math.max(3, playerCount * 0.5); // Rayon adaptatif
        double angleStep = 2 * Math.PI / playerCount;

        for (int i = 0; i < playerCount; i++) {
            Player player = alivePlayers.get(i);
            double angle = i * angleStep;

            double x = spawn.getX() + radius * Math.cos(angle);
            double z = spawn.getZ() + radius * Math.sin(angle);

            Location circlePos = new Location(spawn.getWorld(), x, spawn.getY(), z);

            // Faire regarder vers le centre
            circlePos.setDirection(spawn.toVector().subtract(circlePos.toVector()));

            originalLocations.put(player.getUniqueId(), player.getLocation());
            player.teleport(circlePos);

            // Empêcher le mouvement
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
        }
    }

    // ← NOUVEAU : Dégeler les joueurs
    private void unfreezePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.containsKey(player.getUniqueId())) {
                player.setWalkSpeed(0.2f); // Vitesse par défaut
                player.setFlySpeed(0.1f);
            }
        }
        originalLocations.clear();
        circleCenter = null;
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

        // ← NOUVEAU : Retirer les effets de nuit
        removeBlindnessEffect();
        unfreezePlayers();

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
            if (isPlayerAlive(victim)) {

                // Vérifier la protection du garde
                boolean isProtected = victim.equals(protectedPlayer);

                // Vérifier si la sorcière guérit
                boolean isHealed = victim.equals(witchHealTarget);

                if (isProtected) {
                    someoneProtected = true;
                } else if (isHealed) {
                    someoneHealed = true;
                } else {
                    eliminatePlayer(victim, "dévoré par les loups-garous");
                }
            }
        }

        // Traiter le poison de la sorcière
        if (witchPoisonTarget != null) {
            if (isPlayerAlive(witchPoisonTarget)) {
                eliminatePlayer(witchPoisonTarget, "empoisonné par la sorcière");
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

        SoundManager.playDayStart(getOnlineGamePlayers());

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

        phaseTimerTask = new BukkitRunnable() {
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

        if (plugin.getConfigManager().isFreezingDuringVoteEnabled()) {
            freezePlayers();
        }

        broadcastMessage(Messages.VOTE_START);
        broadcastMessage(Messages.VOTE_DESCRIPTION);
        broadcastMessage("§e💡 Utilisez : §f/vote <joueur> §epour voter");

        SoundManager.playVoteStart(getOnlineGamePlayers());
        showAlivePlayers();

        // Timer pour les votes
        int voteDuration = plugin.getConfigManager().getVoteDuration();
        broadcastMessage("§c⏰ Vous avez " + Utils.formatTime(voteDuration) + " pour voter !");

        phaseTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                processVotes();
            }
        }.runTaskLater(plugin, voteDuration * 20L);

        plugin.getScoreboardManager().updateScoreboard();
    }

    // ← NOUVEAU : Vérifier si tout le monde a voté
    public void checkAllVoted() {
        if (currentState != GameState.VOTE) return;

        int alivePlayers = getAlivePlayersCount();
        int votedPlayers = 0;

        for (UUID voterId : votes.keySet()) {
            if (isPlayerAlive(Bukkit.getPlayer(voterId))) {
                votedPlayers++;
            }
        }

        if (votedPlayers >= alivePlayers) {
            // Tout le monde a voté, annuler le timer et traiter immédiatement
            if (phaseTimerTask != null && !phaseTimerTask.isCancelled()) {
                phaseTimerTask.cancel();
            }

            broadcastMessage("§a✅ Tous les joueurs ont voté ! Dépouillement immédiat...");

            Bukkit.getScheduler().runTaskLater(plugin, this::processVotes, 40L); // 2 secondes de délai
        }
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
                List<UUID> tiedPlayers = new ArrayList<>();
                for (Map.Entry<UUID, Integer> entry : sortedVotes) {
                    if (entry.getValue() == maxVotes) {
                        tiedPlayers.add(entry.getKey());
                    }
                }

                if (tiedPlayers.size() > 1) {
                    // ← NOUVEAU : Égalité, le maire décide
                    handleTieVote(tiedPlayers);
                    return; // On arrête ici, le maire va décider
                } else {
                    eliminatePlayer(mostVoted, "éliminé par vote du village");
                }
            }
        }

        broadcastMessage("§e👥 Total des votants : " + voters.size() + "/" + getAlivePlayersCount());

        if (plugin.getConfigManager().isFreezingDuringVoteEnabled()) {
            unfreezePlayers();
        }

        // Vérifier les conditions de victoire
        if (!checkWinConditions()) {
            // Continuer avec une nouvelle nuit
            Bukkit.getScheduler().runTaskLater(plugin, this::startNight, 100L);
        }
    }

    // ← NOUVEAU : Gestion de l'égalité avec le maire
    private void handleTieVote(List<UUID> tiedPlayers) {
        Player mayorPlayer = Bukkit.getPlayer(mayor);

        if (mayorPlayer == null || !isPlayerAlive(mayorPlayer)) {
            broadcastMessage("§c⚖️ Égalité ! Le maire est mort, personne n'est éliminé.");
            // Continuer avec une nouvelle nuit
            Bukkit.getScheduler().runTaskLater(plugin, this::startNight, 100L);
            return;
        }

        currentState = GameState.MAYOR_VOTE;
        broadcastMessage("§6👑 === VOTE DU MAIRE ===");
        broadcastMessage("§e⚖️ Égalité détectée ! Le maire doit choisir.");

        StringBuilder tiedMessage = new StringBuilder("§eJoueurs à égalité : ");
        for (int i = 0; i < tiedPlayers.size(); i++) {
            Player p = Bukkit.getPlayer(tiedPlayers.get(i));
            if (p != null) {
                tiedMessage.append("§f").append(p.getName());
                if (i < tiedPlayers.size() - 1) {
                    tiedMessage.append("§e, ");
                }
            }
        }
        broadcastMessage(tiedMessage.toString());

        mayorPlayer.sendMessage("§6" + "=".repeat(50));
        mayorPlayer.sendMessage(Utils.centerText("§6§l👑 DÉCISION DU MAIRE 👑", 50));
        mayorPlayer.sendMessage("§6" + "=".repeat(50));
        mayorPlayer.sendMessage("§e⚖️ Il y a égalité ! Vous devez choisir qui éliminer.");
        mayorPlayer.sendMessage("§e💡 Utilisez : §f/vote <joueur>");
        mayorPlayer.sendMessage("");
        mayorPlayer.sendMessage("§eCandidats :");
        for (UUID uuid : tiedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                mayorPlayer.sendMessage("§f  - " + p.getName());
            }
        }
        mayorPlayer.sendMessage("§6" + "=".repeat(50));

        mayorPlayer.playSound(mayorPlayer.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.0f);

        // Stocker temporairement les joueurs à égalité
        votes.clear();
        for (UUID uuid : tiedPlayers) {
            votes.put(uuid, uuid); // Marquer comme candidats
        }

        // Timer pour la décision du maire (30 secondes)
        phaseTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Si le maire n'a pas voté, éliminer aléatoirement
                processMayorVote(tiedPlayers);
            }
        }.runTaskLater(plugin, 600L); // 30 secondes
    }

    // ← NOUVEAU : Traiter le vote du maire
    private void processMayorVote(List<UUID> tiedPlayers) {
        Player mayorPlayer = Bukkit.getPlayer(mayor);

        // Vérifier si le maire a voté
        UUID mayorChoice = null;
        if (votes.containsKey(mayor)) {
            mayorChoice = votes.get(mayor);

            // Vérifier que le choix est valide (dans la liste des égalités)
            if (!tiedPlayers.contains(mayorChoice)) {
                mayorChoice = null;
            }
        }

        UUID eliminatedUuid;
        String eliminatedName;
        
        if (mayorChoice != null) {
            eliminatedUuid = mayorChoice;
            Player p = Bukkit.getPlayer(mayorChoice);
            eliminatedName = (p != null) ? p.getName() : "Joueur hors-ligne";
            broadcastMessage("§6👑 Le maire a choisi : §c" + eliminatedName);
        } else {
            // Le maire n'a pas voté ou vote invalide, choix aléatoire
            UUID randomChoice = tiedPlayers.get(new Random().nextInt(tiedPlayers.size()));
            eliminatedUuid = randomChoice;
            Player p = Bukkit.getPlayer(randomChoice);
            eliminatedName = (p != null) ? p.getName() : "Joueur hors-ligne";
            broadcastMessage("§6⏰ Le maire n'a pas décidé à temps ! Choix aléatoire : §c" + eliminatedName);
        }

        eliminatePlayer(eliminatedUuid, "éliminé par décision du maire");

        // Vérifier les conditions de victoire
        if (!checkWinConditions()) {
            // Continuer avec une nouvelle nuit
            Bukkit.getScheduler().runTaskLater(plugin, this::startNight, 100L);
        }
    }

    private void eliminatePlayer(UUID playerId, String reason) {
        Player player = Bukkit.getPlayer(playerId);
        String playerName = (player != null) ? player.getName() : "Joueur hors-ligne";
        PlayerRole role = players.get(playerId);

        // ← NOUVEAU : Marquer comme mort de façon sûre
        deadPlayers.add(playerId);

        broadcastMessage("§c💀 " + playerName + " a été " + reason + " !");
        broadcastMessage("§6🎭 " + playerName + " était : " + (role != null ? role.getDisplayName() : "§aVillageois"));

        // ← NOUVEAU : Transférer le rôle de maire si nécessaire
        if (playerId.equals(mayor)) {
            // Si le joueur est hors ligne, on ne peut pas appeler transferMayorRole directement avec Player
            // Mais transferMayorRoleOnQuit l'a peut-être déjà fait ?
            // Dans le doute, on gère le transfert
             if (player != null) {
                 transferMayorRole(player);
             } else {
                 // Logique de transfert sans joueur (aléatoire)
                 setNewMayor(null, true);
             }
        }

        // Effets visuels et sonores
        SoundManager.playElimination(getOnlineGamePlayers());

        // ← CORRECTION : Bien mettre en spectateur (si en ligne)
        if (player != null) {
            player.setGameMode(GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            
            // Message privé au joueur éliminé
            player.sendMessage("§c💀 Vous avez été éliminé ! Vous pouvez maintenant observer la partie.");
            player.sendMessage("§7💬 Chattez avec les autres morts ou observez la suite...");
            player.sendMessage("§7👻 Vous êtes maintenant en mode spectateur.");
        }

        // Titre dramatique pour tous
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§c💀 " + playerName, "§6a été " + reason, 10, 60, 20);
        }

        // Statistiques
        if (player != null) {
             plugin.getStatsManager().addDeath(player, reason);
        }
    }

    // Surcharge pour compatibilité
    private void eliminatePlayer(Player player, String reason) {
        eliminatePlayer(player.getUniqueId(), reason);
    }

    // ← NOUVEAU : Transférer le rôle de maire à un autre joueur
    private void transferMayorRole(Player deadMayor) {
        broadcastMessage("§6💔 Le maire est mort ! Il doit choisir son successeur...");

        deadMayor.sendMessage("§6" + "=".repeat(50));
        deadMayor.sendMessage(Utils.centerText("§6§l👑 DÉSIGNATION DU SUCCESSEUR 👑", 50));
        deadMayor.sendMessage("§6" + "=".repeat(50));
        deadMayor.sendMessage("§e💀 Vous êtes mort, mais vous devez désigner un nouveau maire.");
        deadMayor.sendMessage("§e💡 Utilisez : §f/lg maire <joueur>");
        deadMayor.sendMessage("");
        deadMayor.sendMessage("§eJoueurs vivants :");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(p)) {
                deadMayor.sendMessage("§f  - " + p.getName());
            }
        }
        deadMayor.sendMessage("§6" + "=".repeat(50));

        // Timer de 20 secondes pour choisir
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Si pas de choix, désigner aléatoirement
            if (Bukkit.getPlayer(mayor) != null && !isPlayerAlive(Bukkit.getPlayer(mayor))) {
                List<Player> alivePlayers = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isPlayerAlive(p)) {
                        alivePlayers.add(p);
                    }
                }

                if (!alivePlayers.isEmpty()) {
                    Player newMayor = alivePlayers.get(new Random().nextInt(alivePlayers.size()));
                    setNewMayor(newMayor, true);
                }
            }
        }, 400L); // 20 secondes
    }

    // ← NOUVEAU : Définir un nouveau maire
    public void setNewMayor(Player newMayor, boolean random) {
        mayor = newMayor.getUniqueId();

        if (random) {
            broadcastMessage("§6👑 Aucun successeur désigné ! §e" + newMayor.getName() + " §6devient maire par défaut !");
        } else {
            broadcastMessage("§6👑 §e" + newMayor.getName() + " §6est le nouveau maire du village !");
        }

        newMayor.sendMessage("§6" + "=".repeat(50));
        newMayor.sendMessage(Utils.centerText("§6§l👑 VOUS ÊTES LE NOUVEAU MAIRE 👑", 50));
        newMayor.sendMessage("§6" + "=".repeat(50));
        newMayor.sendMessage("§e🎖️ Vous avez été désigné comme nouveau maire !");
        newMayor.sendMessage("§7💡 En cas d'égalité, vous choisirez qui éliminer.");
        newMayor.sendMessage("§6" + "=".repeat(50));

        newMayor.playSound(newMayor.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    /**
     * Gère le transfert du rôle de maire lorsqu'un maire se déconnecte.
     * Le rôle est attribué aléatoirement à un autre joueur vivant.
     */
    public void transferMayorRoleOnQuit() {
        broadcastMessage("§6💔 Le maire a quitté la partie ! Un nouveau maire va être désigné...");

        // Créer une liste de candidats (tous les joueurs vivants sauf le maire actuel)
        List<Player> candidates = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isPlayerAlive(p) && !p.getUniqueId().equals(mayor)) {
                candidates.add(p);
            }
        }

        // S'il y a des candidats, choisir un nouveau maire au hasard
        if (!candidates.isEmpty()) {
            Player newMayor = candidates.get(new Random().nextInt(candidates.size()));
            // Utiliser un petit délai pour que le message soit plus naturel
            Bukkit.getScheduler().runTaskLater(plugin, () -> setNewMayor(newMayor, true), 40L); // 2 secondes
        } else {
            // S'il n'y a pas d'autres joueurs, le rôle de maire est perdu
            mayor = null;
            broadcastMessage("§cIl n'y a plus de joueurs pour devenir maire.");
        }
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

        if (phaseTimerTask != null) {
            phaseTimerTask.cancel();
        }

        // ← NOUVEAU : Nettoyer les effets
        removeBlindnessEffect();
        unfreezePlayers();

        // Enregistrer les statistiques de la partie
        long gameDuration = (System.currentTimeMillis() - gameStartTime) / 1000;
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
                String mayorBadge = player.getUniqueId().equals(mayor) ? " §6👑" : "";
                broadcastMessage(status + " §f" + player.getName() + mayorBadge + " §7: " + role.getDisplayName());
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
        SoundManager.playVictory(getOnlineGamePlayers());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, 20, 100, 40);
            player.setGameMode(GameMode.SURVIVAL);
            player.resetPlayerTime();
            player.setWalkSpeed(0.2f);
            player.setFlySpeed(0.1f);
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
        if (phaseTimerTask != null) {
            phaseTimerTask.cancel();
        }

        // ← NOUVEAU : Nettoyer les effets
        removeBlindnessEffect();
        unfreezePlayers();

        resetGame();
        broadcastMessage("§c⛔ La partie a été arrêtée par un administrateur.");
    }

    private void resetGame() {
        currentState = GameState.WAITING;

        // On ne reset que les joueurs qui étaient dans la partie
        Set<UUID> playersToReset = new HashSet<>(players.keySet());
        playersToReset.addAll(deadPlayers); // Inclure les morts

        resetGameData();

        // Remettre les joueurs en mode normal
        for (UUID uuid : playersToReset) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.SURVIVAL);
                player.resetPlayerTime();
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
                player.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }

        broadcastMessage("§a🔄 Partie réinitialisée ! Utilisez /lg start pour commencer une nouvelle partie.");
        plugin.getScoreboardManager().updateScoreboard();
    }

    private void setupGameEnvironment() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.getInventory().clear();
        }
    }

    private void teleportPlayersToGame() {
        Location gameLocation = plugin.getConfigManager().getSpawnLocation();

        if (gameLocation == null) {
            plugin.getLogger().warning("⚠️ Impossible de récupérer le spawn configuré");
            World world = Bukkit.getWorld(plugin.getConfigManager().getSpawnWorld());
            if (world == null) {
                world = Bukkit.getWorlds().get(0);
            }
            gameLocation = world.getSpawnLocation();
        }

        for (UUID uuid : players.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.teleport(gameLocation);
                player.setGameMode(GameMode.ADVENTURE);
            }
        }

        plugin.getLogger().info("Joueurs téléportés au spawn: " + gameLocation.getWorld().getName() +
                " (" + gameLocation.getBlockX() + ", " + gameLocation.getBlockY() + ", " + gameLocation.getBlockZ() + ")");
    }

    private void showGameStatus() {
        broadcastMessage("§a👥 Joueurs vivants : " + getAlivePlayersCount());
        showAlivePlayers();

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

    private void broadcastMessage(String message) {
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessagePrefix() + " " + message);
    }

    public boolean isPlayerAlive(Player player) {
        return player != null && player.isOnline() &&
                !deadPlayers.contains(player.getUniqueId()) &&
                players.containsKey(player.getUniqueId());
    }

    public boolean isPlayerAlive(UUID playerId) {
        return playerId != null &&
                !deadPlayers.contains(playerId) &&
                players.containsKey(playerId);
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
        if (currentState == GameState.MAYOR_VOTE) {
            // Seul le maire peut voter
            if (!voter.equals(mayor)) {
                return false;
            }
            // Vérifier que la cible est dans les candidats
            if (!votes.containsKey(target)) {
                return false;
            }
        } else if (currentState == GameState.VOTE) {
            if (!isPlayerAlive(Bukkit.getPlayer(voter)) || !isPlayerAlive(Bukkit.getPlayer(target))) {
                return false;
            }
        } else if (currentState == GameState.NIGHT) {
            // This is a werewolf vote
            if (players.get(voter) != PlayerRole.WEREWOLF) {
                return false; // Only werewolves can vote at night
            }
            if (!isPlayerAlive(Bukkit.getPlayer(voter)) || !isPlayerAlive(Bukkit.getPlayer(target))) {
                return false;
            }
        } else {
            return false;
        }

        votes.put(voter, target);

        // ← NOUVEAU : Vérifier si tout le monde a voté
        if (currentState == GameState.VOTE) {
            checkAllVoted();
        }

        return true;
    }

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

    public UUID getMayor() {
        return mayor;
    }

    private void setTimeForPlayers(long time) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (players.containsKey(player.getUniqueId())) {
                player.setPlayerTime(time, false);
            }
        }
    }

    private List<Player> getOnlineGamePlayers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : players.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        return online;
    }
}