package emanondev.deepdungeons.paperpopulator;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.message.DMessage;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.RoomPopulator;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PaperPopulatorType extends DRegistryElement {

    public PaperPopulatorType(@NotNull String id) {
        super(id);
    }

    @NotNull
    public abstract PaperPopulatorInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub);

    @NotNull
    public abstract PaperPopulatorBuilder getBuilder();

    @NotNull
    public DMessage getDescription(Player player) {
        return new DMessage(DeepDungeons.get(), player).append("<red>Description of <gold>" + getId() + "</gold> not implemented</red>");//TODO
    }

    public abstract class PaperPopulatorBuilder extends DInstance<PaperPopulatorType> {

        private Vector offset;
        private Vector direction = BlockFace.NORTH.getDirection();

        protected PaperPopulatorBuilder() {
            super(PaperPopulatorType.this);
        }

        public abstract boolean preserveContainer();

        @NotNull
        public ItemStack toItem() {
            return new ItemBuilder(Material.PAPER).setDescription(toItemLines()).build();
        }

        /**
         * additional info should be provided by implementation
         *
         * @return MonsterSpawner info readable by MonsterSpawnerType
         */
        @NotNull
        protected abstract List<String> toItemLinesImpl();

        /**
         * @return a mutable list with prefilled first two lines
         */
        @NotNull
        public final List<String> toItemLines() {
            ArrayList<String> list = new ArrayList<>();
            list.add(PopulatorTypeManager.LINE_ONE);
            list.add("&9Type:&6 " + getType().getId());
            list.addAll(toItemLinesImpl());
            return list;
        }

        @Nullable
        public Vector getOffset() {
            return offset;
        }

        public void setOffset(Vector offset) {
            this.offset = offset;
        }

        @NotNull
        public Vector getDirection() {
            return direction;
        }

        public void setDirection(@NotNull Vector direction) {
            this.direction = direction;
        }

        public final void writeTo(@NotNull YMLSection section) throws Exception {
            if (offset == null)
                throw new IllegalArgumentException("invalid offset");
            section.set("type", getType().getId());
            section.set("offset", Util.toString(offset));
            section.set("direction", Util.toString(direction));
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        @Contract("_ -> this")
        public abstract PaperPopulatorBuilder fromItemLines(@NotNull List<String> lines);

        public void openGui(Player player) {
            craftGui(player).open(player);
        }

        protected PagedMapGui craftGui(@NotNull Player player) {
            PagedMapGui gui = new PagedMapGui(new DMessage(DeepDungeons.get()).append("&9PaperPopulator: &6%type%",
                    "%type%", PaperPopulatorType.this.getId()), 6, player, null, DeepDungeons.get());
            craftGuiButtons(gui);
            return gui;
        }

        protected abstract void craftGuiButtons(@NotNull PagedMapGui gui);

    }

    public abstract class PaperPopulatorInstance extends DInstance<PaperPopulatorType> implements RoomPopulator {


        private final RoomInstance room;
        private final Vector offset;
        private final Vector direction;

        public PaperPopulatorInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(PaperPopulatorType.this);
            this.room = room;
            this.offset = Util.toVector(section.getString("offset"));
            this.direction = Util.toVector(section.getString("direction"));
        }


        @Contract("-> new")
        @NotNull
        public Vector getOffset() {
            return offset.clone();
        }

        @Contract("-> new")
        @NotNull
        public Vector getDirection() {
            return direction.clone();
        }

        @NotNull
        public RoomInstance getRoomInstance() {
            return room;
        }

    }

}
