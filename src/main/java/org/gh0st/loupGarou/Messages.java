package org.gh0st.loupGarou;

public class Messages {

    // Messages de démarrage
    public static final String GAME_STARTING = "§a🎮 La partie de Loup-Garou commence !";
    public static final String ROLES_DISTRIBUTING = "§e📋 Distribution des rôles en cours...";
    public static final String NOT_ENOUGH_PLAYERS = "§c❌ Il faut au moins {0} joueurs pour commencer !";

    // Messages de phases
    public static final String NIGHT_START = "§1🌙 === NUIT {0} ===";
    public static final String NIGHT_DESCRIPTION = "§9💤 Le village s'endort... Les créatures de la nuit se réveillent !";
    public static final String DAY_START = "§e☀️ === JOUR {0} ===";
    public static final String DAY_DESCRIPTION = "§6🌅 Le soleil se lève sur le village...";
    public static final String VOTE_START = "§c🗳️ === PHASE DE VOTE ===";
    public static final String VOTE_DESCRIPTION = "§c⚖️ Il est temps de voter pour éliminer un suspect !";

    // Messages de rôles
    public static final String WEREWOLF_PHASE = "§c🐺 Phase des Loups-Garous !";
    public static final String WEREWOLF_CHOOSE = "§c💀 Choisissez votre victime avec : /lg tuer <joueur>";
    public static final String SEER_VISION = "§5🔮 Vision révélée : {0} est {1}";
    public static final String GUARD_PROTECTION = "§b🛡️ Vous protégez {0} cette nuit !";
    public static final String WITCH_HEAL = "§d🧪 Vous avez utilisé votre potion de soin sur {0} !";
    public static final String WITCH_POISON = "§d☠️ Vous avez empoisonné {0} !";

    // Messages de fin
    public static final String VILLAGE_VICTORY = "§a🎉 VICTOIRE DU VILLAGE !";
    public static final String VILLAGE_VICTORY_REASON = "§2✅ Tous les loups-garous ont été éliminés !";
    public static final String WEREWOLF_VICTORY = "§c🐺 VICTOIRE DES LOUPS-GAROUS !";
    public static final String WEREWOLF_VICTORY_REASON = "§4💀 Les loups-garous dominent le village !";

    // Messages d'erreur
    public static final String NO_PERMISSION = "§c❌ Vous n'avez pas la permission d'utiliser cette commande.";
    public static final String PLAYER_ONLY = "§c❌ Cette commande ne peut être utilisée que par un joueur.";
    public static final String PLAYER_NOT_FOUND = "§c❌ Joueur introuvable.";
    public static final String NOT_YOUR_TURN = "§c❌ Ce n'est pas votre tour d'agir.";
    public static final String ALREADY_USED = "§c❌ Vous avez déjà utilisé cette capacité !";
    public static final String INVALID_TARGET = "§c❌ Cible invalide.";
    public static final String GAME_NOT_RUNNING = "§c❌ Aucune partie en cours.";
    public static final String GAME_ALREADY_RUNNING = "§c❌ Une partie est déjà en cours.";

    // Messages informatifs
    public static final String VOTE_REGISTERED = "§e🗳️ Votre vote a été enregistré !";
    public static final String VOTE_ANONYMOUS = "§7📊 Un vote a été enregistré...";
    public static final String PROTECTED_SOMEONE = "§b🛡️ Cette nuit, le garde a protégé quelqu'un !";
    public static final String WITCH_SAVED = "§d💚 Cette nuit, la sorcière a sauvé une vie !";
    public static final String QUIET_NIGHT = "§7😴 Cette nuit s'est passée sans incident...";

    // Utilitaire pour formater les messages avec paramètres
    public static String format(String message, Object... args) {
        String result = message;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
    }

    // Messages de bienvenue
    public static final String WELCOME_GAME_RUNNING = "§e🎮 Une partie est en cours ! Vous pourrez participer à la prochaine.";
    public static final String WELCOME_NORMAL = "§a👋 Bienvenue ! Utilisez §f/lg aide §apour commencer.";

    // Messages de chat
    public static final String CHAT_DEAD_PREFIX = "§8💀 [Morts] ";
    public static final String CHAT_WEREWOLF_PREFIX = "§c🐺 [Loups] ";
    public static final String CHAT_SPECTATOR_PREFIX = "§8[Spectateur] ";
}