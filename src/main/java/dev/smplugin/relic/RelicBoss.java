package dev.smplugin.relic;

import org.bukkit.Material;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Stray;
import org.bukkit.entity.WitherSkeleton;

/**
 * Les différents boss de l'événement de la Relique Souveraine. Chaque boss a
 * sa propre relique unique ; il ne peut exister qu'un seul exemplaire de
 * chaque relique à la fois.
 *
 * <p>Les stats sont exprimées en multiplicateurs des valeurs de base de
 * {@code config.yml} (relic.boss.*), pour que l'équilibrage global reste
 * réglable au même endroit.</p>
 */
public enum RelicBoss {

    GARDIEN("gardien", "Gardien Souverain",
            WitherSkeleton.class, 1.0, 1.0, 0.33,
            Skeleton.class, "Écho Souverain",
            "Hache Fend-Couronne", Material.NETHERITE_AXE,
            "Un <gold>Gardien Souverain</gold>"),

    COLOSSE("colosse", "Colosse des Abysses",
            Ravager.class, 1.4, 1.2, 0.30,
            Drowned.class, "Noyé des Abysses",
            "Plastron du Colosse", Material.NETHERITE_CHESTPLATE,
            "Un <gold>Colosse des Abysses</gold>"),

    HERAUT("heraut", "Héraut Voilé",
            Evoker.class, 0.75, 1.0, 0.35,
            Stray.class, "Ombre Voilée",
            "Arc de l'Éclipse", Material.BOW,
            "Un <gold>Héraut Voilé</gold>");

    private final String id;
    private final String bossName;
    private final Class<? extends Mob> entityClass;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double movementSpeed;
    private final Class<? extends Mob> addClass;
    private final String addName;
    private final String relicName;
    private final Material relicMaterial;
    private final String announceArticle;

    RelicBoss(String id, String bossName, Class<? extends Mob> entityClass,
              double healthMultiplier, double damageMultiplier, double movementSpeed,
              Class<? extends Mob> addClass, String addName,
              String relicName, Material relicMaterial, String announceArticle) {
        this.id = id;
        this.bossName = bossName;
        this.entityClass = entityClass;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.movementSpeed = movementSpeed;
        this.addClass = addClass;
        this.addName = addName;
        this.relicName = relicName;
        this.relicMaterial = relicMaterial;
        this.announceArticle = announceArticle;
    }

    public String id() {
        return id;
    }

    public String bossName() {
        return bossName;
    }

    public Class<? extends Mob> entityClass() {
        return entityClass;
    }

    public double healthMultiplier() {
        return healthMultiplier;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public Class<? extends Mob> addClass() {
        return addClass;
    }

    public String addName() {
        return addName;
    }

    public String relicName() {
        return relicName;
    }

    public Material relicMaterial() {
        return relicMaterial;
    }

    /** ex. « Un <gold>Colosse des Abysses</gold> » pour les annonces. */
    public String announceArticle() {
        return announceArticle;
    }

    /** Meta key of the one-copy flag in the database. */
    public String existsKey() {
        return "relic_exists_" + id;
    }

    public static RelicBoss byId(String id) {
        for (RelicBoss boss : values()) {
            if (boss.id.equalsIgnoreCase(id)) {
                return boss;
            }
        }
        return null;
    }
}
