# Changelog Loup-Garou

## [1.0.3] - 08/11/2025

Cette mise à jour majeure introduit le **système du Maire** et corrige plusieurs bugs critiques pour améliorer l'expérience de jeu !

---

## ✨ Nouvelles Fonctionnalités

### 👑 Système du Maire
- **Élection automatique** : Un maire est élu aléatoirement au début de chaque partie
- **Départage des votes** : En cas d'égalité lors des votes, le maire choisit qui éliminer
- **Succession** : Si le maire meurt, il doit désigner son successeur avec `/lg maire <joueur>`
- **Succession automatique** : Si aucun successeur n'est désigné sous 20 secondes, un nouveau maire est élu aléatoirement
- **Badge visuel** : Le maire est identifiable avec un badge 👑 dans `/lg liste`
- **Notification sonore** : Le maire reçoit un son spécial lors de son élection
- **Phase dédiée** : Phase `MAYOR_VOTE` où seul le maire peut voter en cas d'égalité
- **Configurable** : Activez/désactivez avec `game.enable-mayor: true` dans config.yml

### 🌙 Effets de Nuit Immersifs
- **Effet de Blindness** : Les joueurs (sauf les loups-garous) reçoivent un effet de cécité pendant la nuit
- **Atmosphère renforcée** : Simule l'obscurité et le danger de la nuit
- **Retrait automatique** : L'effet disparaît au lever du jour
- **Configurable** : Activez/désactivez avec `game.blindness-at-night: true`

### ⭕ Placement en Cercle (Optionnel)
- **Formation circulaire** : Les joueurs peuvent être placés en cercle autour du spawn la nuit
- **Immobilisation** : Option pour empêcher les joueurs de bouger pendant la nuit
- **Rayon adaptatif** : Le cercle s'adapte automatiquement au nombre de joueurs
- **Vue centrée** : Les joueurs regardent tous vers le centre du cercle
- **Configurable** : Activez/désactivez avec `game.freeze-players-at-night: false`

---

## 🐛 Corrections de Bugs

### Vote et Phases
- ✅ **Vote anticipé** : Si tous les joueurs ont voté, le dépouillement se fait immédiatement
- ✅ **Annulation du timer** : Le timer de vote est annulé quand tout le monde a voté
- ✅ **Message de confirmation** : "Tous les joueurs ont voté ! Dépouillement immédiat..."
- ✅ **Délai de 2 secondes** : Pour permettre aux joueurs de voir leur vote enregistré

### Mode Spectateur
- ✅ **Correction critique** : Les joueurs éliminés sont maintenant **correctement** mis en mode spectateur
- ✅ **Suivi des morts** : Nouveau système avec `Set<UUID> deadPlayers` pour un tracking précis
- ✅ **Capacités de spectateur** : Les morts peuvent voler et traverser les blocs
- ✅ **Vérification améliorée** : La méthode `isPlayerAlive()` utilise maintenant le Set au lieu du GameMode

### Gestion des Effets
- ✅ **Nettoyage des effets** : Les effets de cécité sont retirés à la fin de la nuit
- ✅ **Restauration de mobilité** : Les vitesses de marche/vol sont restaurées après immobilisation
- ✅ **Nettoyage en cas d'arrêt** : Les effets sont supprimés si la partie est arrêtée manuellement

---

## 🎮 Nouvelles Commandes

### `/lg maire <joueur>`
- **Utilisation** : Permet au maire décédé de désigner son successeur
- **Permission** : Automatique pour le maire mort
- **Restriction** : Ne peut désigner qu'un joueur vivant
- **Timer** : 20 secondes pour décider

### `/vote <joueur>` (Amélioré)
- Fonctionne maintenant aussi pendant la phase `MAYOR_VOTE`
- Seul le maire peut voter pendant cette phase
- Validation automatique des candidats à égalité

---

## ⚙️ Nouvelles Options de Configuration

```yaml
game:
  # Système du Maire
  enable-mayor: true              # Activer/désactiver le système de maire
  
  # Effets de Nuit
  blindness-at-night: true        # Effet de cécité pendant la nuit
  freeze-players-at-night: false  # Immobiliser les joueurs en cercle la nuit
```

---

## 🔄 Améliorations Techniques

### Architecture
- Nouveau state `MAYOR_VOTE` dans l'enum `GameState`
- Méthode `checkAllVoted()` pour vérifier si tous les joueurs ont voté
- Méthode `handleTieVote()` pour gérer les égalités
- Méthode `processMayorVote()` pour traiter le vote du maire
- Méthode `transferMayorRole()` pour la succession du maire
- Méthode `setNewMayor()` pour définir un nouveau maire

### Gestion des Timers
- Nouveau `BukkitTask phaseTimerTask` pour pouvoir annuler les timers
- Annulation automatique quand tous les joueurs ont voté
- Annulation lors de l'arrêt de la partie

### Effets et Immobilisation
- Méthode `applyBlindnessEffect()` pour appliquer la cécité
- Méthode `removeBlindnessEffect()` pour retirer la cécité
- Méthode `freezePlayers()` pour immobiliser en cercle
- Méthode `unfreezePlayers()` pour restaurer la mobilité

---

## 📊 Statistiques et Affichage

### Badge du Maire
- Ajout du badge §6👑 dans `/lg liste` à côté du nom du maire
- Visible pour tous les joueurs
- Affiché même dans la révélation des rôles en fin de partie

### Messages Améliorés
- Messages d'élection du maire avec formatage spécial
- Messages de succession avec explications claires
- Messages d'égalité avec liste des candidats
- Messages de décision du maire

---

## 🎨 Expérience Utilisateur

### Retour Visuel
- **Titres personnalisés** : Écrans de titre pour l'élection du maire
- **Sons immersifs** : Sons spéciaux pour les événements du maire
- **Effets visuels** : Effet de cécité pour renforcer l'atmosphère nocturne
- **Messages formatés** : Boîtes de messages avec bordures pour les annonces importantes

### Clarté de l'Information
- Instructions claires pour le maire en cas d'égalité
- Liste des candidats affichée clairement
- Compte à rebours pour les décisions du maire
- Feedback immédiat après chaque action

---

## 🔧 Compatibilité

- **Minecraft** : 1.21.8+
- **Serveur** : Spigot/Paper
- **Java** : 21+
- **WorldGuard** : 7.0+ (optionnel)

---

## 📝 Notes de Migration

### Depuis la version 1.0.2
1. Aucune action requise, la configuration se met à jour automatiquement
2. Les nouvelles options sont ajoutées avec des valeurs par défaut
3. Les parties en cours ne sont pas affectées
4. Redémarrez simplement le serveur après la mise à jour

### Configuration Recommandée
Pour une expérience optimale :
```yaml
game:
  enable-mayor: true              # Recommandé ON
  blindness-at-night: true        # Recommandé ON pour l'immersion
  freeze-players-at-night: false  # Recommandé OFF (expérimental)
```

---

## 🐺 Remerciements

Merci à tous les joueurs qui ont signalé les bugs et suggéré le système du maire !

Un grand merci à la communauté pour son soutien continu. 🙏

---

## 📥 Installation

1. **Sauvegardez** votre configuration actuelle (optionnel)
2. **Téléchargez** le fichier `LoupGarou-1.0.3.jar`
3. **Remplacez** l'ancien fichier dans `plugins/`
4. **Redémarrez** le serveur
5. **Profitez** des nouvelles fonctionnalités !

---

## 🔗 Liens Utiles

- **GitHub** : [Plugin-LoupGarou](https://github.com/Gh0st-cmd/Plugin-LoupGarou)
- **Discord** : [Rejoindre le serveur](https://discord.gg/DGeGB5cmxQ)
- **bStats** : [Statistiques du plugin](https://bstats.org/plugin/bukkit/loup-garou)
- **Documentation** : [Wiki](https://github.com/Gh0st-cmd/Plugin-LoupGarou/wiki)

---

<p align="center">
  <strong>🐺 Bon jeu et longue vie au Maire ! 👑</strong>
</p>

---

## Historique des Versions

- **[1.0.3]** - 2025-01-09 : Système du Maire + Corrections majeures
- **[1.0.2]** - 2025-10-05 : Stabilisation build Java 21 + bStats
- **[1.0.1]** - Version initiale avec rôles de base
- **[1.0.0]** - Première release publique
