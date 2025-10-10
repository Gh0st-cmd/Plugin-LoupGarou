package org.gh0st.loupGarou.role;

/**
 * Énumération définissant tous les rôles disponibles dans le jeu Loup-Garou
 * Chaque rôle a un nom d'affichage, une description et des propriétés spéciales
 */
public enum PlayerRole {

    /**
     * Rôle : Loup-Garou
     * Camp : Loups-Garous
     * Pouvoir : Éliminer un villageois chaque nuit
     */
    WEREWOLF(
            "§c🐺 Loup-Garou",
            "§cVous faites partie de la meute des loups-garous. Votre objectif est d'éliminer tous les villageois. " +
                    "Chaque nuit, vous pouvez choisir une victime avec §f/lg tuer <joueur>§c. " +
                    "Communiquez avec les autres loups en utilisant §f!§c au début de vos messages la nuit.",
            "werewolves",
            true,
            true,
            false
    ),

    /**
     * Rôle : Villageois
     * Camp : Village
     * Pouvoir : Aucun pouvoir spécial, vote pendant la journée
     */
    VILLAGER(
            "§a👨‍🌾 Villageois",
            "§aVous êtes un simple villageois. Votre objectif est d'éliminer tous les loups-garous en votant pendant la journée. " +
                    "Vous n'avez aucun pouvoir spécial, mais votre vote et votre perspicacité sont essentiels pour la victoire du village. " +
                    "Discutez, enquêtez et votez judicieusement avec §f/vote <joueur>§a.",
            "village",
            false,
            false,
            false
    ),

    /**
     * Rôle : Voyante
     * Camp : Village
     * Pouvoir : Découvrir le rôle d'un joueur chaque nuit
     */
    SEER(
            "§5🔮 Voyante",
            "§5Vous possédez le don de clairvoyance. Chaque nuit, vous pouvez découvrir le vrai rôle d'un joueur " +
                    "avec §f/lg voir <joueur>§5. Utilisez cette information pour guider le village vers la victoire, " +
                    "mais attention à ne pas vous faire repérer par les loups-garous ! Votre survie est cruciale.",
            "village",
            false,
            true,
            true
    ),

    /**
     * Rôle : Garde (Chasseur/Protecteur)
     * Camp : Village
     * Pouvoir : Protéger un joueur des attaques chaque nuit
     */
    GUARD(
            "§b🛡️ Garde",
            "§bVous êtes le protecteur du village. Chaque nuit, vous pouvez protéger un joueur des attaques " +
                    "avec §f/lg proteger <joueur>§b. Le joueur protégé survivra à une attaque de loup-garou cette nuit-là. " +
                    "Vous pouvez vous protéger vous-même, mais seulement §lune seule fois§b par partie. Choisissez bien !",
            "village",
            false,
            true,
            true
    ),

    /**
     * Rôle : Sorcière
     * Camp : Village
     * Pouvoir : Une potion de vie et une potion de mort à usage unique
     */
    WITCH(
            "§d🧪 Sorcière",
            "§dVous maîtrisez les arts occultes et possédez deux potions magiques à usage unique. " +
                    "§l1) Potion de Vie§d : §f/lg soigner <joueur>§d - Ressuscite un joueur qui vient d'être tué. " +
                    "§l2) Potion de Mort§d : §f/lg empoisonner <joueur>§d - Tue instantanément un joueur. " +
                    "Utilisez ces pouvoirs avec sagesse, car vous ne pourrez les utiliser qu'une seule fois chacun !",
            "village",
            false,
            true,
            true
    );

    // Propriétés du rôle
    private final String displayName;
    private final String description;
    private final String team;
    private final boolean isWerewolf;
    private final boolean hasNightAction;
    private final boolean isSpecialRole;

    /**
     * Constructeur de l'énumération
     * @param displayName Nom affiché du rôle avec couleurs
     * @param description Description complète du rôle
     * @param team Équipe du rôle ("werewolves" ou "village")
     * @param isWerewolf Si le rôle fait partie des loups-garous
     * @param hasNightAction Si le rôle peut agir la nuit
     * @param isSpecialRole Si le rôle a des capacités spéciales
     */
    PlayerRole(String displayName, String description, String team, boolean isWerewolf, boolean hasNightAction, boolean isSpecialRole) {
        this.displayName = displayName;
        this.description = description;
        this.team = team;
        this.isWerewolf = isWerewolf;
        this.hasNightAction = hasNightAction;
        this.isSpecialRole = isSpecialRole;
    }

    /**
     * @return Le nom d'affichage du rôle avec formatage couleur
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return La description complète du rôle
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return L'équipe du rôle ("werewolves" ou "village")
     */
    public String getTeam() {
        return team;
    }

    /**
     * @return true si le rôle fait partie des loups-garous
     */
    public boolean isWerewolf() {
        return isWerewolf;
    }

    /**
     * @return true si le rôle peut effectuer des actions la nuit
     */
    public boolean hasNightAction() {
        return hasNightAction;
    }

    /**
     * @return true si le rôle a des capacités spéciales
     */
    public boolean isSpecialRole() {
        return isSpecialRole;
    }

    /**
     * @return true si le rôle fait partie du village
     */
    public boolean isVillage() {
        return team.equals("village");
    }

    /**
     * @return Le nom simple du rôle sans formatage
     */
    public String getSimpleName() {
        switch (this) {
            case WEREWOLF: return "Loup-Garou";
            case VILLAGER: return "Villageois";
            case SEER: return "Voyante";
            case GUARD: return "Garde";
            case WITCH: return "Sorcière";
            default: return "Inconnu";
        }
    }

    /**
     * @return L'émoji représentant le rôle
     */
    public String getEmoji() {
        switch (this) {
            case WEREWOLF: return "🐺";
            case VILLAGER: return "👨‍🌾";
            case SEER: return "🔮";
            case GUARD: return "🛡️";
            case WITCH: return "🧪";
            default: return "❓";
        }
    }

    /**
     * @return La couleur principale du rôle (code couleur Minecraft)
     */
    public String getColor() {
        switch (this) {
            case WEREWOLF: return "§c";
            case VILLAGER: return "§a";
            case SEER: return "§5";
            case GUARD: return "§b";
            case WITCH: return "§d";
            default: return "§f";
        }
    }

    /**
     * @return La priorité du rôle pour l'ordre d'action la nuit (plus bas = plus tôt)
     */
    public int getNightPriority() {
        switch (this) {
            case SEER: return 1;      // La voyante agit en premier
            case GUARD: return 2;     // Puis le garde
            case WITCH: return 3;     // Puis la sorcière
            case WEREWOLF: return 4;  // Enfin les loups-garous
            default: return 999;      // Les autres n'agissent pas
        }
    }

    /**
     * @return Les commandes disponibles pour ce rôle
     */
    public String[] getAvailableCommands() {
        switch (this) {
            case WEREWOLF:
                return new String[]{"/lg tuer <joueur>"};
            case SEER:
                return new String[]{"/lg voir <joueur>"};
            case GUARD:
                return new String[]{"/lg proteger <joueur>"};
            case WITCH:
                return new String[]{"/lg soigner <joueur>", "/lg empoisonner <joueur>"};
            default:
                return new String[]{"/vote <joueur>"};
        }
    }

    /**
     * @return Un conseil stratégique pour ce rôle
     */
    public String getStrategyTip() {
        switch (this) {
            case WEREWOLF:
                return "§c💡 Tip : Mélangez-vous aux villageois pendant la journée et coordonnez-vous la nuit !";
            case VILLAGER:
                return "§a💡 Tip : Observez les comportements suspects et posez les bonnes questions !";
            case SEER:
                return "§5💡 Tip : Gardez vos découvertes secrètes jusqu'au bon moment pour les révéler !";
            case GUARD:
                return "§b💡 Tip : Protégez les rôles importants, mais variez vos choix pour rester imprévisible !";
            case WITCH:
                return "§d💡 Tip : Utilisez vos potions au moment crucial, elles peuvent changer le cours du jeu !";
            default:
                return "§f💡 Bonne chance !";
        }
    }

    /**
     * @return true si ce rôle peut être ciblé par les loups-garous
     */
    public boolean canBeKilledByWerewolves() {
        return !isWerewolf; // Tous les rôles sauf les loups-garous
    }

    /**
     * @return true si ce rôle compte pour la victoire du village
     */
    public boolean countsForVillageVictory() {
        return isVillage();
    }

    /**
     * @return true si ce rôle compte pour la victoire des loups-garous
     */
    public boolean countsForWerewolfVictory() {
        return isWerewolf();
    }

    /**
     * Obtient un rôle aléatoire selon les probabilités
     * @return Un rôle aléatoire
     */
    public static PlayerRole getRandomRole() {
        PlayerRole[] roles = values();
        return roles[(int) (Math.random() * roles.length)];
    }

    /**
     * Obtient un rôle par son nom simple
     * @param name Le nom du rôle
     * @return Le rôle correspondant ou null
     */
    public static PlayerRole getByName(String name) {
        for (PlayerRole role : values()) {
            if (role.getSimpleName().equalsIgnoreCase(name) ||
                    role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }

    /**
     * @return Tous les rôles qui font partie du village
     */
    public static PlayerRole[] getVillageRoles() {
        return new PlayerRole[]{VILLAGER, SEER, GUARD, WITCH};
    }

    /**
     * @return Tous les rôles qui font partie des loups-garous
     */
    public static PlayerRole[] getWerewolfRoles() {
        return new PlayerRole[]{WEREWOLF};
    }

    /**
     * @return Tous les rôles spéciaux (avec des pouvoirs)
     */
    public static PlayerRole[] getSpecialRoles() {
        return new PlayerRole[]{SEER, GUARD, WITCH};
    }

    /**
     * @return Description formatée du rôle pour l'aide
     */
    public String getHelpDescription() {
        StringBuilder help = new StringBuilder();
        help.append(getDisplayName()).append("\n");
        help.append("§7Équipe : ").append(isWerewolf() ? "§cLoups-Garous" : "§aVillage").append("\n");

        if (hasNightAction()) {
            help.append("§7Commandes : ");
            String[] commands = getAvailableCommands();
            for (int i = 0; i < commands.length; i++) {
                help.append("§f").append(commands[i]);
                if (i < commands.length - 1) {
                    help.append("§7, ");
                }
            }
            help.append("\n");
        }

        help.append("§7Description : §f").append(getDescription());

        return help.toString();
    }

    /**
     * @return Représentation textuelle du rôle
     */
    @Override
    public String toString() {
        return getDisplayName();
    }
}
