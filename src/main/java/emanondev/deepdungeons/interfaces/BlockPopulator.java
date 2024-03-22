package emanondev.deepdungeons.interfaces;

import emanondev.deepdungeons.event.PopulatorPreChangeBlocksEvent;
import emanondev.deepdungeons.populator.PopulatorPriority;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;

public interface BlockPopulator extends PopulatorType.PopulatorInstance {

    default void populate(@NotNull RoomHandler handler, @Nullable Player who) {
        populate(handler, who, new Random());
    }

    default void populate(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
        PopulatorPreChangeBlocksEvent event = new PopulatorPreChangeBlocksEvent(handler, this, getChangingBlocks(handler, who, random));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        event.getBlocks().forEach(b -> {
            if (b != null) b.update(true, false);
        });
    }

    @NotNull
    default PopulatorPriority getPriority() {
        return PopulatorPriority.LOW;
    }

    @NotNull
    default Collection<BlockState> getChangingBlocks(@NotNull RoomHandler handler, @Nullable Player who) {
        return getChangingBlocks(handler, who, new Random());
    }

    @NotNull
    Collection<BlockState> getChangingBlocks(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random);


}
