package emanondev.deepdungeons.trap;

import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Color;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class TrapType extends DRegistryElement {


    public TrapType(@NotNull String id) {
        super(id);
    }

    @NotNull
    public abstract TrapInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub);

    @NotNull
    public abstract TrapBuilder getBuilder(@NotNull RoomBuilder room);

    public abstract class TrapBuilder extends DInstance<TrapType> {
        private final CompletableFuture<TrapBuilder> completableFuture = new CompletableFuture<>();
        private final RoomBuilder roomBuilder;

        protected TrapBuilder(@NotNull RoomBuilder room) {
            super(TrapType.this);
            this.roomBuilder = room;
        }

        public final void writeTo(@NotNull YMLSection section) throws Exception {
            section.set("type", getType().getId());
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section) throws Exception;

        @NotNull
        public CompletableFuture<TrapBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public void abort() {
            completableFuture.completeExceptionally(new Exception("aborted"));
        }

        public void complete() {
            completableFuture.complete(this);
        }

        @NotNull
        public RoomBuilder getRoomBuilder() {
            return roomBuilder;
        }

        public void setupTools() {

            this.setupToolsImpl();
        }

        protected abstract void handleInteractImpl(@NotNull PlayerInteractEvent event);

        protected abstract void setupToolsImpl();

        public void handleInteract(@NotNull PlayerInteractEvent event) {
            this.handleInteractImpl(event);
        }

        @Nullable
        public Player getPlayer() {
            return roomBuilder.getPlayer();
        }

        @Nullable
        public BlockVector getRoomOffset() {
            return getRoomBuilder().getOffset();
        }

        public void timerTick(@NotNull Player player, @NotNull Color color) {
            this.tickTimerImpl(player, color);
        }

        protected abstract void tickTimerImpl(@NotNull Player player, @NotNull Color color);

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

            private final RoomHandler roomHandler;

            public TrapHandler(@NotNull RoomHandler roomHandler) {
                this.roomHandler = roomHandler;
            }

            @NotNull
            public World getWorld() {
                return roomHandler.getWorld();
            }

            @Contract(pure = true)
            @NotNull
            public RoomHandler getRoom() {
                return this.roomHandler;
            }

            public abstract void setupOffset();

            public abstract void onFirstPlayerEnter(@NotNull Player player);
        }
    }
}
