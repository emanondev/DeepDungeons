package emanondev.deepdungeons.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import emanondev.core.ItemBuilder;
import emanondev.core.gui.GuiButton;
import emanondev.core.gui.ResearchFButton;
import emanondev.deepdungeons.generic.StandGui;

public class EntityTypeParameter extends Parameter<EntityType> {

	public EntityTypeParameter() {
		super("entityType", EntityType.ZOMBIE);
	}

	@Override
	public String toString(EntityType value) {
		return value.name();
	}

	@Override
	public EntityType fromString(String value) {
		try {
			return EntityType.valueOf(value);
		} catch (Exception e) {
			return this.defaultValue;
		}
	}

	@Override
	public GuiButton getEditorButton(StandGui gui) {
		return new ResearchFButton<EntityType>(gui, () -> new ItemBuilder(Material.ZOMBIE_HEAD).setDescription(Arrays.asList(
				"&6&lMob Type: &e%value%",
				"",
				"&7[&fClick&7] &9Any &7> &9Change type"),"%value%",gui.getValue(this).name()
				).build(),
				(s, v) -> v.name().toLowerCase().contains(s.toLowerCase()), (event, v) -> {
					gui.setValue(this, v);
					gui.open(gui.getTargetPlayer());
					return true;
				}, (v) -> new ItemBuilder(getMaterial(v)).setDescription(Arrays.asList("&6" + v.name())).build(),
				() -> getEnabledEntity());
	}

	private Collection<EntityType> getEnabledEntity() {
		ArrayList<EntityType> set = new ArrayList<>();
		for (EntityType type : EntityType.values())
			if (type.isSpawnable() && type.isAlive())
				set.add(type);
		Collections.sort(set,(o1,o2)->o1.name().compareToIgnoreCase(o2.name()));
		return set;
	}
	
	private Material getMaterial(EntityType type) {
		try {
			return Material.valueOf(type.name()+"_SPAWN_EGG");
		}catch (Exception e) {
			return Material.STONE;
		}
	}

}
