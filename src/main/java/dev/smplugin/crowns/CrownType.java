package dev.smplugin.crowns;

import org.bukkit.Material;

/**
 * Les trois couronnes permanentes du SMP.
 */
public enum CrownType {

    KILLS("du Tueur", "Combat",
            "killer", "♛ Tueur",
            Material.IRON_SWORD,
            "Gagnée en cumulant le plus de victoires",
            "joueur contre joueur sur le serveur.",
            "Recalculée instantanément à chaque duel."),

    RESOURCES("des Richesses", "Richesse",
            "magnate", "♛ Magnat",
            Material.GOLD_BLOCK,
            "Décernée par un vote communautaire sur Discord",
            "sur la base de captures d'écran de vos coffres et bases.",
            "Exhibez vos richesses pour la revendiquer."),

    BUILDER("du Bâtisseur", "Esthétique",
            "builder", "♛ Bâtisseur",
            Material.SCAFFOLDING,
            "Remportée grâce au vote de construction organisé",
            "régulièrement. Inscrivez votre création avec",
            "/build submit et laissez le serveur trancher.");

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

    /** ex. « Couronne du Tueur ». */
    public String crownName() {
        return "Couronne " + displayName;
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
