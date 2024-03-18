package emanondev.deepdungeons.spawner.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.spawner.MonsterSpawnerType;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MythicMobsType extends MonsterSpawnerType {

    public MythicMobsType() {
        super("mythicmobs");
    }

    @Override
    @NotNull
    public MythicMobsInstance read(@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new MythicMobsInstance(room, sub);
    }

    @Override
    @NotNull
    public MythicMobsInstanceBuilder getBuilder() {
        return new MythicMobsInstanceBuilder();
    }

    private class MythicMobsInstanceBuilder extends MonsterSpawnerInstanceBuilder {

        @Nullable
        private MythicMob type = null;
        private int min = 1;
        private int max = 1;
        private int levelMin = 1;
        private int levelMax = 1;
        private double chance = 1;

        @Override
        @NotNull
        protected List<String> toItemLinesImpl() {
            List<String> list = new ArrayList<>();
            list.add("&9MobType:&6 " + (type == null ? "null" : type.getInternalName()));
            list.add("&9Min:&6 " + min);
            list.add("&9Max:&6 " + max);
            list.add("&9Chance:&6 " + UtilsString.formatOptional2Digit(chance * 100));
            list.add("&9LevelMin:&6 " + levelMin);
            list.add("&9LevelMax:&6 " + levelMax);
            return list;
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            if (type == null)
                throw new IllegalStateException();
            section.set("mobtype", type.getInternalName());
            section.set("min", min);
            section.set("max", max);
            section.set("chance", chance);
            section.set("level_min", levelMin);
            section.set("level_max", levelMax);
        }

        @Override
        public MonsterSpawnerInstanceBuilder fromItemLines(@NotNull List<String> lines) {
            type = MythicBukkit.inst().getMobManager().getMythicMob(lines.get(1).split(" ")[1]).orElse(null);
            min = Integer.parseInt(lines.get(2).split(" ")[1]);
            max = Integer.parseInt(lines.get(3).split(" ")[1]);
            chance = Double.parseDouble(lines.get(4).split(" ")[1]) / 100;
            levelMin = Integer.parseInt(lines.get(5).split(" ")[1]);
            levelMax = Integer.parseInt(lines.get(6).split(" ")[1]);
            return this;
        }

        @Override
        protected void craftGuiButtons(@NotNull PagedMapGui gui) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.SPAWNER).setDescription(
                    new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                            .append("<!i><gold><b>MobType</b>").newLine()
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
                        gui.open(gui.getTargetPlayer());
                        gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
                        return false;
                    },
                    (MythicMob type) -> new ItemBuilder(Material.ZOMBIE_HEAD).setDescription(
                            new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                                    .append("<!i><gold><b>" + type.getInternalName() + "</b>").newLine()
                                    .append("<gold><blue>Type:</blue> " + (type.getInternalName()))).setGuiProperty().build(),
                    () -> {
                        ArrayList<MythicMob> list = new ArrayList<>(MythicBukkit.inst().getMobManager().getMobTypes());
                        list.sort((t1, t2) -> t1.getInternalName().compareToIgnoreCase(t2.getInternalName()));
                        return list;
                    }
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 100, this::getMin, (val) -> {
                this.setMin(val);
                gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
            }, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, min))
                            .setDescription(new DMessage(DeepDungeons.get(), gui.getTargetPlayer()).append("<gold>Minimum spawned Mobs").newLine()
                                    .append("<gold><blue>Max: </blue>" + max).newLine()
                                    .append("<gold><blue>Min: </blue>" + min)).setGuiProperty().build(), true
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 100, this::getMax, (val) -> {
                this.setMax(val);
                gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
            }, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, max)).setDescription(
                            new DMessage(DeepDungeons.get(), gui.getTargetPlayer()).append("<gold><b>Maximus spawned Mobs</b>").newLine()
                                    .append("<gold><blue>Max: </blue>" + max).newLine()
                                    .append("<gold><blue>Min: </blue>" + min)).setGuiProperty().build(), true));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1D, 0.1D, 100D, () -> getChance() * 100, (val) -> {
                this.setChance(val / 100);
                gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
            }, () ->
                    new ItemBuilder(Material.TRIPWIRE_HOOK).setAmount((int) Math.max(1, chance * 100))
                            .setDescription(
                                    new DMessage(DeepDungeons.get(), gui.getTargetPlayer()).append("<gold><b>Chance to spawn Mobs</b>").newLine()
                                            .append("<gold><blue>Chance: </blue>" + UtilsString.formatOptional2Digit(getChance() * 100) + "%")
                            ).setGuiProperty().build(), true
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 10000, this::getLevelMin, (val) -> {
                this.setLevelMin(val);
                gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
            }, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, levelMin))
                            .setDescription(new DMessage(DeepDungeons.get(), gui.getTargetPlayer()).append("<gold>Minimum Level spawned Mobs").newLine()
                                    .append("<gold><blue>LevelMax: </blue>" + levelMax).newLine()
                                    .append("<gold><blue>LevelMin: </blue>" + levelMin)).setGuiProperty().build(), true
            ));
            gui.addButton(new NumberEditorFButton<>(
                    gui, 1, 1, 10000, this::getLevelMax, (val) -> {
                this.setLevelMax(val);
                gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
            }, () ->
                    new ItemBuilder(Material.REPEATER).setAmount(Math.max(1, levelMax)).setDescription(
                            new DMessage(DeepDungeons.get(), gui.getTargetPlayer()).append("<gold><b>Maximus Level spawned Mobs</b>").newLine()
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

        public double getChance() {
            return chance;
        }

        public void setChance(double chance) {
            if (chance > 1)
                chance = 1;
            if (chance < 0.001)
                chance = 0.001;
            this.chance = chance;
        }

        public void setEntityType(@NotNull MythicMob type) {
            this.type = type;
        }
    }

    private class MythicMobsInstance extends MonsterSpawnerInstance {

        private final MythicMob entityType;
        private final int min;
        private final int max;
        private final double chance;
        private final int levelMin;
        private final int levelMax;

        public MythicMobsInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            entityType = MythicBukkit.inst().getMobManager().getMythicMob(section.getString("mobtype", "null")).orElse(null);
            min = section.getInt("min", 1);
            max = section.getInt("max", min);
            chance = section.getDouble("chance", 1);
            levelMin = section.getInt("level_min", 1);
            levelMax = section.getInt("level_max", 1);
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        /**
         * @return a value from 0 to 1
         */
        public double getChance() {
            return chance;
        }

        @NotNull
        @Override
        public Collection<Entity> spawnMobs(@NotNull Random random, @NotNull Location location, @Nullable Player who) {
            List<Entity> entities = new ArrayList<>();
            if (entityType == null)
                return entities;
            if (Math.random() < chance) {
                int rand = new Random().nextInt() % (max - min + 1) + min;
                boolean failed = false;
                for (int i = 0; i < rand; i++) {
                    int level = new Random().nextInt() % (levelMax - levelMin + 1) + levelMin;
                    Entity entity = BukkitAdapter.adapt(entityType.spawn(BukkitAdapter.adapt(location), level).getEntity());
                    if (entity.isValid())
                        entities.add(entity);
                    else
                        failed = true;
                }
                if (failed) {
                    DeepDungeons.get().logIssue("Failed to spawn monsters at &e" + location.getWorld() + " "
                            + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ());
                    //TODO more info
                }
            }
            return entities;
        }
    }
}
