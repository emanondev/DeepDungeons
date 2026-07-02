package emanondev.deepdungeons.event;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class PlayerLeavingRoomEvent extends RoomEvent implements Cancellable {
    private final PlayerTeleportEvent teleport;

    public PlayerLeavingRoomEvent(@NotNull PlayerTeleportEvent event, @NotNull RoomHandler roomHandler) {
        super(roomHandler);
        this.teleport = event;
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

    @Override
    public boolean isCancelled() {
        return teleport.isCancelled();
    }

    @Override
    public void setCancelled(boolean b) {
        teleport.setCancelled(b);
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
