package emanondev.deepdungeons.event;

import emanondev.deepdungeons.room.RoomType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class PlayerLeftRoomEvent extends RoomEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final PlayerTeleportEvent teleport;

    public PlayerLeftRoomEvent(@NotNull PlayerTeleportEvent event, @NotNull RoomType.RoomInstance.RoomHandler roomHandler) {
        super(roomHandler);
        this.teleport = event;
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
    public Player getPlayer() {
        return teleport.getPlayer();
    }

    @NotNull
    public Location getLocation() {
        return teleport.getFrom();
    }
}
