package emanondev.deepdungeons.generic;

import emanondev.core.gui.PagedGui;
import emanondev.deepdungeons.parameter.Parameter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public interface StandGui extends PagedGui {

    /**
     * Returns a List containing all parameters needed for spawning the mob
     *
     * @return a List containing all parameters needed for spawning the mob
     */
    List<String> fillInfo();

    default List<String> getInfo() {
        if (!getStand().isValid()) {
            this.getInventory().getViewers().forEach(HumanEntity::closeInventory);
            return Collections.emptyList();
        }
        ItemStack item = getStand().getEquipment().getItem(EquipmentSlot.HAND);
        if (!item.hasItemMeta())
            return Collections.emptyList();
        return item.getItemMeta().getLore();
    }

    <T> T getValue(Parameter<T> param);

    <T> void setValue(Parameter<T> param);

    <T> void setValue(Parameter<T> param, T value);

    ArmorStand getStand();
}
