package emanondev.deepdungeons.door.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class StandardType extends DoorType {
    public StandardType() {
        super("standard");
    }

    @Override
    @NotNull
    public StandardInstance read(@NotNull RoomInstance room, @NotNull YMLSection section) {
        return new StandardInstance(room, section);
    }

    @Override
    @NotNull
    public StandardBuilder getBuilder(@NotNull RoomBuilder room) {
        return new StandardBuilder(room);
    }

    public final class StandardBuilder extends DoorBuilder {

        public StandardBuilder(@NotNull RoomBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {

        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {

        }

        @Override
        protected void setupToolsImpl() {
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {

        }

    }

    public class StandardInstance extends DoorInstance {

        public StandardInstance(@NotNull RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
        }

        @Override
        @NotNull
        public DoorHandler createDoorHandler(@NotNull RoomHandler roomHandler) {
            return new StandardHandler(roomHandler);
        }

        public class StandardHandler extends DoorHandler {

            public StandardHandler(@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

        }
    }
}
