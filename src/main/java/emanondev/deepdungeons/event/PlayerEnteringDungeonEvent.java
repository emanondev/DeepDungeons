package emanondev.deepdungeons.event;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerEnteringDungeonEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

//    @NotNull
//    @Contract(pure = true,value="-> new")
//    public Location getToLocation() {
//        return toLocation.clone();
//    }

    //private final Location toLocation;
    private final RoomHandler toRoom;
    private boolean cancelled = false;

    public PlayerEnteringDungeonEvent(@NotNull Player player, @NotNull RoomHandler toRoom) {//, @NotNull Location toLocation*/) {
        super(player);
        this.toRoom = toRoom;
        //this.toLocation = toLocation.clone();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
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

    @NotNull
    public RoomHandler getToRoom() {
        return toRoom;
    }
}
