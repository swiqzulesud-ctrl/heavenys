package dev.smplugin.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Central MiniMessage helpers enforcing the plugin's white/gold visual identity.
 * All user-facing text flows through this class so the theme stays consistent.
 */
public final class Text {

    public static final MiniMessage MM = MiniMessage.miniMessage();

    /** Chat prefix used in front of every plugin message. */
    private static final String PREFIX = "<gold>♛</gold> <white>";

    private Text() {
    }

    /** Deserializes a MiniMessage string into a component. */
    public static Component mm(String mini) {
        return MM.deserialize(mini);
    }

    /** Sends a themed chat message (prefixed, white base colour). */
    public static void msg(Audience audience, String mini) {
        audience.sendMessage(MM.deserialize(PREFIX + mini));
    }

    /** Broadcasts a themed chat message to the whole server. */
    public static void broadcast(String mini) {
        Bukkit.getServer().sendMessage(MM.deserialize(PREFIX + mini));
    }

    /** Shows a title + subtitle with sensible fade timings. */
    public static void title(Audience audience, String titleMini, String subtitleMini) {
        audience.showTitle(Title.title(
                MM.deserialize(titleMini),
                MM.deserialize(subtitleMini),
                Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(3200), Duration.ofMillis(800))));
    }

    /** Shows a title + subtitle to every online player. */
    public static void broadcastTitle(String titleMini, String subtitleMini) {
        title(Bukkit.getServer(), titleMini, subtitleMini);
    }

    /** Sends an action bar line. */
    public static void actionBar(Audience audience, String mini) {
        audience.sendActionBar(MM.deserialize(mini));
    }

    /** Converts a component back to plain text (used for chat-input parsing). */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** Deserializes a list of MiniMessage lore lines, stripping the default italics. */
    public static List<Component> lore(String... lines) {
        List<Component> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(MM.deserialize("<!italic>" + line));
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
