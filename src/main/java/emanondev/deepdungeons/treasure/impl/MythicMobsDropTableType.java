package emanondev.deepdungeons.treasure.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.treasure.TreasureType;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.drops.DropTable;
import io.lumine.mythic.core.drops.LootBag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MythicMobsDropTableType extends TreasureType {

    public MythicMobsDropTableType() {
        super("mythicmobs_loottable");
    }

    @Override
    @NotNull
    public MythicMobsDropTableInstance read(@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new MythicMobsDropTableInstance(room, sub);
    }

    @Override
    @NotNull
    public MythicMobsDropTableInstanceBuilder getBuilder() {
        return new MythicMobsDropTableInstanceBuilder();
    }


    public class MythicMobsDropTableInstanceBuilder extends TreasureInstanceBuilder {

        private DropTable table = null;

        public MythicMobsDropTableInstanceBuilder() {
            super();
        }

        @NotNull
        public DropTable getTable() {
            return table;
        }

        public void setTable(@NotNull DropTable table) {
            this.table = table;
        }

        @Override
        @NotNull
        protected List<String> toItemLinesImpl() {
            return List.of("&9MythicMobsDropTable:&6 " + (table == null ? "null" : table.getInternalName()));
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            if (table == null)
                throw new IllegalStateException();
            section.set("table", table.getInternalName());
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
                table = MythicBukkit.inst().getDropManager().getDropTable(lines.get(1).split(" ")[1]).orElse(null);
            }
            return this;
        }


        @Override
        protected void craftGuiButtons(@NotNull PagedMapGui gui) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.CHEST).setDescription(
                    new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                            .append("<!i><gold><b>MythicMobsDropTable</b>").newLine()
                            .append("<gold>Type:<blue> " + (table == null ? "null" : table.getInternalName()) + "</blue>")).setGuiProperty().build(),
                    (String text, DropTable lootTable) -> {
                        String[] split = text.split(" ");
                        for (String s : split) {
                            if (!(lootTable.getInternalName().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                return false;
                        }
                        return true;
                    },
                    (InventoryClickEvent event, DropTable lootTable) -> {
                        setTable(lootTable);
                        gui.open(gui.getTargetPlayer());
                        gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
                        return false;
                    },
                    (DropTable lootTable) -> new ItemBuilder(Material.CHEST).setDescription(
                            new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                                    .append("<!i><gold><b>" + lootTable.getInternalName() + "</b>")).setGuiProperty().build(),
                    () -> {
                        ArrayList<DropTable> list = new ArrayList<>(MythicBukkit.inst().getDropManager().getDropTables());
                        list.sort(Comparator.comparing(DropTable::getInternalName));
                        return list;
                    }
            ));
        }
    }

    public class MythicMobsDropTableInstance extends TreasureInstance {

        private final DropTable table;

        private MythicMobsDropTableInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            table = MythicBukkit.inst().getDropManager().getDropTable(section.getString("table", "null")).orElse(null);
            if (table == null) {
                DeepDungeons.get().logIssue("Failed to find loot table &e" + section.getString("table", "null"));
                //TODO more info
            }
        }

        @Override
        @NotNull
        public Collection<ItemStack> getTreasure(@NotNull Random random, @NotNull Location location, @Nullable Player who) {
            if (table == null)
                return Collections.emptyList();
            ArrayList<ItemStack> list = new ArrayList<>();
            LootBag bag = table.generate();
            bag.getLootTable().forEach(drop -> {
                if (drop instanceof IItemDrop idrop) {
                    ItemStack item = BukkitAdapter.adapt(idrop.getDrop(bag.getMetadata(), drop.getAmount()));
                    if (item != null)
                        list.add(item);
                }
            });
            return list;
        }
    }
}
