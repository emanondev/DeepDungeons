package emanondev.deepdungeons.spawner;

import emanondev.core.Hooks;
import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.spawner.impl.MythicMobsType;
import emanondev.deepdungeons.spawner.impl.VanillaMobsType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MonsterSpawnerTypeManager extends DRegistry<MonsterSpawnerType> {

    public static final String LINE_ONE = "MONSTERSPAWNER BLUEPRINT";
    private static final MonsterSpawnerTypeManager instance = new MonsterSpawnerTypeManager();

    private MonsterSpawnerTypeManager() {
        super(DeepDungeons.get(), "MonsterSpawnerManager", true);
        this.register(new VanillaMobsType());
        if (Hooks.isMythicMobsEnabled())
            this.register(new MythicMobsType());
    }

    public static @NotNull
    MonsterSpawnerTypeManager getInstance() {
        return instance;
    }

    @Contract("null -> null")
    public @Nullable
    MonsterSpawnerType getMonsterSpawnerType(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getMonsterSpawnerType(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    public @Nullable
    MonsterSpawnerType getMonsterSpawnerType(@Nullable ItemMeta meta) {
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
    public MonsterSpawnerType.MonsterSpawnerInstanceBuilder getMonsterSpawnerInstance(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getMonsterSpawnerInstance(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    public MonsterSpawnerType.MonsterSpawnerInstanceBuilder getMonsterSpawnerInstance(@Nullable ItemMeta meta) {
        if (meta == null)
            return null;
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
