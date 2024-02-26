package emanondev.deepdungeons.treasure;

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
import java.util.Collection;
import java.util.List;
import java.util.Random;

public abstract class TreasureType extends DRegistryElement {

    public TreasureType(@NotNull String id) {
        super(id);
    }

    public abstract @NotNull TreasureInstance read(@NotNull RoomType.RoomInstance instance, @NotNull YMLSection sub);


    public abstract @NotNull TreasureInstanceBuilder getBuilder();

    public abstract class TreasureInstanceBuilder extends DInstance<TreasureType> {

        protected TreasureInstanceBuilder() {
            super(TreasureType.this);
        }

        public @NotNull ItemStack toItem() {
            return new ItemBuilder(Material.PAPER).setDescription(toItemLines()).build();
        }

        /**
         * additional info should be provided by implementation
         *
         * @return Treasure info readable by TreasureType
         */
        protected abstract @NotNull List<String> toItemLinesImpl();

        /**
         * @return a mutable list with prefilled first two lines
         */
        public final @NotNull List<String> toItemLines() {
            ArrayList<String> list = new ArrayList<>();
            list.add(TreasureTypeManager.LINE_ONE);
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
        public abstract TreasureInstanceBuilder fromItemLines(@NotNull List<String> lines);
    }

    public abstract class TreasureInstance extends DInstance<TreasureType> {


        private final RoomType.RoomInstance room;

        public TreasureInstance(@NotNull RoomType.RoomInstance room) {
            super(TreasureType.this);
            this.room = room;
        }

        public @NotNull RoomType.RoomInstance getRoomInstance() {
            return room;
        }

        /**
         * Return a collection of not null ItemStacks, collection may be empty
         * <br><br>Implementation note: generated treasures should be consistent with given random
         *
         * @param random   seed for generation
         * @param location where the treasure is generated
         * @param who      optional who's getting the treasure
         * @return A collection of not null ItemStacks, collection may be empty
         */
        public abstract @NotNull Collection<ItemStack> getTreasure(@NotNull Random random, @NotNull Location location, @Nullable Player who);

    }
}
