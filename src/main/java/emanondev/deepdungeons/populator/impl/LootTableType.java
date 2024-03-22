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
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.*;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LootTableType extends APaperPopulatorType {

    public LootTableType() {
        super("loottable");
    }

    @Override
    @NotNull
    public LootTableInstance read(@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new LootTableInstance(room, sub);
    }

    @NotNull
    @Override
    public APopulatorBuilder getBuilder(@NotNull RoomBuilder room) {
        return new LootTableBuilder(room);
    }

    @Override
    @NotNull
    public LootTablePaperBuilder getPaperBuilder() {
        return new LootTablePaperBuilder();
    }


    public class LootTableBuilder extends APopulatorBuilder {

        private final List<Location> offsets = new ArrayList<>();
        private LootTable table = LootTables.SIMPLE_DUNGEON.getLootTable();


        public LootTableBuilder(@NotNull RoomBuilder room) {
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
            inv.setItem(0, CUtils.createItem(player, Material.PAPER, "populatorbuilder.loottable_info"));
            inv.setItem(1, CUtils.createItem(player, Material.STICK, offsets.size(), false, "populatorbuilder.loottable_selector"));
            inv.setItem(6, CUtils.createItem(player, Material.LIME_DYE, "populatorbuilder.base_confirm"));
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            offsets.forEach(loc -> CUtils.markBlock(player, loc.toVector().add(getRoomOffset()).toBlockVector(), color));
        }

        @NotNull
        public LootTable getTable() {
            return table;
        }

        public void setTable(@NotNull LootTable table) {
            this.table = table;
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            if (table == null || offsets.isEmpty())
                throw new Exception("Location not set");
            section.set("table", table.getKey().toString());
            List<String> offsetsString = new ArrayList<>();
            offsets.forEach(off -> offsetsString.add(Util.toStringNoWorld(off)));
            section.set("offsets", offsetsString);
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.CHEST).setDescription(
                    new DMessage(DeepDungeons.get(), player)
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
                        gui.open(player);
                        return false;
                    },
                    (LootTables lootTable) -> new ItemBuilder(Material.CHEST).setDescription(
                            new DMessage(DeepDungeons.get(), player)
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

    public class LootTablePaperBuilder extends APaperPopulatorBuilder {

        private LootTable table = LootTables.SIMPLE_DUNGEON.getLootTable();


        public LootTablePaperBuilder() {
            super();
        }

        @Override
        public boolean preserveContainer() {
            return true;
        }

        @NotNull
        public LootTable getTable() {
            return table;
        }

        public void setTable(@NotNull LootTable table) {
            this.table = table;
        }

        @Override
        @NotNull
        protected List<String> toItemLinesImpl() {
            return List.of("&9LootTable:&6 " + table.getKey());
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            Location offset = getOffset();
            if (offset == null)
                throw new Exception("Location not set");
            section.set("table", table.getKey().toString());
            section.set("offsets", List.of(Util.toStringNoWorld(offset)));
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.CHEST).setDescription(
                    new DMessage(DeepDungeons.get(), player)
                            .append("<!i><gold><b>LootTable</b>").newLine()//TODO lang
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
                        gui.open(player);
                        player.getInventory().setItemInMainHand(this.toItem());
                        return false;
                    },
                    (LootTables lootTable) -> new ItemBuilder(Material.CHEST).setDescription(
                            new DMessage(DeepDungeons.get(), player)
                                    .append("<!i><gold><b>" + lootTable.name() + "</b>").newLine()//TODO lang
                                    .append("<gold>Type:<blue> " + (lootTable.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ?
                                            lootTable.getKey().toString().substring(10) : lootTable.getKey().toString()) + "</blue>")).setGuiProperty().build(),
                    () -> {
                        ArrayList<LootTables> list = new ArrayList<>(Arrays.asList(LootTables.values()));
                        list.sort(Comparator.comparing(l -> l.getKey().getKey()));
                        return list;
                    }
            ));
        }

        /**
         * first line contains the type and shall be ignored
         *
         * @param lines
         */
        @Override
        public void fromItemLinesImpl(@NotNull List<String> lines) {
            String[] args = lines.get(0).split(" ")[1].split(":");
            table = Bukkit.getLootTable(new NamespacedKey(args[0], args[1]));
        }
    }

    public class LootTableInstance extends APopulatorInstance implements ItemPopulator {

        private final LootTable table;
        private final List<Location> offsets = new ArrayList<>();

        private LootTableInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            String[] args = section.getString("table").split(":");
            table = Bukkit.getLootTable(new NamespacedKey(args[0], args[1]));
            if (table == null) {
                DeepDungeons.get().logIssue("Failed to find loot table &e" + args[0] + ":" + args[1]);
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
                result.put(loc, table.populateLoot(random, new LootContext.Builder(loc).killer(who).lootingModifier(0).build()));
            });
            return result;
        }

    }
}
