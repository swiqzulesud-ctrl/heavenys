package dev.smplugin.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Central MiniMessage helpers enforcing the plugin's white/gold visual identity.
 * All user-facing text flows through this class so the theme stays consistent.
 *
 * <p>Arclight/Spigot does not bundle Adventure the way Paper does, so the
 * library is shaded and bridged through {@link BukkitAudiences}. Legacy-string
 * conversions are provided for the Bukkit APIs that predate components
 * (item names/lore, inventory titles, scoreboard teams, entity names).</p>
 */
public final class Text {

    public static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** Chat prefix used in front of every plugin message. */
    private static final String PREFIX = "<gold>♛</gold> <white>";

    private static BukkitAudiences audiences;

    private Text() {
    }

    /** Creates the Bukkit audience bridge. Called once, on enable. */
    public static void init(Plugin plugin) {
        audiences = BukkitAudiences.create(plugin);
    }

    /** Tears the audience bridge down. Called on disable. */
    public static void shutdown() {
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    /** Adventure audience for any Bukkit sender (player, console, ...). */
    public static Audience of(CommandSender sender) {
        return audiences.sender(sender);
    }

    /** Deserializes a MiniMessage string into a component. */
    public static Component mm(String mini) {
        return MM.deserialize(mini);
    }

    /** Converts MiniMessage to a legacy §-string for pre-component Bukkit APIs. */
    public static String legacy(String mini) {
        return LEGACY.serialize(MM.deserialize(mini));
    }

    /** Sends a themed chat message (prefixed, white base colour). */
    public static void msg(CommandSender sender, String mini) {
        of(sender).sendMessage(MM.deserialize(PREFIX + mini));
    }

    /** Sends an unprefixed themed line (leaderboards, help rows). */
    public static void raw(CommandSender sender, String mini) {
        of(sender).sendMessage(MM.deserialize(mini));
    }

    /** Broadcasts a themed chat message to all players and the console. */
    public static void broadcast(String mini) {
        audiences.all().sendMessage(MM.deserialize(PREFIX + mini));
    }

    /** Shows a title + subtitle with sensible fade timings. */
    public static void title(CommandSender sender, String titleMini, String subtitleMini) {
        of(sender).showTitle(times(titleMini, subtitleMini));
    }

    /** Shows a title + subtitle to every online player. */
    public static void broadcastTitle(String titleMini, String subtitleMini) {
        audiences.players().showTitle(times(titleMini, subtitleMini));
    }

    private static Title times(String titleMini, String subtitleMini) {
        return Title.title(
                MM.deserialize(titleMini),
                MM.deserialize(subtitleMini),
                Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3200), Duration.ofMillis(800)));
    }

    /** Sends an action bar line. */
    public static void actionBar(CommandSender sender, String mini) {
        of(sender).sendActionBar(MM.deserialize(mini));
    }

    /** Converts a list of MiniMessage lines to legacy lore strings. */
    public static List<String> legacyLore(String... lines) {
        List<String> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(legacy("<white>" + line));
        }
        return out;
    }

    /** Formats a duration in seconds as a compact human string, e.g. "1d 3h 20m". */
    public static String duration(long seconds) {
        if (seconds <= 0) {
            return "moments";
        }
        long days = seconds / 86_400;
        long hours = (seconds % 86_400) / 3_600;
        long minutes = (seconds % 3_600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (sb.isEmpty()) sb.append(secs).append("s");
        return sb.toString().trim();
    }
}
