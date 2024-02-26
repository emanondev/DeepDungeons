package emanondev.deepdungeons.treasure.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.treasure.TreasureType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
            return List.of("LootTable: " + table.getKey());
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("table", table.getKey());
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
                String args[] = lines.get(1).split(" ")[1].split(":");
                table = Bukkit.getLootTable(new NamespacedKey(args[0], args[1]));
            }
            return this;
        }

    }

    public class LootTableInstance extends TreasureInstance {

        private final LootTable table;

        private LootTableInstance(@NotNull RoomType.RoomInstance room, @NotNull YMLSection sub) {
            super(room);
            String[] args = sub.getString("table").split(":");
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
