package emanondev.deepdungeons.parameter;

import emanondev.core.gui.GuiButton;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.generic.StandGui;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class ChanceParameter extends Parameter<Integer> {

    public ChanceParameter() {
        super("chance", 100);
    }

    @Override
    public String toString(Integer value) {
        return value == null ? "100" : String.valueOf(Math.min(100, Math.max(0, value)));
    }

    @Override
    public Integer fromString(String value) {
        try {
            return Math.min(100, Math.max(0, Integer.parseInt(value)));
        } catch (Exception e) {
            return 100;
        }
    }

    public Integer readValue(List<String> text) {
        Integer i = super.readValue(text);
        return i == null ? defaultValue : i;
    }

    @Override
    public GuiButton getEditorButton(StandGui gui) {
        return new NumberEditorFButton<>(gui, 10, 1, 100, () -> gui.getValue(ChanceParameter.this),
                (v) -> gui.setValue(ChanceParameter.this, Math.min(100, Math.max(0, v))),
                () -> new ItemStack(Material.REPEATER),
                () -> DeepDungeons.get().getLanguageConfig(gui.getTargetPlayer()).loadStringList(
                        "mobprovider.chance.info", Arrays.asList("&6&lSpawn Chance: &e%value% &9%", "")),
                null);
    }

}
