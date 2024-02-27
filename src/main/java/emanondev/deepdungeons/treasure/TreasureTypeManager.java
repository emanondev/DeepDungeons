package emanondev.deepdungeons.treasure;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.treasure.impl.LootTableType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TreasureTypeManager extends DRegistry<TreasureType> {

    public static final String LINE_ONE = "TREASURE BLUEPRINT";
    private static final TreasureTypeManager instance = new TreasureTypeManager();

    private TreasureTypeManager() {
        super(DeepDungeons.get(), "TreasureTypeManager", true);
        this.register(new LootTableType());
    }

    public static TreasureTypeManager getInstance() {
        return instance;
    }

    public @Nullable TreasureType getTreasureType(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getTreasureType(itemStack.getItemMeta());
    }

    public @Nullable TreasureType getTreasureType(@NotNull ItemMeta meta) {
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        return get(lore.get(0).split(" ")[1]);
    }

    public @Nullable TreasureType.TreasureInstanceBuilder getTreasureInstance(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getTreasureInstance(itemStack.getItemMeta());
    }

    public @Nullable TreasureType.TreasureInstanceBuilder getTreasureInstance(@NotNull ItemMeta meta) {
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        TreasureType type = get(lore.get(0).split(" ")[1]);
        if (type == null)
            return null;
        return type.getBuilder().fromItemLines(lore);
    }

}
