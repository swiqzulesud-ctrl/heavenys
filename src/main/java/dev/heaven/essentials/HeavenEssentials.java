package dev.heaven.essentials;

import dev.heaven.essentials.command.AbstractCommand;
import dev.heaven.essentials.command.AnvilCommand;
import dev.heaven.essentials.command.BackCommand;
import dev.heaven.essentials.command.BottomCommand;
import dev.heaven.essentials.command.BroadcastCommand;
import dev.heaven.essentials.command.ClearCommand;
import dev.heaven.essentials.command.EnderchestCommand;
import dev.heaven.essentials.command.FeedCommand;
import dev.heaven.essentials.command.FlyCommand;
import dev.heaven.essentials.command.GamemodeCommand;
import dev.heaven.essentials.command.GodCommand;
import dev.heaven.essentials.command.HatCommand;
import dev.heaven.essentials.command.HealCommand;
import dev.heaven.essentials.command.HeartstealCommand;
import dev.heaven.essentials.command.IgnoreCommand;
import dev.heaven.essentials.command.InvseeCommand;
import dev.heaven.essentials.command.MsgCommand;
import dev.heaven.essentials.command.ReplyCommand;
import dev.heaven.essentials.command.RepairCommand;
import dev.heaven.essentials.command.SetspawnCommand;
import dev.heaven.essentials.command.SpawnCommand;
import dev.heaven.essentials.command.SpeedCommand;
import dev.heaven.essentials.command.TimeCommand;
import dev.heaven.essentials.command.TopCommand;
import dev.heaven.essentials.command.TpCommand;
import dev.heaven.essentials.command.TpallCommand;
import dev.heaven.essentials.command.TphereCommand;
import dev.heaven.essentials.command.WeatherCommand;
import dev.heaven.essentials.command.WorkbenchCommand;
import dev.heaven.essentials.config.Messages;
import dev.heaven.essentials.data.DataStore;
import dev.heaven.essentials.lifesteal.HeartManager;
import dev.heaven.essentials.lifesteal.LifestealListener;
import dev.heaven.essentials.lifesteal.gui.HeartsGui;
import dev.heaven.essentials.lifesteal.gui.HeartsGuiListener;
import dev.heaven.essentials.listener.BackListener;
import dev.heaven.essentials.listener.GodListener;
import dev.heaven.essentials.listener.PlayerConnectionListener;
import dev.heaven.essentials.service.BackService;
import dev.heaven.essentials.service.GodService;
import dev.heaven.essentials.service.PrivateMessageService;
import dev.heaven.essentials.service.SpawnService;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * HeavenEssentials - a Heaven-themed Lifesteal system combined with a
 * lightweight Essentials-style command suite for Paper.
 *
 * <p>This class wires together every manager, service, listener and command.
 * All state lives in the dedicated components; the main class only performs
 * lifecycle management (enable, disable, reload).</p>
 */
public final class HeavenEssentials extends JavaPlugin {

    private Messages messages;
    private DataStore dataStore;
    private HeartManager heartManager;
    private SpawnService spawnService;
    private BackService backService;
    private GodService godService;
    private PrivateMessageService privateMessageService;
    private HeartsGui heartsGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.messages = new Messages(this);
        this.dataStore = new DataStore(this);
        this.heartManager = new HeartManager(this, dataStore);
        this.spawnService = new SpawnService(dataStore);
        this.backService = new BackService();
        this.godService = new GodService();
        this.privateMessageService = new PrivateMessageService(dataStore);
        this.heartsGui = new HeartsGui(this);

        registerListeners();
        registerCommands();

        // Apply stored hearts to anyone already online (plugin managers / reloads).
        for (Player player : getServer().getOnlinePlayers()) {
            heartManager.initializePlayer(player);
        }

        getLogger().info("HeavenEssentials enabled - the heavens are watching.");
    }

    @Override
    public void onDisable() {
        dataStore.save();
        getLogger().info("HeavenEssentials disabled - player data saved.");
    }

    /**
     * Reloads config.yml, messages.yml and data.yml from disk and re-applies
     * heart values to every online player.
     */
    public void reloadPlugin() {
        reloadConfig();
        messages.reload();
        dataStore.reload();
        for (Player player : getServer().getOnlinePlayers()) {
            heartManager.applyHearts(player);
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new LifestealListener(this), this);
        pm.registerEvents(new HeartsGuiListener(this), this);
        pm.registerEvents(new GodListener(godService), this);
        pm.registerEvents(new BackListener(backService), this);
    }

    private void registerCommands() {
        register("heartsteal", new HeartstealCommand(this));
        register("gm", new GamemodeCommand(this));
        register("fly", new FlyCommand(this));
        register("heal", new HealCommand(this));
        register("feed", new FeedCommand(this));
        register("god", new GodCommand(this));
        register("speed", new SpeedCommand(this));
        register("tp", new TpCommand(this));
        register("tphere", new TphereCommand(this));
        register("tpall", new TpallCommand(this));
        register("spawn", new SpawnCommand(this));
        register("setspawn", new SetspawnCommand(this));
        register("back", new BackCommand(this));
        register("invsee", new InvseeCommand(this));
        register("enderchest", new EnderchestCommand(this));
        register("clear", new ClearCommand(this));
        register("repair", new RepairCommand(this));
        register("workbench", new WorkbenchCommand(this));
        register("anvil", new AnvilCommand(this));
        register("hat", new HatCommand(this));
        register("top", new TopCommand(this));
        register("bottom", new BottomCommand(this));
        register("day", new TimeCommand(this, 1000L, "commands.time.day", "heaven.command.day"));
        register("night", new TimeCommand(this, 13000L, "commands.time.night", "heaven.command.night"));
        register("sun", new WeatherCommand(this, false, "commands.weather.sun", "heaven.command.sun"));
        register("rain", new WeatherCommand(this, true, "commands.weather.rain", "heaven.command.rain"));
        register("broadcast", new BroadcastCommand(this));
        register("msg", new MsgCommand(this));
        register("reply", new ReplyCommand(this));
        register("ignore", new IgnoreCommand(this));
    }

    private void register(String name, AbstractCommand executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name),
                "Command '" + name + "' is missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public Messages messages() {
        return messages;
    }

    public DataStore dataStore() {
        return dataStore;
    }

    public HeartManager heartManager() {
        return heartManager;
    }

    public SpawnService spawnService() {
        return spawnService;
    }

    public BackService backService() {
        return backService;
    }

    public GodService godService() {
        return godService;
    }

    public PrivateMessageService privateMessageService() {
        return privateMessageService;
    }

    public HeartsGui heartsGui() {
        return heartsGui;
    }
}
