package emanondev.deepdungeons.paperpopulator;

import emanondev.core.Hooks;
import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.paperpopulator.PaperPopulatorType.PaperPopulatorBuilder;
import emanondev.deepdungeons.paperpopulator.impl.LootTableType;
import emanondev.deepdungeons.paperpopulator.impl.MythicMobsDropTableType;
import emanondev.deepdungeons.paperpopulator.impl.MythicMobsType;
import emanondev.deepdungeons.paperpopulator.impl.VanillaMobsType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PopulatorTypeManager extends DRegistry<PaperPopulatorType> {

    public static final String LINE_ONE = "POPULATOR BLUEPRINT";
    private static final PopulatorTypeManager instance = new PopulatorTypeManager();

    private PopulatorTypeManager() {
        super(DeepDungeons.get(), "PaperPopulatorManager", true);
        this.register(new VanillaMobsType());
        this.register(new LootTableType());
        if (Hooks.isMythicMobsEnabled()) {
            this.register(new MythicMobsDropTableType());
            this.register(new MythicMobsType());
        }
    }

    @NotNull
    public static PopulatorTypeManager getInstance() {
        return instance;
    }

    @Contract("null -> null")
    @Nullable
    public PaperPopulatorType getPopulatorType(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getPopulatorType(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    @Nullable
    public PaperPopulatorType getPopulatorType(@Nullable ItemMeta meta) {
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
    public PaperPopulatorBuilder getPopulatorBuilder(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getPopulatorBuilder(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    public PaperPopulatorBuilder getPopulatorBuilder(@Nullable ItemMeta meta) {
        if (meta == null)
            return null;
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        PaperPopulatorType type = get(lore.get(0).split(" ")[1]);
        if (type == null)
            return null;
        return type.getBuilder().fromItemLines(lore);
    }
}
