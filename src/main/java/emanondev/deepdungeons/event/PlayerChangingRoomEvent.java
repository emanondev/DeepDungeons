package emanondev.deepdungeons.event;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PlayerChangingRoomEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final RoomHandler fromRoom;
    private final RoomHandler toRoom;
    private final Location toLocation;
    private boolean cancelled = false;

    public PlayerChangingRoomEvent(@NotNull Player player, @NotNull RoomHandler fromRoom, @NotNull RoomHandler toRoom, @NotNull Location toLocation) {
        super(player);
        this.fromRoom = fromRoom;
        this.toRoom = toRoom;
        this.toLocation = toLocation.clone();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public RoomHandler getFromRoom() {
        return fromRoom;
    }

    @NotNull
    public RoomHandler getToRoom() {
        return toRoom;
    }

    @NotNull
    @Contract(pure = true, value = "-> new")
    public Location getToLocation() {
        return toLocation.clone();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean value) {
        this.cancelled = value;
    }

}
