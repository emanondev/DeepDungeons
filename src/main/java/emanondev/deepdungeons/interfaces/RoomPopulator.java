package emanondev.deepdungeons.interfaces;

import emanondev.deepdungeons.paperpopulator.PopulatorPriority;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public interface RoomPopulator {

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

    @Contract("-> new")
    @NotNull
    Vector getOffset();
}
