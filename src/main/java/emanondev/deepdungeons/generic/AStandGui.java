package emanondev.deepdungeons.generic;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsString;
import emanondev.core.gui.FButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.parameter.Parameter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class AStandGui extends PagedMapGui implements StandGui {
    private final String managerName;
    private final ArmorStand stand;
    private final Provider provider;
    @SuppressWarnings("rawtypes")
    private final HashMap<Parameter, Object> params = new HashMap<>();

    public AStandGui(@NotNull String managerName, @NotNull Player player, @NotNull ArmorStand stand, @NotNull Provider provider) {
        super("&9" + managerName + "Provider " + provider.getId(), 6, player, null, DeepDungeons.get());
        if (!UtilsString.isValidID(managerName))
            throw new IllegalStateException();
        this.stand = stand;
        this.provider = provider;
        this.managerName = managerName;

        this.setButton(44,
                new FButton(this, () -> new ItemBuilder(Material.BARRIER)
                        .setDescription(this.getLanguageSection(getTargetPlayer()).loadStringList(
                                "provider.delete.info", List.of("&6&lClick to delete this provider")))// TODO
                        // better
                        // text
                        .build(), (event) -> {
                    stand.remove();
                    new ArrayList<>(this.getInventory().getViewers()).forEach(HumanEntity::closeInventory);
                    return false;
                }));
        registerParams();
        ItemStack item = stand.getEquipment().getItemInMainHand();
        loadValues(item.hasItemMeta() ? item.getItemMeta().getLore() : Collections.emptyList());
    }

    public abstract void registerParams();

    public Provider getProvider() {
        return provider;
    }

    public <T> void registerParam(Parameter<T> param, int slot) {
        this.setValue(param);
        this.setButton(slot, param.getEditorButton(this));
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(Parameter<T> param) {
        return (T) params.get(param);
    }

    public <T> void setValue(Parameter<T> param) {
        this.setValue(param, param.defaultValue);
    }

    public <T> void setValue(Parameter<T> param, T value) {
        params.put(param, value);
    }

    @SuppressWarnings("unchecked")
    public void loadValues(List<String> info) {
        params.keySet().forEach((k) -> params.put(k, k.readValue(info)));
    }

    public ArmorStand getStand() {
        return stand;
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (!stand.isValid())
            return;
        stand.setSmall(true);
        stand.getEquipment().setItem(EquipmentSlot.HAND,
                new ItemBuilder(Material.PAPER).setDisplayName(managerName + " " + provider.getId()).setLore(fillInfo()).build(),
                true);
        stand.setCustomName(ChatColor.GRAY + managerName);
        stand.setCustomNameVisible(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> fillInfo() {
        List<String> info = new ArrayList<>();
        params.forEach((k, v) -> k.addValue(info, v));
        return info;
    }
}
