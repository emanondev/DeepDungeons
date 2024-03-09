package emanondev.deepdungeons.room.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class SimpleType extends RoomType {
    public SimpleType() {
        super("simple");
    }

    @Override
    public @NotNull RoomInstanceBuilder getBuilder(@NotNull String id, @NotNull Player player) {
        return new SimpleInstanceBuilder(id, player);
    }

    @Override
    protected @NotNull SimpleInstance readImpl(@NotNull String id, @NotNull YMLSection section) {
        return new SimpleInstance(id, section);
    }


    public class SimpleInstanceBuilder extends RoomInstanceBuilder {
        public SimpleInstanceBuilder(@NotNull String id, @NotNull Player player) {
            super(id, player);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {

        }

        @Override
        protected void timerTickImpl() {

        }

        @Override
        public void handleInteractImpl(@NotNull PlayerInteractEvent event) {


        }

        @Override
        public void setupToolsImpl() {
            this.getCompletableFuture().complete(this);
        }
    }


    public class SimpleInstance extends RoomInstance {
        public SimpleInstance(@NotNull String id, @NotNull YMLSection section) {
            super(id, section);
        }

        @Override
        public @NotNull RoomHandler createRoomHandler(DungeonType.DungeonInstance.DungeonHandler dungeonHandler) {
            return new RoomHandler(dungeonHandler);
        }
    }

}
