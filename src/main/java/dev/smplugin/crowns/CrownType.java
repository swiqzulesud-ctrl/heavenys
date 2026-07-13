package dev.smplugin.crowns;

import org.bukkit.Material;

/**
 * The three permanent crowns of the SMP.
 */
public enum CrownType {

    KILLS("Kills", "Combat",
            "killer", "♛ Killer",
            Material.IRON_SWORD,
            "Earned by claiming the most",
            "player-vs-player kills on the server.",
            "Recalculated instantly on every duel."),

    RESOURCES("Resources", "Wealth",
            "magnate", "♛ Magnate",
            Material.GOLD_BLOCK,
            "Awarded by community vote on Discord",
            "based on submitted storage/base screenshots.",
            "Show off your vaults and farms to claim it."),

    BUILDER("the Builder", "Aesthetics",
            "builder", "♛ Builder",
            Material.SCAFFOLDING,
            "Won through the in-game builder vote",
            "held every few days. Submit your build",
            "with /build submit and let the server decide.");

    private final String displayName;
    private final String category;
    private final String teamName;
    private final String tabPrefix;
    private final Material icon;
    private final String[] description;

    CrownType(String displayName, String category, String teamName, String tabPrefix,
              Material icon, String... description) {
        this.displayName = displayName;
        this.category = category;
        this.teamName = teamName;
        this.tabPrefix = tabPrefix;
        this.icon = icon;
        this.description = description;
    }

    /** e.g. "Crown of Kills". */
    public String crownName() {
        return "Crown of " + displayName;
    }

    public String category() {
        return category;
    }

    /** Scoreboard team backing the tab prefix; one team per crown. */
    public String teamName() {
        return "smp_crown_" + teamName;
    }

    /** Tab-list prefix (rendered white/gold). */
    public String tabPrefix() {
        return tabPrefix;
    }

    public Material icon() {
        return icon;
    }

    public String[] description() {
        return description;
    }
}
