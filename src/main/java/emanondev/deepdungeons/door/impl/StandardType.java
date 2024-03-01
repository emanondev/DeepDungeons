package emanondev.deepdungeons.door.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class StandardType extends DoorType {
    public StandardType() {
        super("standard");
    }

    @Override
    public @NotNull StandardInstance read(@NotNull RoomType.RoomInstance room, @NotNull YMLSection section) {
        return new StandardInstance(room, section);
    }

    @Override
    public @NotNull StandardInstanceBuilder getBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
        return new StandardInstanceBuilder(room);
    }

    public class StandardInstanceBuilder extends DoorInstanceBuilder {

        public StandardInstanceBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
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
        protected void tickTimerImpl(@NotNull Player player) {

        }

    }

    public class StandardInstance extends DoorInstance {

        public StandardInstance(@NotNull RoomType.RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
        }

        @Override
        public @NotNull DoorHandler getHandler() {
            return new StandardHandler();
        }

        public class StandardHandler extends DoorHandler {

        }
    }
}
