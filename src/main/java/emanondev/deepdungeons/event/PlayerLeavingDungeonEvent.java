package emanondev.deepdungeons.event;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PlayerLeavingDungeonEvent extends PlayerEvent {
    private final RoomHandler fromRoom;
    private final Location toLocation;

    public PlayerLeavingDungeonEvent(@NotNull Player player,@NotNull RoomHandler fromRoom, @NotNull Location toLocation) {
        super(player);
        this.fromRoom = fromRoom;
        this.toLocation = toLocation.clone();
    }

    private static final HandlerList HANDLERS = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    @NotNull
    @Contract(pure = true,value="-> new")
    public Location getToLocation() {
        return toLocation.clone();
    }
    @NotNull
    public RoomHandler getFromRoom() {
        return fromRoom;
    }
}
