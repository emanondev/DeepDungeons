package emanondev.deepdungeons.event;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PlayerChangedRoomEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final RoomHandler fromRoom;
    private final RoomHandler toRoom;
    private final Location fromLocation;

    public PlayerChangedRoomEvent(@NotNull Player player, @NotNull RoomHandler fromRoom, @NotNull RoomHandler toRoom, @NotNull Location fromLocation) {
        super(player);
        this.fromRoom = fromRoom;
        this.toRoom = toRoom;
        this.fromLocation = fromLocation.clone();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
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
    public Location getFromLocation() {
        return fromLocation.clone();
    }
}
