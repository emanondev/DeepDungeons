package emanondev.deepdungeons.spawner;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class MonsterSpawnerType extends DRegistryElement {

    public MonsterSpawnerType(@NotNull String id) {
        super(id);
    }

    public abstract @NotNull MonsterSpawnerType.MonsterSpawnerInstance read(@NotNull RoomType.RoomInstance instance, @NotNull YMLSection sub);


    public abstract @NotNull MonsterSpawnerType.MonsterSpawnerInstanceBuilder getBuilder();

    public abstract class MonsterSpawnerInstanceBuilder extends DInstance<MonsterSpawnerType> {

        protected MonsterSpawnerInstanceBuilder() {
            super(MonsterSpawnerType.this);
        }

        public @NotNull ItemStack toItem() {
            return new ItemBuilder(Material.PAPER).setDescription(toItemLines()).build();
        }

        /**
         * additional info should be provided by implementation
         *
         * @return MonsterSpawner info readable by MonsterSpawnerType
         */
        protected abstract @NotNull List<String> toItemLinesImpl();

        /**
         * @return a mutable list with prefilled first two lines
         */
        public final @NotNull List<String> toItemLines() {
            ArrayList<String> list = new ArrayList<>();
            list.add(MonsterSpawnerTypeManager.LINE_ONE);
            list.add("&9Type:&6 " + getType().getId());
            list.addAll(toItemLinesImpl());
            return list;
        }

        public final void writeTo(@NotNull YMLSection section) {
            section.set("type", getType().getId());
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        @Contract("_ -> this")
        public abstract MonsterSpawnerType.MonsterSpawnerInstanceBuilder fromItemLines(@NotNull List<String> lines);
    }

    public abstract class MonsterSpawnerInstance extends DInstance<MonsterSpawnerType> {


        private final RoomType.RoomInstance room;

        public MonsterSpawnerInstance(@NotNull RoomType.RoomInstance room) {
            super(MonsterSpawnerType.this);
            this.room = room;
        }

        public @NotNull RoomType.RoomInstance getRoomInstance() {
            return room;
        }

        public abstract void spawnMobs(@NotNull Random random, @NotNull Location location, @Nullable Player who);

    }

}
