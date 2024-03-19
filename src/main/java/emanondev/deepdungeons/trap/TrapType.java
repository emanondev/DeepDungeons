package emanondev.deepdungeons.trap;

import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.ParticleUtility;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import emanondev.deepdungeons.room.RoomType.RoomInstanceBuilder;
import org.bukkit.Color;
import org.bukkit.Particle;
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
    public abstract TrapInstanceBuilder getBuilder(@NotNull RoomInstanceBuilder room);

    public abstract class TrapInstanceBuilder extends DInstance<TrapType> {
        private final CompletableFuture<TrapInstanceBuilder> completableFuture = new CompletableFuture<>();
        private final RoomInstanceBuilder roomBuilder;

        protected TrapInstanceBuilder(@NotNull RoomInstanceBuilder room) {
            super(TrapType.this);
            this.roomBuilder = room;
        }

        public final void writeTo(@NotNull YMLSection section) {
            section.set("type", getType().getId());
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        @NotNull
        public CompletableFuture<TrapInstanceBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public void abort() {
            completableFuture.completeExceptionally(new Exception("aborted"));
        }

        public void complete() {
            completableFuture.complete(this);
        }

        @NotNull
        public RoomInstanceBuilder getRoomBuilder() {
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

        protected void showWEBound(@NotNull Player player) {
            try {
                ParticleUtility.spawnParticleBoxFaces(player, roomBuilder.getTickCounter() / 6 + 6, 4, Particle.REDSTONE,
                        WorldEditUtility.getSelectionBoxExpanded(player), new Particle.DustOptions(Color.WHITE, 0.3F));
            } catch (Exception ignored) {
            }
        }
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

            @NotNull
            public World getWorld() {
                return roomHandler.getWorld();
            }

            @Contract(pure = true)
            @NotNull
            public RoomHandler getRoom() {
                return this.roomHandler;
            }

            private final RoomHandler roomHandler;

            public TrapHandler(@NotNull RoomHandler roomHandler){
                this.roomHandler = roomHandler;
            }

            public abstract void setupOffset();

            public abstract void onFirstPlayerEnter(@NotNull Player player);
        }
    }
}
