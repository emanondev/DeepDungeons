package emanondev.deepdungeons.trap;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class TrapType extends DRegistryElement {


    public TrapType(@NotNull String id) {
        super(id);
    }

    public abstract @NotNull TrapType.TrapInstance read(@NotNull RoomType.RoomInstance instance, @NotNull YMLSection sub);


    public abstract @NotNull TrapType.TrapInstanceBuilder getBuilder();

    public abstract class TrapInstanceBuilder extends DInstance<TrapType> {

        protected TrapInstanceBuilder() {
            super(TrapType.this);
        }

        public @NotNull ItemStack toItem() {
            return new ItemBuilder(Material.PAPER).setDescription(toItemLines()).build();
        }

        /**
         * additional info should be provided by implementation
         *
         * @return Trap info readable by TrapType
         */
        protected abstract @NotNull List<String> toItemLinesImpl();

        /**
         * @return a mutable list with prefilled first two lines
         */
        public final @NotNull List<String> toItemLines() {
            ArrayList<String> list = new ArrayList<>();
            list.add(TrapTypeManager.LINE_ONE);
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
        public abstract @NotNull TrapType.TrapInstanceBuilder fromItemLines(@NotNull List<String> lines);
    }

    public abstract class TrapInstance extends DInstance<TrapType> {


        private final RoomType.RoomInstance room;

        public TrapInstance(@NotNull RoomType.RoomInstance room, @NotNull YMLSection section) {
            super(TrapType.this);
            this.room = room;
        }

        public abstract TrapHandler createTrapHandler(@NotNull RoomType.RoomInstance.RoomHandler roomHandler);

        public @NotNull RoomType.RoomInstance getRoomInstance() {
            return room;
        }

        public abstract class TrapHandler {

            public abstract void setupOffset();

            public abstract void onFirstPlayerEnter(@NotNull Player player);
        }
    }
}
