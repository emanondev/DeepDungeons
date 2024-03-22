package emanondev.deepdungeons.interfaces;

import emanondev.core.YMLSection;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.populator.PopulatorPriority;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public interface PopulatorType {

    @NotNull
    default PopulatorPriority getPriority() {
        return PopulatorPriority.NORMAL;
    }

    @NotNull
    String getId();

    @NotNull
    DMessage getDescription(@NotNull Player player);

    PopulatorBuilder getBuilder(RoomBuilder roomBuilder);

    interface PopulatorBuilder {


        @Nullable
        default Player getPlayer() {
            return getRoomBuilder().getPlayer();
        }

        @Nullable
        default BlockVector getRoomOffset() {
            return getRoomBuilder().getOffset();
        }


        void writeTo(@NotNull YMLSection section) throws Exception;

        @NotNull
        CompletableFuture<PopulatorBuilder> getCompletableFuture();

        @NotNull
        RoomBuilder getRoomBuilder();

        void setupTools();

        void handleInteract(@NotNull PlayerInteractEvent event);

        void timerTick(@NotNull Player player, @NotNull Color color);
    }

    interface PopulatorInstance {

        default void populate(@NotNull RoomHandler handler, @Nullable Player who) {
            populate(handler, who, new Random());
        }

        void populate(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random);

        @NotNull
        default PopulatorPriority getPriority() {
            return PopulatorPriority.NORMAL;
        }

        @NotNull
        RoomInstance getRoomInstance();

    }
}
