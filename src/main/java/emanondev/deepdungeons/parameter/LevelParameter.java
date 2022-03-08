package emanondev.deepdungeons.parameter;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import emanondev.core.gui.GuiButton;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.generic.StandGui;

public class LevelParameter extends Parameter<Integer> {

	public LevelParameter() {
		super("level", 0);
	}

	@Override
	public String toString(Integer value) {
		return value == null ? String.valueOf(this.defaultValue) : String.valueOf(Math.max(0, value));
	}

	@Override
	public Integer fromString(String value) {
		try {
			return Math.max(0, Integer.valueOf(value));
		} catch (Exception e) {
			return this.defaultValue;
		}
	}

	public Integer readValue(List<String> text) {
		Integer i = super.readValue(text);
		return i == null ? defaultValue : i;
	}

	@Override
	public GuiButton getEditorButton(StandGui gui) {
		return new NumberEditorFButton<Integer>(gui, 1, 1, 10, () -> gui.getValue(LevelParameter.this),
				(v) -> gui.setValue(LevelParameter.this, v), () -> new ItemStack(Material.IRON_SWORD),
				() -> DeepDungeons.get().getLanguageConfig(gui.getTargetPlayer())
						.loadStringList("parameter.level.info", Arrays.asList("&6&lLevel: &e%value%", "")),
				null);
	}

}
