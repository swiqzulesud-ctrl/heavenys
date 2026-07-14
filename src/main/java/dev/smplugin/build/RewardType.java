package dev.smplugin.build;

import org.bukkit.Material;

/**
 * Les cinq récompenses parmi lesquelles le gagnant du vote peut choisir.
 */
public enum RewardType {

    HEARTS(Material.GOLDEN_APPLE, "+2 Cœurs Max",
            "Gagnez définitivement deux cœurs supplémentaires.",
            "Cumulable à chaque victoire, dans la limite du serveur."),

    BEACON(Material.BEACON, "Une Balise",
            "Une balise complète, prête à couronner",
            "votre base de lumière et de puissance."),

    END_CRYSTAL(Material.END_CRYSTAL, "Un Cristal de l'End",
            "Un cristal de l'End immaculé —",
            "décoration ou destruction, à vous de voir."),

    DRAGON_EGG(Material.DRAGON_EGG, "Un Œuf de Dragon",
            "Le trophée le plus rare du jeu,",
            "sans mettre un pied dans l'End."),

    PLAYER_HEAD(Material.PLAYER_HEAD, "Une Tête de Joueur",
            "La tête de n'importe quel joueur.",
            "Son nom vous sera demandé dans le chat.");

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
