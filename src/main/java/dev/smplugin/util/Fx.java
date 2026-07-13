package dev.smplugin.util;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;

/**
 * Sound and particle helpers implementing the plugin's white/gold aesthetic:
 * END_ROD, SNOWFLAKE, CLOUD and WHITE_ASH particles, white fireworks and
 * gentle bell/level-up sounds.
 */
public final class Fx {

    private Fx() {
    }

    // ------------------------------------------------------------- sounds

    /** Success "ding" for confirmations (vote cast, reward claimed, ...). */
    public static void success(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.4f);
    }

    /** Soft click when navigating GUIs. */
    public static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    /** Gentle error tone for denied/invalid actions. */
    public static void deny(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
    }

    /** Grand fanfare for crown changes and event milestones (all players). */
    public static void fanfare(Player player) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
    }

    /** Deep, ominous rumble for relic countdown warnings. */
    public static void ominous(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.7f);
        player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.9f, 0.5f);
    }

    // ---------------------------------------------------------- particles

    /** White sparkle burst around a location (crown changes, rewards). */
    public static void whiteBurst(Location loc) {
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.05);
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 25, 0.6, 0.6, 0.6, 0.02);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.2, 0), 15, 0.4, 0.1, 0.4, 0.01);
    }

    /** Soft ambient white ash used as an aura tick. */
    public static void whiteAura(Location loc, double radius) {
        loc.getWorld().spawnParticle(Particle.WHITE_ASH, loc.clone().add(0, 1, 0), 12, radius, 1.0, radius, 0.0);
    }

    /** One ring of an END_ROD spiral, used for the Guardian's aura. */
    public static void spiralTick(Location center, double angle, double radius, double height) {
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        center.getWorld().spawnParticle(Particle.END_ROD,
                center.clone().add(x, height, z), 1, 0, 0, 0, 0);
    }

    /** Vertical END_ROD beam marking the relic drop, visible from afar. */
    public static void beamTick(Location base) {
        for (double y = 0; y <= 24; y += 0.8) {
            base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0, y, 0), 1, 0.05, 0, 0.05, 0);
        }
    }

    // ---------------------------------------------------------- fireworks

    /** Launches a white celebratory firework (builder-vote winner). */
    public static void whiteFirework(Location loc) {
        loc.getWorld().spawn(loc, Firework.class, fw -> {
            var meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.WHITE)
                    .withFade(Color.fromRGB(255, 215, 0))
                    .withTrail()
                    .withFlicker()
                    .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        });
    }
}
