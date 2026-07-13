package dev.smplugin;

import dev.smplugin.build.BuildVoteManager;
import dev.smplugin.build.RewardManager;
import dev.smplugin.command.BuildCommand;
import dev.smplugin.command.CrownsCommand;
import dev.smplugin.command.RelicCommand;
import dev.smplugin.command.SMPluginCommand;
import dev.smplugin.crowns.CrownListener;
import dev.smplugin.crowns.CrownManager;
import dev.smplugin.crowns.KillTracker;
import dev.smplugin.data.Database;
import dev.smplugin.gui.GuiListener;
import dev.smplugin.relic.RelicListener;
import dev.smplugin.relic.RelicManager;
import dev.smplugin.util.ChatInput;
import dev.smplugin.util.Keys;
import dev.smplugin.util.Text;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

/**
 * SMPlugin — a white/gold prestige layer for survival SMPs:
 * the Three Crowns, the recurring Builder Vote and the Sovereign's Relic.
 */
public final class SMPlugin extends JavaPlugin {

    private Database database;
    private ChatInput chatInput;
    private CrownManager crowns;
    private KillTracker kills;
    private RewardManager rewards;
    private BuildVoteManager buildVote;
    private RelicManager relic;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);
        Text.init(this);

        database = new Database(this);
        try {
            database.init(getConfig().getString("storage.file", "smplugin.db"));
        } catch (SQLException e) {
            getLogger().severe("Could not open the SQLite database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        chatInput = new ChatInput(this);
        crowns = new CrownManager(this, database);
        kills = new KillTracker(this, database, crowns);
        rewards = new RewardManager(this, database);
        buildVote = new BuildVoteManager(this, database, rewards);
        relic = new RelicManager(this, database);

        crowns.load();
        kills.load();
        rewards.load();
        buildVote.load();
        relic.load();

        var pm = getServer().getPluginManager();
        pm.registerEvents(chatInput, this);
        pm.registerEvents(kills, this);
        pm.registerEvents(new CrownListener(this, crowns), this);
        pm.registerEvents(rewards, this);
        pm.registerEvents(new RelicListener(this, relic), this);
        pm.registerEvents(new GuiListener(), this);

        register("crowns", new CrownsCommand(this));
        register("build", new BuildCommand(this));
        register("relic", new RelicCommand(this));
        register("smplugin", new SMPluginCommand(this));

        getLogger().info("SMPlugin enabled — the Crowns await their holders.");
    }

    @Override
    public void onDisable() {
        if (relic != null) {
            relic.unload();
        }
        if (crowns != null) {
            crowns.unload();
        }
        if (database != null) {
            database.shutdown();
        }
        Text.shutdown();
        getLogger().info("SMPlugin disabled.");
    }

    private <T extends CommandExecutor & TabCompleter> void register(String name, T handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command /" + name + " is missing from plugin.yml!");
            return;
        }
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    // ------------------------------------------------------------ accessors

    public ChatInput chatInput() {
        return chatInput;
    }

    public CrownManager crowns() {
        return crowns;
    }

    public KillTracker kills() {
        return kills;
    }

    public RewardManager rewards() {
        return rewards;
    }

    public BuildVoteManager buildVote() {
        return buildVote;
    }

    public RelicManager relic() {
        return relic;
    }
}
