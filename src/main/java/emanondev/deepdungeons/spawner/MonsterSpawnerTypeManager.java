package emanondev.deepdungeons.spawner;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.spawner.impl.VanillaMobsType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MonsterSpawnerTypeManager extends DRegistry<MonsterSpawnerType> {

    public static final String LINE_ONE = "MONSTERSPAWNER BLUEPRINT";
    private static final MonsterSpawnerTypeManager instance = new MonsterSpawnerTypeManager();

    private MonsterSpawnerTypeManager() {
        super(DeepDungeons.get(), "MonsterSpawnerManager", true);
        this.register(new VanillaMobsType());
    }

    public static @NotNull MonsterSpawnerTypeManager getInstance() {
        return instance;
    }

    public @Nullable MonsterSpawnerType getMonsterSpawnerType(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getMonsterSpawnerType(itemStack.getItemMeta());
    }

    public @Nullable MonsterSpawnerType getMonsterSpawnerType(@NotNull ItemMeta meta) {
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        return get(lore.get(0).split(" ")[1]);
    }

    public MonsterSpawnerType.MonsterSpawnerInstanceBuilder getMonsterSpawnerInstance(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getMonsterSpawnerInstance(itemStack.getItemMeta());
    }

    public MonsterSpawnerType.MonsterSpawnerInstanceBuilder getMonsterSpawnerInstance(@NotNull ItemMeta meta) {
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.size() == 0)
            return null;
        MonsterSpawnerType type = get(lore.get(0).split(" ")[1]);
        if (type == null)
            return null;
        return type.getBuilder().fromItemLines(lore);
    }
}
