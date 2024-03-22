package emanondev.deepdungeons.event;

import emanondev.deepdungeons.interfaces.ItemPopulator;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public class PopulatorGenerateLootEvent extends RoomEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ItemPopulator populator;
    private final Map<Location, Collection<ItemStack>> drops;
    private boolean cancelled;

    public PopulatorGenerateLootEvent(@NotNull RoomHandler roomHandler, @NotNull ItemPopulator populator, @NotNull Map<Location, Collection<ItemStack>> drops) {
        super(roomHandler);
        this.populator = populator;
        this.drops = Map.copyOf(drops);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public ItemPopulator getPopulator() {
        return populator;
    }

    @NotNull
    public Map<Location, Collection<ItemStack>> getDrops() {
        return drops;
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