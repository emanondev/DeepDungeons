package emanondev.deepdungeons.parameter;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsString;
import emanondev.core.gui.GuiButton;
import emanondev.core.gui.ResearchFButton;
import emanondev.deepdungeons.generic.StandGui;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MythicMobTypeParameter extends Parameter<MythicMob> {

    public MythicMobTypeParameter() {
        super("mythicMobType", null);
    }

    @Override
    public String toString(MythicMob value) {
        return value.getInternalName();
    }

    @Override
    public MythicMob fromString(String value) {
        try {
            return MythicMobs.inst().getMobManager().getMythicMob(value);
        } catch (Exception e) {
            return this.defaultValue;
        }
    }

    @Override
    public GuiButton getEditorButton(StandGui gui) {
        return new ResearchFButton<>(gui,
                () -> new ItemBuilder(Material.ZOMBIE_HEAD).setDescription(
                        Arrays.asList("&6&lMob Type: &e%value%", "", "&7[&fClick&7] &9Any &7> &9Change type"),
                        "%value%", gui.getValue(this) == null ? "null" : gui.getValue(this).getInternalName()).build(),
                (s, v) -> v.getInternalName().toLowerCase().contains(s.toLowerCase())
                        || v.getDisplayName().toString().toLowerCase().contains(s.toLowerCase()),
                (event, v) -> {
                    gui.setValue(this, v);
                    gui.open(gui.getTargetPlayer());
                    return true;
                }, (
                v) -> new ItemBuilder(Material.STONE)
                .setDescription(Arrays.asList("&6" + v.getInternalName(),
                        "&9DisplayName: &f" + v.getDisplayName(), "&9Type: &e" + v.getEntityType(),
                        "&9Health: &e" + v.getHealth()
                                + (v.getPerLevelHealth() > 0.01
                                ? " &9(+&e" + UtilsString.formatOptional2Digit(
                                v.getPerLevelHealth()) + " &9per lv)"
                                : ""),
                        "&9Damage: &e" + v.getDamage()
                                + (v.getPerLevelDamage() > 0.01
                                ? " &9(+&e" + UtilsString.formatOptional2Digit(
                                v.getPerLevelDamage()) + " &9per lv)"
                                : ""),
                        "&9Armor: &e" + v.getArmor() + (v.getPerLevelArmor() > 0.01 ? " &9(+&e"
                                + UtilsString.formatOptional2Digit(v.getPerLevelArmor()) + " &9per lv)"
                                : "")))
                .build(),
                this::getEnabledEntity);
    }

    private Collection<MythicMob> getEnabledEntity() {
        ArrayList<MythicMob> mobs = new ArrayList<>(MythicMobs.inst().getMobManager().getMobTypes());
        mobs.sort((o1, o2) -> o1.getInternalName().compareToIgnoreCase(o2.getInternalName()));
        return mobs;
    }

}
