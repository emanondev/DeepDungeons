package emanondev.deepdungeons.populator;

import emanondev.core.Hooks;
import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.interfaces.PaperPopulatorType;
import emanondev.deepdungeons.interfaces.PaperPopulatorType.PaperPopulatorBuilder;
import emanondev.deepdungeons.populator.impl.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PopulatorTypeManager extends DRegistry<APopulatorType> {

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
        this.register(new DeleteEmptyContainerType());
    }

    @NotNull
    public static PopulatorTypeManager getInstance() {
        return instance;
    }

    @Nullable
    public PaperPopulatorType getPaper(String id) {
        return get(id) instanceof PaperPopulatorType pap ? pap : null;
    }

    @NotNull
    public Collection<PaperPopulatorType> getAllPapers() {
        List<PaperPopulatorType> values = new ArrayList<>();
        getAll().forEach(pop -> {
            if (pop instanceof PaperPopulatorType paper) values.add(paper);
        });
        return values;
    }

    @NotNull
    public Collection<String> getPaperIds() {
        List<String> values = new ArrayList<>();
        getAll().forEach(pop -> {
            if (pop instanceof PaperPopulatorType paper) values.add(paper.getId());
        });
        return Collections.unmodifiableCollection(values);
    }

    @Contract("null -> null")
    @Nullable
    public PaperPopulatorType getPaperPopulatorType(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getPaperPopulatorType(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    @Nullable
    public PaperPopulatorType getPaperPopulatorType(@Nullable ItemMeta meta) {
        if (meta == null)
            return null;
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.isEmpty())
            return null;
        APopulatorType val = get(lore.getFirst().split(" ")[1]);
        return val instanceof PaperPopulatorType paper ? paper : null;
    }

    @Contract("null -> null")
    public PaperPopulatorBuilder getPaperPopulatorBuilder(@Nullable ItemStack itemStack) {
        if (itemStack == null)
            return null;
        if (itemStack.getType() != Material.PAPER || !itemStack.hasItemMeta())
            return null;
        return getPaperPopulatorBuilder(itemStack.getItemMeta());
    }

    @Contract("null -> null")
    public PaperPopulatorBuilder getPaperPopulatorBuilder(@Nullable ItemMeta meta) {
        if (meta == null)
            return null;
        if (!meta.hasLore() || !LINE_ONE.equals(meta.getDisplayName()))
            return null;
        List<String> lore = meta.getLore();
        if (lore.isEmpty())
            return null;
        APopulatorType type = get(lore.getFirst().split(" ")[1]);
        if (type == null)
            return null;
        if (!(type instanceof PaperPopulatorType paperPop))
            return null;
        return paperPop.getPaperBuilder().fromItemLines(lore);
    }
}
