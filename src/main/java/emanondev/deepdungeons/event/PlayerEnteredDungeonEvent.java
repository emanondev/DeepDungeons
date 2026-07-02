package emanondev.deepdungeons.event;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PlayerEnteredDungeonEvent extends PlayerEvent {
    private final RoomHandler toRoom;

//    @NotNull
//    @Contract(pure = true,value="-> new")
//    public Location getFromLocation() {
//        return fromLocation.clone();
//    }
//
//    private final Location fromLocation;

    public PlayerEnteredDungeonEvent(@NotNull Player player, @NotNull RoomHandler toRoom) {//, @NotNull Location fromLocation) {
        super(player);
        this.toRoom = toRoom;
        //this.fromLocation = fromLocation.clone();
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
    public RoomHandler getToRoom() {
        return toRoom;
    }
}
