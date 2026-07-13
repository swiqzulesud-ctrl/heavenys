package dev.smplugin.build;

import org.bukkit.Material;

/**
 * The five rewards a builder-vote winner may choose from.
 */
public enum RewardType {

    HEARTS(Material.GOLDEN_APPLE, "+2 Max Hearts",
            "Permanently gain two extra hearts.",
            "Stacks across wins, up to a server cap."),

    BEACON(Material.BEACON, "A Beacon",
            "A full beacon, ready to crown",
            "your base with light and power."),

    END_CRYSTAL(Material.END_CRYSTAL, "An End Crystal",
            "A pristine end crystal —",
            "decoration or destruction, your call."),

    DRAGON_EGG(Material.DRAGON_EGG, "A Dragon Egg",
            "The rarest trophy in the game,",
            "without touching the End."),

    PLAYER_HEAD(Material.PLAYER_HEAD, "A Player Head",
            "The head of any player you name.",
            "You'll be asked to type the name in chat.");

    private final Material icon;
    private final String displayName;
    private final String[] description;

    RewardType(Material icon, String displayName, String... description) {
        this.icon = icon;
        this.displayName = displayName;
        this.description = description;
    }

    public Material icon() {
        return icon;
    }

    public String displayName() {
        return displayName;
    }

    public String[] description() {
        return description;
    }
}
