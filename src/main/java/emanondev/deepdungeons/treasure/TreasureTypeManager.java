package emanondev.deepdungeons.treasure;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.treasure.impl.LootTableType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
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

    @Contract("null -> null")
    public @Nullable TreasureType getTreasureType(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getTreasureType(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    public @Nullable TreasureType getTreasureType(@Nullable ItemMeta meta) {
        if (meta == null)
            return null;
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        return get(lore.get(0).split(" ")[1]);
    }

    @Contract("null -> null")
    public @Nullable TreasureType.TreasureInstanceBuilder getTreasureInstance(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getTreasureInstance(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    public @Nullable TreasureType.TreasureInstanceBuilder getTreasureInstance(@Nullable ItemMeta meta) {
        if (meta == null)
            return null;
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
