package emanondev.deepdungeons.treasure;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.message.DMessage;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
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

    @NotNull
    public abstract TreasureInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub);

    @NotNull
    public abstract TreasureInstanceBuilder getBuilder();

    @NotNull
    public DMessage getDescription(Player player) {
        return new DMessage(DeepDungeons.get(), player).append("<red>Description of <gold>" + getId() + "</gold> not implemented</red>");//TODO
    }

    public abstract class TreasureInstanceBuilder extends DInstance<TreasureType> {

        private Vector offset;

        protected TreasureInstanceBuilder() {
            super(TreasureType.this);
        }

        public Vector getOffset() {
            return offset;
        }

        @NotNull
        public ItemStack toItem() {
            return new ItemBuilder(Material.PAPER).setDescription(toItemLines()).build();
        }

        /**
         * additional info should be provided by implementation
         *
         * @return Treasure info readable by TreasureType
         */
        @NotNull
        protected abstract List<String> toItemLinesImpl();

        /**
         * @return a mutable list with prefilled first two lines
         */
        @NotNull
        public final List<String> toItemLines() {
            ArrayList<String> list = new ArrayList<>();
            list.add(TreasureTypeManager.LINE_ONE);
            list.add("&9Type:&6 " + getType().getId());
            list.addAll(toItemLinesImpl());
            return list;
        }

        public final void writeTo(@NotNull YMLSection section) throws Exception {
            section.set("type", getType().getId());
            if (offset == null)
                throw new IllegalArgumentException("invalid offset");
            section.set("offset", Util.toString(offset));
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        @Contract("_ -> this")
        public abstract TreasureInstanceBuilder fromItemLines(@NotNull List<String> lines);

        public void openGui(Player player) {
            craftGui(player).open(player);
        }

        protected PagedMapGui craftGui(@NotNull Player player) {
            PagedMapGui gui = new PagedMapGui(new DMessage(DeepDungeons.get()).append("&9Treasure: &6%type%",
                    "%type%", TreasureType.this.getId()), 6, player, null, DeepDungeons.get());
            craftGuiButtons(gui);
            return gui;
        }

        protected abstract void craftGuiButtons(@NotNull PagedMapGui gui);

        public void setOffset(Vector offset) {
            this.offset = offset;
        }
    }

    public abstract class TreasureInstance extends DInstance<TreasureType> {

        private final RoomInstance room;
        private final Vector offset;

        public TreasureInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(TreasureType.this);
            this.room = room;
            this.offset = Util.toVector(section.getString("offset"));
        }

        @Contract("-> new")
        @NotNull
        public Vector getOffset() {
            return offset.clone();
        }

        @NotNull
        public RoomInstance getRoomInstance() {
            return room;
        }

        /**
         * Return a collection of not null ItemStacks, collection may be empty
         * <p>Implementation note: generated treasures should be consistent with given random
         *
         * @param random   seed for generation
         * @param location where the treasure is generated
         * @param who      optional who's getting the treasure
         * @return A collection of not null ItemStacks, collection may be empty
         */
        @NotNull
        public abstract Collection<ItemStack> getTreasure(@NotNull Random random, @NotNull Location location, @Nullable Player who);

    }
}
