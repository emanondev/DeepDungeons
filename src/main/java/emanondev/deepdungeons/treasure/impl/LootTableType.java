package emanondev.deepdungeons.treasure.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.treasure.TreasureType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LootTableType extends TreasureType {

    public LootTableType() {
        super("loottable");
    }

    @Override
    public @NotNull LootTableType.LootTableInstance read(@NotNull RoomType.RoomInstance room, @NotNull YMLSection sub) {
        return new LootTableInstance(room, sub);
    }

    @Override
    public @NotNull LootTableType.LootTableInstanceBuilder getBuilder() {
        return new LootTableInstanceBuilder();
    }


    public class LootTableInstanceBuilder extends TreasureInstanceBuilder {

        private LootTable table = LootTables.SIMPLE_DUNGEON.getLootTable();

        public LootTableInstanceBuilder() {
            super();
        }

        public @NotNull LootTable getTable() {
            return table;
        }

        public void setTable(@NotNull LootTable table) {
            this.table = table;
        }

        @Override
        protected @NotNull List<String> toItemLinesImpl() {
            return List.of("&9LootTable:&6 " + table.getKey());
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("table", table.getKey().toString());
        }

        /**
         * first line contains the type and shall be ignored
         *
         * @param lines
         * @return
         */
        @Override
        @Contract("_ -> this")
        public TreasureInstanceBuilder fromItemLines(@NotNull List<String> lines) {
            if (lines.size() >= 2) {
                String[] args = lines.get(1).split(" ")[1].split(":");
                table = Bukkit.getLootTable(new NamespacedKey(args[0], args[1]));
            }
            return this;
        }

        @Override
        protected void craftGuiButtons(@NotNull PagedMapGui gui) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.CHEST).setDescription(
                    new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                            .append("<!i><gold><b>LootTable</b>").newLine()
                            .append("<gold>Type:<blue> " + (table.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ?
                                    table.getKey().toString().substring(10) : table.getKey().toString()) + "</blue>")).setGuiProperty().build(),
                    (String text, LootTables lootTable) -> {
                        String[] split = text.split(" ");
                        for (String s : split) {
                            if (!(lootTable.name().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))
                                    || lootTable.getKey().toString().contains(s.toLowerCase(Locale.ENGLISH))))
                                return false;
                        }
                        return true;
                    },
                    (InventoryClickEvent event, LootTables lootTable) -> {
                        setTable(lootTable.getLootTable());
                        gui.open(gui.getTargetPlayer());
                        gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
                        return false;
                    },
                    (LootTables lootTable) -> new ItemBuilder(Material.CHEST).setDescription(
                            new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                                    .append("<!i><gold><b>" + lootTable.name() + "</b>").newLine()
                                    .append("<gold>Type:<blue> " + (lootTable.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ?
                                            lootTable.getKey().toString().substring(10) : lootTable.getKey().toString()) + "</blue>")).setGuiProperty().build(),
                    () -> {
                        ArrayList<LootTables> list = new ArrayList<>(Arrays.asList(LootTables.values()));
                        list.sort(Comparator.comparing(l -> l.getKey().getKey()));
                        return list;
                    }
            ));
        }
    }

    public class LootTableInstance extends TreasureInstance {

        private final LootTable table;

        private LootTableInstance(@NotNull RoomType.RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            String[] args = section.getString("table").split(":");
            table = Bukkit.getLootTable(new NamespacedKey(args[0], args[1]));
            if (table == null) {
                //TODO
            }
        }

        @Override
        public @NotNull Collection<ItemStack> getTreasure(@NotNull Random random, @NotNull Location location, @Nullable Player who) {
            if (table == null)
                return Collections.emptyList();
            return table.populateLoot(random, new LootContext.Builder(location).killer(who).lootingModifier(0).build());
        }
    }
}
