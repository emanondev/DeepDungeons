package emanondev.deepdungeons.spawner.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.spawner.MonsterSpawnerType;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
            list.add("MobType: " + type.name());
            list.add("Min: " + min);
            list.add("Max: " + max);
            list.add("Chance: " + chance * 100);
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

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public double getChance() {
            return chance;
        }

        public void setChance(double chance) {
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
            super(room);
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

        public EntityType getEntityType() {
            return entityType;
        }
    }
}
