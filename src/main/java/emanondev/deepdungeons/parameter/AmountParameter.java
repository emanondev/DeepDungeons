package emanondev.deepdungeons.parameter;

import emanondev.core.gui.GuiButton;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.deepdungeons.DeepDungeons;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class AmountParameter extends Parameter<Integer> {

    public AmountParameter() {
        super("amount", 1);
    }

    @Override
    public String toString(Integer value) {
        return value == null ? String.valueOf(this.defaultValue) : String.valueOf(Math.max(1, value));
    }

    @Override
    public Integer fromString(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (Exception e) {
            return 1;
        }
    }

    public Integer readValue(List<String> text) {
        Integer i = super.readValue(text);
        return i == null ? defaultValue : i;
    }

    @Override
    public GuiButton getEditorButton(StandGui gui) {
        return new NumberEditorFButton<>(gui, 1, 1, 10, () -> gui.getValue(AmountParameter.this),
                (v) -> gui.setValue(AmountParameter.this, Math.max(1, v)), () -> new ItemStack(Material.REPEATER),
                () -> DeepDungeons.get().getLanguageConfig(gui.getTargetPlayer())
                        .loadStringList("parameter.amount.info", Arrays.asList("&6&lAmount: &e%value%", "")),
                null);
    }

}
