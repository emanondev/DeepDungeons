package emanondev.deepdungeons.trap;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrapTypeManager extends DRegistry<TrapType> {

    public static final String LINE_ONE = "TRAP BLUEPRINT";
    private static final TrapTypeManager instance = new TrapTypeManager();

    public TrapTypeManager() {
        super(DeepDungeons.get(), "TreasureManager", true);
    }

    public static TrapTypeManager getInstance() {
        return instance;
    }

    public boolean isTrapItem(@NotNull ItemStack itemStack) {
        return getTrapType(itemStack) != null;
    }

    public @Nullable
    TrapType getTrapType(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getTrapType(itemStack.getItemMeta());
    }

    public @Nullable
    TrapType getTrapType(@NotNull ItemMeta meta) {
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        return get(lore.get(0).split(" ")[1]);
    }

    public @Nullable
    TrapType.TrapInstanceBuilder getTrapInstance(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getTrapInstance(itemStack.getItemMeta());
    }

    public @Nullable
    TrapType.TrapInstanceBuilder getTrapInstance(@NotNull ItemMeta meta) {
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        TrapType type = get(lore.get(0).split(" ")[1]);
        if (type == null)
            return null;
        return type.getBuilder().fromItemLines(lore);
    }

}
