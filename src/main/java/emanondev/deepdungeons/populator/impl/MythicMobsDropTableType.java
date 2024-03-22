package emanondev.deepdungeons.populator.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.ItemPopulator;
import emanondev.deepdungeons.populator.APaperPopulatorType;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.drops.DropTable;
import io.lumine.mythic.core.drops.LootBag;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MythicMobsDropTableType extends APaperPopulatorType {

    public MythicMobsDropTableType() {
        super("mythicmobsloottable");
    }

    @Override
    @NotNull
    public MythicMobsDropTableInstance read(@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new MythicMobsDropTableInstance(room, sub);
    }

    @NotNull
    @Override
    public APopulatorBuilder getBuilder(@NotNull RoomType.RoomBuilder room) {
        return new MythicMobsDropTableBuilder(room);
    }

    @Override
    @NotNull
    public MythicMobsDropTablePaperBuilder getPaperBuilder() {
        return new MythicMobsDropTablePaperBuilder();
    }

    public class MythicMobsDropTableBuilder extends APopulatorBuilder {

        private final List<Location> offsets = new ArrayList<>();
        private DropTable table = null;

        public MythicMobsDropTableBuilder(@NotNull RoomType.RoomBuilder room) {
            super(room);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    if (event.getClickedBlock() == null)
                        return;
                    if (event.getClickedBlock().getState() instanceof Container)
                        this.toggleOffset(event.getClickedBlock().getLocation());
                    else
                        this.toggleOffset(event.getClickedBlock().getRelative(event.getBlockFace()).getLocation());
                    this.getRoomBuilder().setupTools();
                }
                case 6 -> {
                    if (offsets.isEmpty() || table == null) {
                        //TODO lang uncompleted
                        return;
                    }
                    this.complete();
                    this.getRoomBuilder().setupTools();
                }
            }
        }

        @Override
        protected void setupToolsImpl(@NotNull PlayerInventory inv, @NotNull Player player) {
            inv.setItem(0, CUtils.createItem(player, Material.PAPER, "populatorbuilder.mythicmobsloottable_info"));
            inv.setItem(1, CUtils.createItem(player, Material.STICK, offsets.size(), false, "populatorbuilder.mythicmobsloottable_selector"));
            inv.setItem(6, CUtils.createItem(player, Material.LIME_DYE, "populatorbuilder.base_confirm"));
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            offsets.forEach(loc -> CUtils.markBlock(player, loc.toVector().add(getRoomOffset()).toBlockVector(), color));
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.CHEST).setDescription(
                    new DMessage(DeepDungeons.get(), player) //TODO
                            .append("<!i><gold><b>MythicMobsDropTable</b>").newLine()//TODO lang
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
                        gui.open(player);
                        return false;
                    },
                    (DropTable lootTable) -> new ItemBuilder(Material.CHEST).setDescription(
                            new DMessage(DeepDungeons.get(), player) //TODO lang
                                    .append("<!i><gold><b>" + lootTable.getInternalName() + "</b>")).setGuiProperty().build(),
                    () -> {
                        ArrayList<DropTable> list = new ArrayList<>(MythicBukkit.inst().getDropManager().getDropTables());
                        list.sort(Comparator.comparing(DropTable::getInternalName));
                        return list;
                    }
            ));
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            if (table == null || offsets.isEmpty())
                throw new Exception("Location not set");
            section.set("table", table.getInternalName());
            List<String> offsetsString = new ArrayList<>();
            offsets.forEach(off -> offsetsString.add(Util.toStringNoWorld(off)));
            section.set("offsets", offsetsString);
        }

        @NotNull
        public DropTable getTable() {
            return table;
        }

        public void setTable(@NotNull DropTable table) {
            this.table = table;
        }

        public void toggleOffset(@NotNull Location location) {
            location = location.clone();
            location.setWorld(null);
            location.subtract(getRoomOffset());
            location.setX(location.getBlockX() + 0.5D);
            location.setY(location.getBlockY());
            location.setZ(location.getBlockZ() + 0.5D);
            if (!offsets.remove(location))
                offsets.add(location);
        }
    }

    public class MythicMobsDropTablePaperBuilder extends APaperPopulatorBuilder {

        private DropTable table = null;

        public MythicMobsDropTablePaperBuilder() {
            super();
        }

        @Override
        public boolean preserveContainer() {
            return true;
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
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            Location offset = getOffset();
            if (offset == null)
                throw new Exception("Location not set");
            if (table == null)
                throw new IllegalStateException();
            section.set("table", table.getInternalName());
            section.set("offsets", List.of(Util.toStringNoWorld(offset)));
        }

        /**
         * first line contains the type and shall be ignored
         */
        @Override
        public void fromItemLinesImpl(@NotNull List<String> lines) {
            table = MythicBukkit.inst().getDropManager().getDropTable(lines.get(0).split(" ")[1]).orElse(null);
        }


        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.CHEST).setDescription(
                    new DMessage(DeepDungeons.get(), player) //TODO
                            .append("<!i><gold><b>MythicMobsDropTable</b>").newLine()//TODO lang
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
                        gui.open(player);
                        return false;
                    },
                    (DropTable lootTable) -> new ItemBuilder(Material.CHEST).setDescription(
                            new DMessage(DeepDungeons.get(), player) //TODO lang
                                    .append("<!i><gold><b>" + lootTable.getInternalName() + "</b>")).setGuiProperty().build(),
                    () -> {
                        ArrayList<DropTable> list = new ArrayList<>(MythicBukkit.inst().getDropManager().getDropTables());
                        list.sort(Comparator.comparing(DropTable::getInternalName));
                        return list;
                    }
            ));
        }
    }

    public class MythicMobsDropTableInstance extends APopulatorInstance implements ItemPopulator {

        private final DropTable table;
        private final List<Location> offsets = new ArrayList<>();

        private MythicMobsDropTableInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            table = MythicBukkit.inst().getDropManager().getDropTable(section.getString("table", "null")).orElse(null);
            if (table == null) {
                DeepDungeons.get().logIssue("Failed to find loot table &e" + section.getString("table", "null"));
                //TODO more info
            }
            section.getStringList("offsets", Collections.emptyList()).forEach(val -> offsets.add(Util.toLocationNoWorld(val)));
        }

        @Override
        public Map<Location, Collection<ItemStack>> getItems(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
            if (table == null)
                return Collections.emptyMap();
            Map<Location, Collection<ItemStack>> result = new HashMap<>();
            offsets.forEach(offset -> {
                Location loc = CUtils.sum(handler.getLocation(),offset);
                ArrayList<ItemStack> list = new ArrayList<>();
                LootBag bag = table.generate();
                bag.getLootTable().forEach(drop -> {
                    if (drop instanceof IItemDrop idrop) {
                        ItemStack item = BukkitAdapter.adapt(idrop.getDrop(bag.getMetadata(), drop.getAmount()));
                        if (item != null)
                            list.add(item);
                    }
                });
                result.put(loc, list);
            });
            return result;
        }
    }
}
