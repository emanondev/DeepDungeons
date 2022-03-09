package emanondev.deepdungeons.parameter;

import emanondev.core.gui.GuiButton;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.generic.StandGui;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class SpreadParameter extends Parameter<Double> {

    public SpreadParameter() {
        super("spread", 0D);
    }

    @Override
    public String toString(Double value) {
        return value == null ? String.valueOf(this.defaultValue) : String.valueOf(Math.max(0, value));
    }

    @Override
    public Double fromString(String value) {
        try {
            return Math.max(0, Double.parseDouble(value));
        } catch (Exception e) {
            return this.defaultValue;
        }
    }

    public Double readValue(List<String> text) {
        Double i = super.readValue(text);
        return i == null ? defaultValue : i;
    }

    @Override
    public GuiButton getEditorButton(StandGui gui) {
        return new NumberEditorFButton<>(gui, 1D, 0.01D, 10D, () -> gui.getValue(SpreadParameter.this),
                (v) -> gui.setValue(SpreadParameter.this, Math.max(0, v)), () -> new ItemStack(Material.REPEATER),
                () -> DeepDungeons.get().getLanguageConfig(gui.getTargetPlayer())
                        .loadStringList("parameter.spread.info", Arrays.asList("&6&lSpread: &e%value%", "")),
                null);
    }

}
