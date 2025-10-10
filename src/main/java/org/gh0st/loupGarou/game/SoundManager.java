package org.gh0st.loupGarou.game;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.gh0st.loupGarou.role.PlayerRole;

public class SoundManager {

    private static void playToAll(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public static void playGameStart() {
        playToAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.2f);
    }

    public static void playNightStart() {
        playToAll(Sound.ENTITY_WOLF_GROWL, 1.0f, 0.8f);
    }

    public static void playDayStart() {
        playToAll(Sound.ENTITY_CHICKEN_HURT, 1.0f, 1.0f);
    }

    public static void playVoteStart() {
        playToAll(Sound.BLOCK_ANVIL_LAND, 0.7f, 1.2f);
    }

    public static void playElimination() {
        playToAll(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }

    public static void playVictory() {
        playToAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public static void playRoleReveal(Player player, PlayerRole role) {
        Sound sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

        switch (role) {
            case WEREWOLF:
                sound = Sound.ENTITY_WOLF_GROWL;
                break;
            case SEER:
                sound = Sound.BLOCK_ENCHANTMENT_TABLE_USE;
                break;
            case GUARD:
                sound = Sound.ITEM_SHIELD_BLOCK;
                break;
            case WITCH:
                sound = Sound.BLOCK_BREWING_STAND_BREW;
                break;
        }

        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }

    public static void playError(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    public static void playSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.8f, 1.0f);
    }

    public static void playVoteSubmitted(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    public static void playWarning() {
        playToAll(Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.8f);
    }
}