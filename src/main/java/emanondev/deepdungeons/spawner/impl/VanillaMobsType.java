package emanondev.deepdungeons.spawner.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.gui.ResearchFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.spawner.MonsterSpawnerType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class VanillaMobsType extends MonsterSpawnerType {

    public VanillaMobsType() {
        super("vanillamobs");
    }

    @Override
    public @NotNull VanillaMobsInstance read(RoomType.@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new VanillaMobsInstance(room, sub);
    }

    @Override
    public @NotNull VanillaMobsInstanceBuilder getBuilder() {
        return new VanillaMobsInstanceBuilder();
    }

    private class VanillaMobsInstanceBuilder extends MonsterSpawnerInstanceBuilder {

        private EntityType type = EntityType.ZOMBIE;
        private int min = 1;
        private int max = 1;
        private double chance = 1;

        @Override
        protected @NotNull List<String> toItemLinesImpl() {
            List<String> list = new ArrayList<>();
            list.add("&9MobType:&6 " + type.name());
            list.add("&9Min:&6 " + min);
            list.add("&9Max:&6 " + max);
            list.add("&9Chance:&6 " + UtilsString.formatOptional2Digit(chance * 100));
            return list;
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("mobtype", type.name());
            section.set("min", min);
            section.set("max", max);
            section.set("chance", chance);
        }

        @Override
        public MonsterSpawnerInstanceBuilder fromItemLines(@NotNull List<String> lines) {
            type = EntityType.valueOf(lines.get(1).split(" ")[1]);
            min = Integer.parseInt(lines.get(2).split(" ")[1]);
            max = Integer.parseInt(lines.get(3).split(" ")[1]);
            chance = Double.parseDouble(lines.get(4).split(" ")[1]) / 100;
            return this;
        }

        @Override
        protected void craftGuiButtons(@NotNull PagedMapGui gui) {
            gui.addButton(new ResearchFButton<>(gui, () -> new ItemBuilder(Material.SPAWNER).setDescription(
                    new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                            .append("<!i><gold><b>EntityType</b>").newLine()
                            .append("<gold><blue>Type:</blue> " + (type.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ?
                                    type.getKey().toString().substring(10) : type.getKey().toString()) )).setGuiProperty().build(),
                    (String text, EntityType type) -> {
                        String[] split = text.split(" ");
                        for (String s : split) {
                            if (!(type.name().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))
                                    || type.getKey().toString().contains(s.toLowerCase(Locale.ENGLISH))))
                                return false;
                        }
                        return true;
                    },
                    (InventoryClickEvent event, EntityType type) -> {
                        setEntityType(type);
                        gui.open(gui.getTargetPlayer());
                        gui.getTargetPlayer().getInventory().setItemInMainHand(this.toItem());
                        return false;
                    },
                    (EntityType type) -> new ItemBuilder(Material.CHEST).setDescription(
                            new DMessage(DeepDungeons.get(), gui.getTargetPlayer())
                                    .append("<!i><gold><b>" + type.name() + "</b>").newLine()
                                    .append("<gold><blue>Type:</blue> " + (type.getKey().getNamespace().equals(NamespacedKey.MINECRAFT) ?
                                            type.getKey().toString().substring(10) : type.getKey().toString()))).setGuiProperty().build(),
                    () -> {
                        ArrayList<EntityType> list = new ArrayList<>(Arrays.asList(EntityType.values()));
                        list.removeIf((type) -> !type.isSpawnable()||!type.isAlive());
                        list.sort(Comparator.comparing(Enum::name));
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

        @NotNull
        public EntityType getEntityType() {
            return type;
        }

        public void setEntityType(@NotNull EntityType type) {
            this.type = type;
        }
    }

    private class VanillaMobsInstance extends MonsterSpawnerInstance {

        private final EntityType entityType;
        private final int min;
        private final int max;
        private final double chance;

        public VanillaMobsInstance(RoomType.RoomInstance room, @NotNull YMLSection section) {
            super(room,section);
            entityType = section.getEntityType("mobtype", EntityType.ZOMBIE);
            min = section.getInt("min");
            max = section.getInt("max");
            chance = section.getDouble("chance");
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

        @Override
        public void spawnMobs(@NotNull Random random, @NotNull Location location, @Nullable Player who) {
            //TODO register mobs for handler
        }

        public @NotNull EntityType getEntityType() {
            return entityType;
        }
    }
}
