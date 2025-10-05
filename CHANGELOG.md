# Changelog Loup-Garou
Ce document répertorie les changements notables pour chaque version du plugin Loup-Garou.

 # [1.0.2] - 2025-10-05

Cette mise à jour se concentre sur la stabilisation du processus de build pour garantir la compatibilité avec les plateformes modernes (Java 21) et intègre des statistiques d'utilisation anonymes.

# 🛠️ Corrections de Bugs et Stabilité

Correction du Build (Java 21) : Résolution du bug de build Unsupported class file major version 65 lors de l'exécution de la tâche shadowJar sur les environnements Java 21. Le processus de packaging est désormais stable et utilise la Java Toolchain 21 de manière cohérente.

Nettoyage Gradle : Optimisation et correction des scripts Gradle (build.gradle) pour une meilleure performance et pour suivre les bonnes pratiques de configuration (Toolchain, compatibilité, dépendances des tâches).

Suppression des Avertissements : Début de la migration des méthodes dépréciées de l'API Bukkit (ChatColor, broadcastMessage, sendTitle) vers l'API moderne Adventure (Kyori), réduisant ainsi le nombre d'avertissements lors de la compilation. (Note : Les autres warnings dépréciés feront l'objet de mises à jour futures.)

# ✨ Améliorations et Nouvelles Fonctionnalités

Ajout des Statistiques (bStats) : Intégration de bStats (v3.0.2) pour la collecte de données anonymes. Ces statistiques nous aident à comprendre l'utilisation du plugin (versions de Minecraft, nombre de serveurs, etc.) afin de prioriser les développements futurs.

Note : Les statistiques sont entièrement anonymes et peuvent être désactivées dans le fichier de configuration de bStats si vous le souhaitez.

Relocation des Dépendances : Les dépendances internes (bStats, GSON) sont désormais déplacées dans un namespace dédié du plugin (org.gh0st.loupgarou.bstats et org.gh0st.loupgarou.libs.gson) pour éviter tout conflit avec d'autres plugins sur votre serveur.
