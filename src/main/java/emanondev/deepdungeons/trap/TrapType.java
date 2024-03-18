package emanondev.deepdungeons.trap;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
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

    @NotNull
    public abstract TrapInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub);

    @NotNull
    public abstract TrapInstanceBuilder getBuilder();

    public abstract class TrapInstanceBuilder extends DInstance<TrapType> {

        protected TrapInstanceBuilder() {
            super(TrapType.this);
        }

        @NotNull
        public ItemStack toItem() {
            return new ItemBuilder(Material.PAPER).setDescription(toItemLines()).build();
        }

        /**
         * additional info should be provided by implementation
         *
         * @return Trap info readable by TrapType
         */
        @NotNull
        protected abstract List<String> toItemLinesImpl();

        /**
         * @return a mutable list with prefilled first two lines
         */
        @NotNull
        public final List<String> toItemLines() {
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
        @NotNull
        public abstract TrapInstanceBuilder fromItemLines(@NotNull List<String> lines);
    }

    public abstract class TrapInstance extends DInstance<TrapType> {


        private final RoomInstance room;

        public TrapInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(TrapType.this);
            this.room = room;
        }

        public abstract TrapHandler createTrapHandler(@NotNull RoomHandler roomHandler);

        @NotNull
        public RoomInstance getRoomInstance() {
            return room;
        }

        public abstract class TrapHandler {

            public abstract void setupOffset();

            public abstract void onFirstPlayerEnter(@NotNull Player player);
        }
    }
}
