package emanondev.deepdungeons.parameter;

import emanondev.core.ItemBuilder;
import emanondev.core.gui.GuiButton;
import emanondev.core.gui.ResearchFButton;
import emanondev.deepdungeons.generic.StandGui;
import org.bukkit.Material;
import pro.dracarys.DracarysGUI.api.DracarysGUIAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DracarysGuiLootGroupParameter extends Parameter<String> {

    public DracarysGuiLootGroupParameter() {
        super("DrcGuiLootGroup", null);
    }

    @Override
    public String toString(String value) {
        return value;
    }

    @Override
    public String fromString(String value) {
        return value;
    }

    @Override
    public GuiButton getEditorButton(StandGui gui) {
        return new ResearchFButton<>(gui, () -> new ItemBuilder(Material.GOLD_INGOT).setDescription(Arrays.asList(
                "&6&lLootGroup: &e%value%",
                "",
                "&7[&fClick&7] &9Any &7> &9Change type"), "%value%", "" + gui.getValue(this)
        ).build(),
                (s, v) -> v.toLowerCase().contains(s), (event, v) -> {
            gui.setValue(this, v);
            return true;
        }, (v) -> new ItemBuilder(Material.GOLD_INGOT).setDescription(List.of("&6" + v)).build(), () -> {
            ArrayList<String> tab = new ArrayList<>(DracarysGUIAPI.getLootGroups());
            Collections.sort(tab);
            return tab;
        });
    }

}
