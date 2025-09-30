# 🐺 Plugin Loup-Garou pour Minecraft

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-brightgreen.svg)](https://www.minecraft.net/)
[![Spigot](https://img.shields.io/badge/Spigot-Compatible-orange.svg)](https://www.spigotmc.org/)
[![Paper](https://img.shields.io/badge/Paper-Compatible-blue.svg)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-17+-red.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> Plugin Minecraft complet et immersif pour jouer au célèbre jeu du Loup-Garou directement sur votre serveur ! 🎮

## 📖 Description

**LoupGarou** est un plugin Minecraft entièrement en français qui permet de jouer au jeu du Loup-Garou (Werewolf) avec vos amis sur votre serveur Minecraft. Le plugin offre une expérience complète avec tous les rôles classiques, un système de phases automatiques, des statistiques de joueurs et une intégration WorldGuard pour limiter le jeu à une zone spécifique.

## ✨ Fonctionnalités principales

### 🎭 Rôles disponibles
- **🐺 Loup-Garou** : Éliminez les villageois chaque nuit
- **👨‍🌾 Villageois** : Trouvez et éliminez les loups-garous par vote
- **🔮 Voyante** : Découvrez le rôle d'un joueur chaque nuit
- **🛡️ Garde** : Protégez un joueur des attaques nocturnes
- **🧪 Sorcière** : Utilisez vos potions de vie et de mort (usage unique)

### 🎮 Système de jeu complet
- ✅ **Phases automatiques** : Nuit → Jour → Vote → Répéter
- ✅ **Durées configurables** pour chaque phase
- ✅ **Conditions de victoire** automatiques
- ✅ **Chat spécialisé** pour les morts et les loups-garous
- ✅ **Scoreboard dynamique** avec affichage du rôle personnel
- ✅ **Effets sonores** immersifs selon les phases
- ✅ **Système de vote** intégré avec comptage automatique

### 🌍 Intégration WorldGuard
- ✅ **Zone de jeu dédiée** : Restreint le jeu à une région WorldGuard spécifique
- ✅ **Monde configurable** : Choisissez le monde de jeu
- ✅ **Spawn personnalisé** : Définissez le point de spawn avec `/lg setspawn`
- ✅ **Compatible sans WorldGuard** : Fonctionne aussi sans restriction de zone

### 📊 Fonctionnalités avancées
- ✅ **Statistiques persistantes** : Parties jouées, victoires, morts
- ✅ **Configuration complète** : Durées, ratios de rôles, messages personnalisables
- ✅ **Compatible LuckPerms** : Ne modifie pas les préfixes de chat hors partie
- ✅ **Multi-monde** : N'affecte que les joueurs participant à la partie
- ✅ **Système de permissions** : Gestion fine des accès
- ✅ **Commande reload** : Rechargez la config sans redémarrer le serveur

## 🚀 Installation

### Prérequis
- **Minecraft** 1.21+ (Spigot/Paper)
- **Java** 17 ou supérieur
- **WorldGuard** 7.0+ (optionnel)

### Étapes d'installation

1. **Téléchargez** le fichier `LoupGarou-1.0.0.jar` depuis les [releases](../../releases)

2. **Placez** le fichier dans le dossier `plugins/` de votre serveur

3. **Redémarrez** votre serveur

4. **Configurez** (optionnel) le fichier `plugins/LoupGarou/config.yml`

5. **Créez une région WorldGuard** (optionnel) :
   ```
   /rg define loupgarou
   ```

6. **Définissez le spawn** :
   ```
   /lg setspawn
   ```

7. **Lancez une partie** :
   ```
   /lg start
   ```

## 🎯 Commandes

### Commandes d'administration
| Commande | Description | Permission |
|----------|-------------|------------|
| `/lg start` | Démarrer une partie | `loupgarou.admin` |
| `/lg stop` | Arrêter la partie en cours | `loupgarou.admin` |
| `/lg reload` | Recharger la configuration | `loupgarou.admin` |
| `/lg setspawn` | Définir le spawn de jeu | `loupgarou.admin` |
| `/lg statut` | Voir le statut de la partie | `loupgarou.use` |
| `/lg liste` | Voir la liste des joueurs | `loupgarou.use` |

### Commandes de jeu
| Commande | Description | Permission |
|----------|-------------|------------|
| `/vote <joueur>` | Voter pour éliminer un joueur | `loupgarou.vote` |
| `/lg stats` | Voir ses statistiques | `loupgarou.use` |
| `/lg aide` | Afficher l'aide | `loupgarou.use` |

### Commandes par rôle
| Commande | Rôle | Description |
|----------|------|-------------|
| `/lg tuer <joueur>` | 🐺 Loup-Garou | Choisir une victime la nuit |
| `/lg voir <joueur>` | 🔮 Voyante | Découvrir le rôle d'un joueur |
| `/lg proteger <joueur>` | 🛡️ Garde | Protéger un joueur la nuit |
| `/lg soigner <joueur>` | 🧪 Sorcière | Ressusciter un joueur |
| `/lg empoisonner <joueur>` | 🧪 Sorcière | Tuer un joueur |

## ⚙️ Configuration

Le fichier `config.yml` est automatiquement créé au premier démarrage :

```yaml
# Configuration du jeu
game:
  min-players: 4          # Nombre minimum de joueurs
  max-players: 20         # Nombre maximum de joueurs
  night-duration: 60      # Durée de la nuit (secondes)
  day-duration: 120       # Durée du jour (secondes)
  vote-duration: 60       # Durée du vote (secondes)
  auto-restart: true      # Redémarrage automatique
  restart-delay: 30       # Délai avant redémarrage (secondes)

# Configuration du spawn
spawn:
  world: "minijeux"       # Monde de jeu
  x: 0.5                  # Coordonnée X
  y: 100.0                # Coordonnée Y
  z: 0.5                  # Coordonnée Z
  yaw: 0.0                # Rotation horizontale
  pitch: 0.0              # Rotation verticale

# Intégration WorldGuard
worldguard:
  enabled: true                  # Activer WorldGuard
  region-name: "loupgarou"       # Nom de la région
  world-name: "minijeux"         # Monde de la région
  check-on-join: true            # Vérifier à la connexion

# Configuration des rôles
roles:
  werewolf:
    ratio: 0.25                  # 1 loup pour 4 joueurs
  seer:
    min-players: 6               # Minimum pour la voyante
  guard:
    min-players: 8               # Minimum pour le garde
  witch:
    min-players: 10              # Minimum pour la sorcière

# Scoreboard
scoreboard:
  enabled: true                  # Activer le scoreboard
  only-in-region: true           # Afficher seulement dans la région
  update-interval: 20            # Intervalle de mise à jour (ticks)
```

## 🎨 Captures d'écran

### Scoreboard avec rôle personnel
```
╔════════════════╗
║ 🐺 LOUP-GAROU  ║
║                ║
║ 🎭 Votre rôle: ║
║ 🔮 Voyante     ║
║                ║
║ 📊 État: Nuit  ║
║ 🌅 Jour: 3     ║
║                ║
║ 🐺 Loups: 2    ║
║ 👥 Village: 5  ║
║                ║
║ ✅ Vivant      ║
╚════════════════╝
```

### Messages de chat
```
[Nuit] 👤 Joueur1 » Qui est suspect ?
[Loups] 🐺 LoupGarou1 » On tue Joueur2 !
[Morts] 💀 Joueur3 » Ah je savais !
```

## 🔧 Compilation

### Avec Maven
```bash
git clone https://github.com/votre-nom/LoupGarouPlugin.git
cd LoupGarouPlugin
mvn clean package
```

Le fichier `.jar` sera généré dans `target/LoupGarou-1.0.0.jar`

### Avec Gradle
```bash
git clone https://github.com/votre-nom/LoupGarouPlugin.git
cd LoupGarouPlugin
gradle build
```

Le fichier `.jar` sera généré dans `build/libs/LoupGarou-1.0.0.jar`

## 📋 Permissions

| Permission | Description | Défaut |
|------------|-------------|--------|
| `loupgarou.admin` | Accès aux commandes d'administration | OP |
| `loupgarou.use` | Utiliser les commandes de base | Tous |
| `loupgarou.vote` | Pouvoir voter | Tous |
| `loupgarou.bypass` | Contourner les restrictions en jeu | OP |

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à :

1. 🍴 **Fork** le projet
2. 🔨 **Créer** une branche (`git checkout -b feature/amélioration`)
3. 💾 **Commit** vos changements (`git commit -m 'Ajout d'une fonctionnalité'`)
4. 📤 **Push** vers la branche (`git push origin feature/amélioration`)
5. 🎉 **Ouvrir** une Pull Request

## 📝 Roadmap

- [ ] Ajout de nouveaux rôles (Cupidon, Chasseur, Ancien, etc.)
- [ ] Système de parties classées
- [ ] Intégration Discord pour notifications
- [ ] Support multi-langues
- [ ] Interface graphique (GUI)
- [ ] Mode spectateur amélioré
- [ ] Enregistrement et replay des parties

## 🐛 Rapporter un bug

Si vous trouvez un bug, merci de créer une [issue](../../issues) avec :
- 📝 Description détaillée du problème
- 🔄 Étapes pour reproduire
- 📸 Captures d'écran si possible
- 📊 Version du plugin, de Minecraft et du serveur
- 📄 Logs d'erreur

## 💬 Support

- 📖 **Wiki** : [Lien vers le wiki](../../wiki)
- 💬 **Discord** : [Rejoindre le serveur Discord](#)
- 📧 **Email** : votre-email@exemple.com

## 📜 Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 👏 Remerciements

- Merci à la communauté Spigot/Paper pour leurs ressources
- Inspiré du jeu de société Loup-Garou de Thiercelieux
- Merci à tous les contributeurs et testeurs

## 📊 Statistiques

![GitHub stars](https://img.shields.io/github/stars/Gh0st-cmd/Plugin-LoupGarou-Minecraft?style=social)
![GitHub forks](https://img.shields.io/github/forks/Gh0st-cmd/Plugin-LoupGarou-Minecraft?style=social)
![GitHub issues](https://img.shields.io/github/issues/Gh0st-cmd/Plugin-LoupGarou-Minecraft)
![GitHub pull requests](https://img.shields.io/github/issues-pr/Gh0st-cmd/Plugin-LoupGarou-Minecraft)

---

<p align="center">
  Développé avec ❤️ pour la communauté Minecraft francophone 🇫🇷
</p>

<p align="center">
  <strong>🐺 Bon jeu et que le meilleur gagne ! 🎮</strong>
</p>
