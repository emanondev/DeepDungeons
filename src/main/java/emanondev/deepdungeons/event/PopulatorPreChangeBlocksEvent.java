package emanondev.deepdungeons.event;

import emanondev.deepdungeons.interfaces.BlockPopulator;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.block.BlockState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PopulatorPreChangeBlocksEvent extends RoomEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final BlockPopulator populator;
    private final List<BlockState> blocks;
    private boolean cancelled;

    public PopulatorPreChangeBlocksEvent(@NotNull RoomHandler roomHandler, @NotNull BlockPopulator populator, @NotNull Collection<BlockState> blocks) {
        super(roomHandler);
        this.populator = populator;
        this.blocks = new ArrayList<>(blocks);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public BlockPopulator getPopulator() {
        return populator;
    }

    /**
     * @return mutable list of block states to be changed
     */
    @NotNull
    public List<BlockState> getBlocks() {
        return blocks;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean value) {
        this.cancelled = value;
    }
}