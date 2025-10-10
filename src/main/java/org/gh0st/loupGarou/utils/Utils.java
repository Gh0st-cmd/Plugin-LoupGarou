package org.gh0st.loupGarou.utils;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Utils {

    private static final Random random = new Random();

    /**
     * Formate un temps en secondes vers un format lisible
     */
    public static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        if (remainingSeconds == 0) {
            return minutes + "m";
        } else {
            return minutes + "m " + remainingSeconds + "s";
        }
    }

    /**
     * Sélectionne des joueurs aléatoires
     */
    public static List<Player> getRandomPlayers(int count) {
        List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        List<Player> selectedPlayers = new ArrayList<>();

        for (int i = 0; i < Math.min(count, allPlayers.size()); i++) {
            int randomIndex = random.nextInt(allPlayers.size());
            selectedPlayers.add(allPlayers.remove(randomIndex));
        }

        return selectedPlayers;
    }

    /**
     * Centre un texte dans une chaîne de caractères de longueur donnée
     */
    public static String centerText(String text, int length) {
        if (text.length() >= length) {
            return text;
        }

        int padding = (length - stripColorCodes(text).length()) / 2;
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < padding; i++) {
            builder.append(" ");
        }

        builder.append(text);

        while (stripColorCodes(builder.toString()).length() < length) {
            builder.append(" ");
        }

        return builder.toString();
    }

    /**
     * Supprime les codes couleur d'un texte pour calculer la vraie longueur
     */
    private static String stripColorCodes(String text) {
        return text.replaceAll("§.", "");
    }

    /**
     * Envoie un message centré à un joueur
     */
    public static void sendCenteredMessage(Player player, String message) {
        player.sendMessage(centerText(message, 50));
    }

    /**
     * Vérifie si une chaîne est un nombre entier
     */
    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Obtient un nombre aléatoire entre min et max (inclus)
     */
    public static int randomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Crée une barre de progression textuelle
     */
    public static String createProgressBar(int current, int max, int length) {
        double percentage = (double) current / max;
        int filledLength = (int) (length * percentage);

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filledLength; i++) {
            bar.append("█");
        }
        bar.append("§7");
        for (int i = filledLength; i < length; i++) {
            bar.append("█");
        }

        return bar.toString();
    }

    /**
     * Formate un pourcentage
     */
    public static String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    /**
     * Pluralise un mot selon le nombre
     */
    public static String pluralize(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }

    /**
     * Crée une ligne de séparation
     */
    public static String createSeparator(int length, char character) {
        return String.valueOf(character).repeat(length);
    }

    /**
     * Valide si un nom de joueur est valide
     */
    public static boolean isValidPlayerName(String name) {
        return name != null && name.length() >= 1 && name.length() <= 16 && name.matches("[a-zA-Z0-9_]+");
    }
}
