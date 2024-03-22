package emanondev.deepdungeons.populator.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.MobPopulator;
import emanondev.deepdungeons.populator.APaperPopulatorType;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MythicMobsType extends APaperPopulatorType {

    public MythicMobsType() {
        super("mythicmobs");
    }

    @Override
    @NotNull
    public MythicMobsInstance read(@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new MythicMobsInstance(room, sub);
    }

    @NotNull
    @Override
    public APopulatorBuilder getBuilder(@NotNull RoomType.RoomBuilder room) {
        return new MythicMobsBuilder(room);
    }

    @Override
    @NotNull
    public MythicMobsPaperBuilder getPaperBuilder() {
        return new MythicMobsPaperBuilder();
    }

    public class MythicMobsBuilder extends APopulatorBuilder {
        private final List<Location> offsets = new ArrayList<>();
        @Nullable
        private MythicMob type = null;
        private int min = 1;
        private int max = 1;
        private int levelMin = 1;
        private int levelMax = 1;

        public MythicMobsBuilder(@NotNull RoomType.RoomBuilder room) {
            super(room);
        }


        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            if (offsets.isEmpty())
                throw new Exception("Location not set");
            if (type == null)
                throw new IllegalStateException();
            section.set("mobtype", type.getInternalName());
            section.set("min", min);
            section.set("max", max);
            section.set("level_min", levelMin);
            section.set("level_max", levelMax);
            List<String> offsetsString = new ArrayList<>();
            offsets.forEach(off -> offsetsString.add(Util.toStringNoWorld(off)));
            section.set("offsets", offsetsString);
        }

        public void toggleOffset(@NotNull Location location) {
            location = location.clone();
            location.setWorld(null);
            location.subtract(getRoomOffset());
            location.setX(location.getBlockX() + 0.5D);
            location.setY(location.getBlockY());
            location.setZ(location.getBlockZ() + 0.5D);
            Location finalLocation = location;
            if (!offsets.removeIf(loc -> CUtils.isEqual(loc,finalLocation)))
                offsets.add(location);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    if (event.getClickedBlock() == null)
                        return;
                    Location loc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation();
                    loc.setYaw(event.getPlayer().getLocation().getYaw() + 180);
                    this.toggleOffset(loc);
                    this.getRoomBuilder().setupTools();
                }
                case 6 -> {
                    if (offsets.isEmpty() || type == null) {
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
            inv.setItem(0, CUtils.createItem(player, Material.PAPER, "populatorbuilder.mythicmobs_info"));
            inv.setItem(1, CUtils.createItem(player, Material.STICK, offsets.size(), false, "populatorbuilder.mythicmobs_selector"));
            inv.setItem(6, CUtils.createItem(player, Material.LIME_DYE, "populatorbuilder.base_confirm"));
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            offsets.forEach(loc -> CUtils.markBlock(player, loc.toVector().add(getRoomOffset()).toBlockVector(), color));
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.SPAWNER).setDescription(
                    new DMessage(DeepDungeons.get(), player)
                            .append("<!i><gold><b>MobType</b>").newLine()//TODO lang
                            .append("<gold><blue>Type:</blue> " + (type == null ? "null" : type.getInternalName()))).setGuiProperty().build(),
                    (String text, MythicMob type) -> {
                        String[] split = text.split(" ");
                        for (String s : split) {
                            if (!(type.getInternalName().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                return false;
                        }
                        return true;
                    },
                    (InventoryClickEvent event, MythicMob type) -> {
                        setEntityType(type);
                        gui.open(player);
                        return false;
                    },
                    (MythicMob type) -> new ItemBuilder(Material.ZOMBIE_HEAD).setDescription(
                            new DMessage(DeepDungeons.get(), player)
                                    .append("<!i><gold><b>" + type.getInternalName() + "</b>").newLine()//TODO lang
                                    .append("<gold><blue>Type:</blue> " + (type.getInternalName()))).setGuiProperty().build(),
                    () -> {
                        ArrayList<MythicMob> list = new ArrayList<>(MythicBukkit.inst().getMobManager().getMobTypes());
                        list.sort((t1, t2) -> t1.getInternalName().compareToIgnoreCase(t2.getInternalName()));
                        return list;
                    }
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 100, this::getMin, this::setMin, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, min))
                            .setDescription(new DMessage(DeepDungeons.get(), player).append("<gold>Minimum spawned Mobs").newLine()
                                    .append("<gold><blue>Max: </blue>" + max).newLine()//TODO lang
                                    .append("<gold><blue>Min: </blue>" + min)).setGuiProperty().build(), true
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 100, this::getMax, this::setMax, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, max)).setDescription(//TODO lang
                            new DMessage(DeepDungeons.get(), player).append("<gold><b>Maximus spawned Mobs</b>").newLine()
                                    .append("<gold><blue>Max: </blue>" + max).newLine()
                                    .append("<gold><blue>Min: </blue>" + min)).setGuiProperty().build(), true));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 10000, this::getLevelMin, this::setLevelMin, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, levelMin))//TODO lang
                            .setDescription(new DMessage(DeepDungeons.get(), player).append("<gold>Minimum Level spawned Mobs").newLine()
                                    .append("<gold><blue>LevelMax: </blue>" + levelMax).newLine()
                                    .append("<gold><blue>LevelMin: </blue>" + levelMin)).setGuiProperty().build(), true
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 10000, this::getLevelMax, this::setLevelMax, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, levelMax)).setDescription(//TODO lang
                            new DMessage(DeepDungeons.get(), player).append("<gold><b>Maximus Level spawned Mobs</b>").newLine()
                                    .append("<gold><blue>LevelMax: </blue>" + levelMax).newLine()
                                    .append("<gold><blue>LevelMin: </blue>" + levelMin)).setGuiProperty().build(), true));
        }

        public int getLevelMin() {
            return levelMin;
        }

        public void setLevelMin(int min) {
            if (min < 0)
                min = 0;
            if (min > 10000)
                min = 10000;
            if (min > levelMax)
                this.levelMax = min;
            this.levelMin = min;
        }

        public int getLevelMax() {
            return levelMax;
        }

        public void setLevelMax(int max) {
            if (max < 1)
                max = 1;
            if (max > 10000)
                max = 10000;
            if (max < levelMin)
                this.levelMin = max;
            this.levelMax = max;
        }

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            if (min < 0)
                min = 0;
            if (min > 100)
                min = 100;
            if (min > max)
                this.max = min;
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            if (max < 1)
                max = 1;
            if (max > 100)
                max = 100;
            if (max < min)
                this.min = max;
            this.max = max;
        }

        public void setEntityType(@NotNull MythicMob type) {
            this.type = type;
        }
    }

    private class MythicMobsPaperBuilder extends APaperPopulatorBuilder {

        @Nullable
        private MythicMob type = null;
        private int min = 1;
        private int max = 1;
        private int levelMin = 1;
        private int levelMax = 1;

        @Override
        public boolean preserveContainer() {
            return false;
        }

        @Override
        @NotNull
        protected List<String> toItemLinesImpl() {
            List<String> list = new ArrayList<>();
            list.add("&9MobType:&6 " + (type == null ? "null" : type.getInternalName()));
            list.add("&9Min:&6 " + min);
            list.add("&9Max:&6 " + max);
            list.add("&9LevelMin:&6 " + levelMin);
            list.add("&9LevelMax:&6 " + levelMax);
            return list;
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            Location offset = getOffset();
            if (offset == null)
                throw new Exception("Location not set");
            if (type == null)
                throw new IllegalStateException();
            section.set("mobtype", type.getInternalName());
            section.set("min", min);
            section.set("max", max);
            section.set("level_min", levelMin);
            section.set("level_max", levelMax);
            section.set("offsets", List.of(Util.toStringNoWorld(offset)));
        }

        @Override
        public void fromItemLinesImpl(@NotNull List<String> lines) {
            type = MythicBukkit.inst().getMobManager().getMythicMob(lines.get(0).split(" ")[1]).orElse(null);
            min = Integer.parseInt(lines.get(1).split(" ")[1]);
            max = Integer.parseInt(lines.get(2).split(" ")[1]);
            levelMin = Integer.parseInt(lines.get(3).split(" ")[1]);
            levelMax = Integer.parseInt(lines.get(4).split(" ")[1]);
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.SPAWNER).setDescription(
                    new DMessage(DeepDungeons.get(), player)
                            .append("<!i><gold><b>MobType</b>").newLine()//TODO lang
                            .append("<gold><blue>Type:</blue> " + (type == null ? "null" : type.getInternalName()))).setGuiProperty().build(),
                    (String text, MythicMob type) -> {
                        String[] split = text.split(" ");
                        for (String s : split) {
                            if (!(type.getInternalName().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))))
                                return false;
                        }
                        return true;
                    },
                    (InventoryClickEvent event, MythicMob type) -> {
                        setEntityType(type);
                        gui.open(player);
                        return false;
                    },
                    (MythicMob type) -> new ItemBuilder(Material.ZOMBIE_HEAD).setDescription(
                            new DMessage(DeepDungeons.get(), player)
                                    .append("<!i><gold><b>" + type.getInternalName() + "</b>").newLine()//TODO lang
                                    .append("<gold><blue>Type:</blue> " + (type.getInternalName()))).setGuiProperty().build(),
                    () -> {
                        ArrayList<MythicMob> list = new ArrayList<>(MythicBukkit.inst().getMobManager().getMobTypes());
                        list.sort((t1, t2) -> t1.getInternalName().compareToIgnoreCase(t2.getInternalName()));
                        return list;
                    }
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 100, this::getMin, this::setMin, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, min))
                            .setDescription(new DMessage(DeepDungeons.get(), player).append("<gold>Minimum spawned Mobs").newLine()
                                    .append("<gold><blue>Max: </blue>" + max).newLine()//TODO lang
                                    .append("<gold><blue>Min: </blue>" + min)).setGuiProperty().build(), true
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 100, this::getMax, this::setMax, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, max)).setDescription(//TODO lang
                            new DMessage(DeepDungeons.get(), player).append("<gold><b>Maximus spawned Mobs</b>").newLine()
                                    .append("<gold><blue>Max: </blue>" + max).newLine()
                                    .append("<gold><blue>Min: </blue>" + min)).setGuiProperty().build(), true));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 10000, this::getLevelMin, this::setLevelMin, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, levelMin))//TODO lang
                            .setDescription(new DMessage(DeepDungeons.get(), player).append("<gold>Minimum Level spawned Mobs").newLine()
                                    .append("<gold><blue>LevelMax: </blue>" + levelMax).newLine()
                                    .append("<gold><blue>LevelMin: </blue>" + levelMin)).setGuiProperty().build(), true
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 10000, this::getLevelMax, this::setLevelMax, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, levelMax)).setDescription(//TODO lang
                            new DMessage(DeepDungeons.get(), player).append("<gold><b>Maximus Level spawned Mobs</b>").newLine()
                                    .append("<gold><blue>LevelMax: </blue>" + levelMax).newLine()
                                    .append("<gold><blue>LevelMin: </blue>" + levelMin)).setGuiProperty().build(), true));
        }

        public int getLevelMin() {
            return levelMin;
        }

        public void setLevelMin(int min) {
            if (min < 0)
                min = 0;
            if (min > 10000)
                min = 10000;
            if (min > levelMax)
                this.levelMax = min;
            this.levelMin = min;
        }

        public int getLevelMax() {
            return levelMax;
        }

        public void setLevelMax(int max) {
            if (max < 1)
                max = 1;
            if (max > 10000)
                max = 10000;
            if (max < levelMin)
                this.levelMin = max;
            this.levelMax = max;
        }

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            if (min < 0)
                min = 0;
            if (min > 100)
                min = 100;
            if (min > max)
                this.max = min;
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            if (max < 1)
                max = 1;
            if (max > 100)
                max = 100;
            if (max < min)
                this.min = max;
            this.max = max;
        }

        public void setEntityType(@NotNull MythicMob type) {
            this.type = type;
        }
    }

    private class MythicMobsInstance extends APopulatorInstance implements MobPopulator {

        private final List<Location> offsets = new ArrayList<>();
        private final MythicMob entityType;
        private final int min;
        private final int max;
        private final int levelMin;
        private final int levelMax;

        public MythicMobsInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            entityType = MythicBukkit.inst().getMobManager().getMythicMob(section.getString("mobtype", "null")).orElse(null);
            min = section.getInt("min", 1);
            max = section.getInt("max", min);
            levelMin = section.getInt("level_min", 1);
            levelMax = section.getInt("level_max", 1);
            section.getStringList("offsets", Collections.emptyList()).forEach(val -> offsets.add(Util.toLocationNoWorld(val)));
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @NotNull
        @Override
        public Collection<Entity> spawnMobs(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
            List<Location> randomPick = new ArrayList<>(offsets.size());
            if (offsets.isEmpty())
                return Collections.emptyList();
            int amount = random.nextInt() % (max - min + 1) + min;
            List<Entity> entities = new ArrayList<>();
            if (entityType == null)
                return entities;

            for (int i = 0; i < amount; i++) {
                if (randomPick.isEmpty()) {
                    randomPick.addAll(offsets);
                    Collections.shuffle(randomPick, random);
                }
                Location location = CUtils.sum(handler.getLocation(), randomPick.remove(randomPick.size() - 1));
                int level = new Random().nextInt() % (levelMax - levelMin + 1) + levelMin;
                Entity entity = BukkitAdapter.adapt(entityType.spawn(BukkitAdapter.adapt(location), level).getEntity());
                entity.setPersistent(true);
                if (entity.isValid())
                    entities.add(entity);
                else {
                    DeepDungeons.get().logIssue("Failed to spawn monster at &e" + location.getWorld() + " "
                            + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
                    //TODO more info
                }
            }
            return entities;
        }

        @Override
        public boolean spawnGuardians() {
            return true;
        }
    }
}
