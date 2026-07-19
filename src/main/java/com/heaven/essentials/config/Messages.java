package com.heaven.essentials.config;

import com.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and renders every user-facing message from {@code messages.yml}.
 * All strings are parsed with MiniMessage, and a {@code <prefix>} tag is always
 * available. Missing keys degrade gracefully to a visible placeholder rather
 * than throwing, so a malformed config never crashes a command.
 */
public final class Messages {

    /** Legacy (§) serializer with hex support, for inventory titles and item text on Spigot. */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final HeavenEssentials plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final File file;

    private FileConfiguration config;
    private String prefix = "";

    public Messages(HeavenEssentials plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        // Merge in any keys missing from the user's file using the packaged defaults.
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
            config.options().copyDefaults(true);
        }
        this.prefix = config.getString("prefix", "");
    }

    /** Convenience factory for a safe, non-parsed placeholder (prevents MiniMessage injection). */
    public static TagResolver ph(String key, String value) {
        return Placeholder.unparsed(key, value == null ? "" : value);
    }

    /** Convenience factory for a parsed placeholder (value may contain MiniMessage tags). */
    public static TagResolver parsed(String key, String value) {
        return Placeholder.parsed(key, value == null ? "" : value);
    }

    /** Renders a single message path into a {@link Component}. */
    public Component render(String path, TagResolver... resolvers) {
        String raw = config.getString(path);
        if (raw == null) {
            return miniMessage.deserialize("<red>Missing message: " + path + "</red>");
        }
        return miniMessage.deserialize(raw, withPrefix(resolvers));
    }

    /** Renders a list message path (e.g. GUI lore) into components. */
    public List<Component> renderList(String path, TagResolver... resolvers) {
        List<String> raw = config.getStringList(path);
        List<Component> out = new ArrayList<>(raw.size());
        TagResolver combined = withPrefix(resolvers);
        for (String line : raw) {
            out.add(miniMessage.deserialize(line, combined));
        }
        return out;
    }

    /**
     * Sends a rendered message to the recipient. Empty messages are treated as
     * intentionally disabled and silently skipped.
     */
    public void send(CommandSender recipient, String path, TagResolver... resolvers) {
        String raw = config.getString(path);
        if (raw != null && raw.isEmpty()) {
            return;
        }
        plugin.audiences().sender(recipient).sendMessage(render(path, resolvers));
    }

    /** Renders a message path to a legacy (§) string, for APIs that only accept strings. */
    public String legacy(String path, TagResolver... resolvers) {
        return LEGACY.serialize(render(path, resolvers));
    }

    /** Renders a list path to legacy (§) strings (e.g. item lore on Spigot). */
    public List<String> renderListLegacy(String path, TagResolver... resolvers) {
        List<Component> components = renderList(path, resolvers);
        List<String> out = new ArrayList<>(components.size());
        for (Component component : components) {
            out.add(LEGACY.serialize(component));
        }
        return out;
    }

    /** Serialises a component to a legacy (§) string. */
    public String toLegacy(Component component) {
        return LEGACY.serialize(component);
    }

    public String rawPrefix() {
        return prefix;
    }

    private TagResolver withPrefix(TagResolver... resolvers) {
        TagResolver[] all = new TagResolver[resolvers.length + 1];
        all[0] = Placeholder.parsed("prefix", prefix);
        System.arraycopy(resolvers, 0, all, 1, resolvers.length);
        return TagResolver.resolver(all);
    }

    /** Persists an untouched copy of the default file if the user has none. */
    public void saveDefaultIfAbsent() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                plugin.saveResource("messages.yml", false);
            } catch (IllegalArgumentException ignored) {
                // Resource already handled elsewhere.
            }
        }
    }
}
