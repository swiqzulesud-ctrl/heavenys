package com.heaven.essentials;

import com.heaven.essentials.command.HeavenCommand;
import com.heaven.essentials.command.commands.AnvilCommand;
import com.heaven.essentials.command.commands.BackCommand;
import com.heaven.essentials.command.commands.BottomCommand;
import com.heaven.essentials.command.commands.BroadcastCommand;
import com.heaven.essentials.command.commands.ClearCommand;
import com.heaven.essentials.command.commands.EnderchestCommand;
import com.heaven.essentials.command.commands.FeedCommand;
import com.heaven.essentials.command.commands.FlyCommand;
import com.heaven.essentials.command.commands.GamemodeCommand;
import com.heaven.essentials.command.commands.GodCommand;
import com.heaven.essentials.command.commands.HatCommand;
import com.heaven.essentials.command.commands.HealCommand;
import com.heaven.essentials.command.commands.HeartStealCommand;
import com.heaven.essentials.command.commands.IgnoreCommand;
import com.heaven.essentials.command.commands.InvseeCommand;
import com.heaven.essentials.command.commands.MessageCommand;
import com.heaven.essentials.command.commands.ReplyCommand;
import com.heaven.essentials.command.commands.RepairCommand;
import com.heaven.essentials.command.commands.SetSpawnCommand;
import com.heaven.essentials.command.commands.SpawnCommand;
import com.heaven.essentials.command.commands.SpeedCommand;
import com.heaven.essentials.command.commands.TeleportCommand;
import com.heaven.essentials.command.commands.TimeCommand;
import com.heaven.essentials.command.commands.TopCommand;
import com.heaven.essentials.command.commands.TpAllCommand;
import com.heaven.essentials.command.commands.TpHereCommand;
import com.heaven.essentials.command.commands.WeatherCommand;
import com.heaven.essentials.command.commands.WorkbenchCommand;
import com.heaven.essentials.config.ConfigManager;
import com.heaven.essentials.config.Messages;
import com.heaven.essentials.hearts.HeartManager;
import com.heaven.essentials.listeners.BackListener;
import com.heaven.essentials.listeners.ConnectionListener;
import com.heaven.essentials.listeners.GodListener;
import com.heaven.essentials.listeners.LifestealListener;
import com.heaven.essentials.listeners.MenuListener;
import com.heaven.essentials.managers.BackManager;
import com.heaven.essentials.managers.ChatService;
import com.heaven.essentials.managers.GodManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * HeavenEssentials - a Heaven-themed Lifesteal system combined with a
 * lightweight Essentials-style command suite for Paper 1.26.2.
 *
 * <p>This class owns the plugin lifecycle: it constructs the managers, wires up
 * listeners and commands, and guarantees player data is persisted on shutdown.</p>
 */
public final class HeavenEssentials extends JavaPlugin {

    private BukkitAudiences audiences;
    private ConfigManager configManager;
    private Messages messages;
    private HeartManager heartManager;
    private BackManager backManager;
    private GodManager godManager;
    private ChatService chatService;

    @Override
    public void onEnable() {
        // Adventure bridge (Spigot does not ship Adventure natively).
        this.audiences = BukkitAudiences.create(this);

        // Managers (order matters: config -> messages -> hearts).
        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.heartManager = new HeartManager(this);
        this.heartManager.load();
        this.backManager = new BackManager();
        this.godManager = new GodManager();
        this.chatService = new ChatService();

        registerListeners();
        registerCommands();

        // Apply stored hearts to anyone already online (e.g. after a /reload).
        for (Player player : getServer().getOnlinePlayers()) {
            heartManager.initialise(player);
        }

        getLogger().info("HeavenEssentials has ascended. Lifesteal + Essentials are ready.");
    }

    @Override
    public void onDisable() {
        if (heartManager != null) {
            heartManager.saveSync();
        }
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
        getLogger().info("HeavenEssentials has been disabled. Player hearts saved.");
    }

    /** Reloads config + messages and re-applies hearts (clamping if the max dropped). */
    public void reloadAll() {
        configManager.reload();
        messages.reload();
        heartManager.clampAllToMax();
        for (Player player : getServer().getOnlinePlayers()) {
            heartManager.apply(player);
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new LifestealListener(this), this);
        pm.registerEvents(new ConnectionListener(this), this);
        pm.registerEvents(new BackListener(this), this);
        pm.registerEvents(new GodListener(this), this);
        pm.registerEvents(new MenuListener(this), this);
    }

    private void registerCommands() {
        bind("heartsteal", new HeartStealCommand(this));
        bind("gm", new GamemodeCommand(this));
        bind("fly", new FlyCommand(this));
        bind("heal", new HealCommand(this));
        bind("feed", new FeedCommand(this));
        bind("god", new GodCommand(this));
        bind("speed", new SpeedCommand(this));
        bind("tp", new TeleportCommand(this));
        bind("tphere", new TpHereCommand(this));
        bind("tpall", new TpAllCommand(this));
        bind("spawn", new SpawnCommand(this));
        bind("setspawn", new SetSpawnCommand(this));
        bind("back", new BackCommand(this));
        bind("invsee", new InvseeCommand(this));
        bind("enderchest", new EnderchestCommand(this));
        bind("clear", new ClearCommand(this));
        bind("repair", new RepairCommand(this));
        bind("workbench", new WorkbenchCommand(this));
        bind("anvil", new AnvilCommand(this));
        bind("hat", new HatCommand(this));
        bind("top", new TopCommand(this));
        bind("bottom", new BottomCommand(this));

        TimeCommand time = new TimeCommand(this);
        bind("day", time);
        bind("night", time);

        WeatherCommand weather = new WeatherCommand(this);
        bind("sun", weather);
        bind("rain", weather);

        bind("broadcast", new BroadcastCommand(this));
        bind("msg", new MessageCommand(this));
        bind("reply", new ReplyCommand(this));
        bind("ignore", new IgnoreCommand(this));
    }

    private void bind(String name, HeavenCommand command) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml and was not registered.");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    // ---------------------------------------------------------------------
    //  Manager accessors
    // ---------------------------------------------------------------------

    public BukkitAudiences audiences() {
        return audiences;
    }

    public ConfigManager configs() {
        return configManager;
    }

    public Messages messages() {
        return messages;
    }

    public HeartManager hearts() {
        return heartManager;
    }

    public BackManager back() {
        return backManager;
    }

    public GodManager god() {
        return godManager;
    }

    public ChatService chat() {
        return chatService;
    }
}
