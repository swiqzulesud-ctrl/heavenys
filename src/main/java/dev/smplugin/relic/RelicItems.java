package dev.smplugin.relic;

import dev.smplugin.util.Keys;
import dev.smplugin.util.Text;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Construit et reconnaît les objets-reliques uniques (un par boss).
 *
 * <p>Équilibrage volontairement contenu : aucun enchantement ne dépasse de
 * plus de <b>2 niveaux</b> le maximum vanilla (ex. Tranchant VII, Solidité V,
 * Épines V, Puissance VII).</p>
 */
public final class RelicItems {

    private RelicItems() {
    }

    /** Crée la relique unique du boss donné. */
    public static ItemStack create(RelicBoss boss) {
        ItemStack stack = new ItemStack(boss.relicMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        meta.setDisplayName(Text.legacy("<!italic><gold><bold>" + boss.relicName() + "</bold></gold>"));
        switch (boss) {
            case GARDIEN -> {
                // Vanilla max : Tranchant V, Butin III, Solidité III.
                meta.addEnchant(Enchantment.SHARPNESS, 7, true);
                meta.addEnchant(Enchantment.LOOTING, 4, true);
                meta.addEnchant(Enchantment.UNBREAKING, 5, true);
                meta.setLore(Text.legacyLore(
                        "<white>Forgée dans la chute du Gardien Souverain.</white>",
                        "<gray>Il ne peut en exister qu'une seule.</gray>",
                        "",
                        "<gold>✦</gold> <white>Tranchant VII</white>",
                        "<gold>✦</gold> <white>Butin IV</white>",
                        "<gold>✦</gold> <white>Solidité V</white>",
                        "",
                        "<gray><italic>La couronne obéit à qui tient la hache.</italic></gray>"));
            }
            case COLOSSE -> {
                // Vanilla max : Protection IV, Épines III, Solidité III.
                meta.addEnchant(Enchantment.PROTECTION, 6, true);
                meta.addEnchant(Enchantment.THORNS, 5, true);
                meta.addEnchant(Enchantment.UNBREAKING, 5, true);
                meta.setLore(Text.legacyLore(
                        "<white>Arraché à la carcasse du Colosse des Abysses.</white>",
                        "<gray>Il ne peut en exister qu'un seul.</gray>",
                        "",
                        "<gold>✦</gold> <white>Protection VI</white>",
                        "<gold>✦</gold> <white>Épines V</white>",
                        "<gold>✦</gold> <white>Solidité V</white>",
                        "",
                        "<gray><italic>Les abysses rendent coup pour coup.</italic></gray>"));
            }
            case HERAUT -> {
                // Vanilla max : Puissance V, Frappe II, Solidité III.
                meta.addEnchant(Enchantment.POWER, 7, true);
                meta.addEnchant(Enchantment.PUNCH, 4, true);
                meta.addEnchant(Enchantment.FLAME, 1, true);
                meta.addEnchant(Enchantment.UNBREAKING, 5, true);
                meta.setLore(Text.legacyLore(
                        "<white>Tendu avec les derniers murmures du Héraut Voilé.</white>",
                        "<gray>Il ne peut en exister qu'un seul.</gray>",
                        "",
                        "<gold>✦</gold> <white>Puissance VII</white>",
                        "<gold>✦</gold> <white>Frappe IV</white>",
                        "<gold>✦</gold> <white>Flamme I</white>",
                        "<gold>✦</gold> <white>Solidité V</white>",
                        "",
                        "<gray><italic>Chaque flèche porte une éclipse.</italic></gray>"));
            }
        }
        meta.getPersistentDataContainer().set(Keys.RELIC_ITEM, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(Keys.RELIC_ID, PersistentDataType.STRING, boss.id());
        stack.setItemMeta(meta);
        return stack;
    }

    /** True when the stack carries a relic PDC tag (survives renames/anvils). */
    public static boolean isRelic(ItemStack stack) {
        return relicBoss(stack) != null;
    }

    /**
     * Resolves which boss's relic this stack is, or null if it isn't one.
     * Legacy stacks (before multiple bosses existed) count as the Gardien's axe.
     */
    public static RelicBoss relicBoss(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        var pdc = stack.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(Keys.RELIC_ID, PersistentDataType.STRING);
        if (id != null) {
            return RelicBoss.byId(id);
        }
        if (pdc.has(Keys.RELIC_ITEM, PersistentDataType.BYTE)) {
            return RelicBoss.GARDIEN;
        }
        return null;
    }
}
