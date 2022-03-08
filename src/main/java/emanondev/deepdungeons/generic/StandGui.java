package emanondev.deepdungeons.generic;

import java.util.Collections;
import java.util.List;

import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import emanondev.core.gui.PagedGui;
import emanondev.deepdungeons.parameter.Parameter;

public interface StandGui extends PagedGui {

	/**
	 * Returns a List containing all parameters needed for spawning the mob
	 * 
	 * @return a List containing all parameters needed for spawning the mob
	 */
	public abstract List<String> fillInfo();

	public default List<String> getInfo() {
		if (!getStand().isValid()) {
			this.getInventory().getViewers().forEach((V) -> V.closeInventory());
			return Collections.emptyList();
		}
		ItemStack item = getStand().getEquipment().getItem(EquipmentSlot.HAND);
		if (item == null || !item.hasItemMeta())
			return Collections.emptyList();
		return item.getItemMeta().getLore();
	}

	public <T> T getValue(Parameter<T> param);

	public <T extends Object> void setValue(Parameter<T> param);

	public <T extends Object> void setValue(Parameter<T> param, T value);

	public ArmorStand getStand();
}
