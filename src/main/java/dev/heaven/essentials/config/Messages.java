package dev.heaven.essentials.config;

import dev.heaven.essentials.HeavenEssentials;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads and formats every configurable MiniMessage string from messages.yml.
 *
 * <p>Missing keys fall back to the defaults bundled inside the jar, so a
 * partially edited messages.yml never breaks the plugin. The {@code <prefix>}
 * placeholder is available in every message and resolves to
 * {@code general.prefix}. A message set to an empty string is silently
 * skipped, allowing server owners to disable individual messages.</p>
 */
public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final HeavenEssentials plugin;
    private FileConfiguration config;
    private Component prefix;

    public Messages(HeavenEssentials plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Re-reads messages.yml from disk, creating it from the jar if absent. */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                this.config.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not read bundled messages.yml defaults", exception);
        }

        this.prefix = MINI_MESSAGE.deserialize(config.getString("general.prefix", ""));
    }

    /** Returns the raw MiniMessage string stored at the given key. */
    public String raw(String key) {
        return config.getString(key, "<red>Missing message: " + key + "</red>");
    }

    /** Returns whether a message key exists and is non-empty. */
    public boolean isPresent(String key) {
        String raw = config.getString(key, "");
        return raw != null && !raw.isEmpty();
    }

    /**
     * Deserializes the message at {@code key} with the standard prefix
     * resolver plus any additional placeholder resolvers.
     */
    public Component format(String key, TagResolver... resolvers) {
        TagResolver.Builder builder = TagResolver.builder()
                .resolver(Placeholder.component("prefix", prefix));
        for (TagResolver resolver : resolvers) {
            builder.resolver(resolver);
        }
        return MINI_MESSAGE.deserialize(raw(key), builder.build());
    }

    /**
     * Formats every line of the string list at {@code key}. Used for GUI lore.
     */
    public List<Component> formatList(String key, TagResolver... resolvers) {
        TagResolver.Builder builder = TagResolver.builder()
                .resolver(Placeholder.component("prefix", prefix));
        for (TagResolver resolver : resolvers) {
            builder.resolver(resolver);
        }
        TagResolver combined = builder.build();
        return config.getStringList(key).stream()
                .map(line -> (Component) MINI_MESSAGE.deserialize(line, combined))
                .toList();
    }

    /**
     * Sends the message at {@code key} to the audience, skipping empty
     * (disabled) messages entirely.
     */
    public void send(Audience audience, String key, TagResolver... resolvers) {
        if (!isPresent(key)) {
            return;
        }
        audience.sendMessage(format(key, resolvers));
    }
}
