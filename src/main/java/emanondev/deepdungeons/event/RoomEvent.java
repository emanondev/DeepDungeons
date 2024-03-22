package emanondev.deepdungeons.event;

import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class RoomEvent extends Event {

    private final RoomHandler roomHandler;

    public RoomEvent(@NotNull RoomHandler roomHandler) {
        this.roomHandler = roomHandler;
    }

    @NotNull
    public RoomHandler getRoomHandler() {
        return roomHandler;
    }

    @NotNull
    public DungeonHandler getDungeonHandler() {
        return roomHandler.getDungeonHandler();
    }

    @NotNull
    public RoomInstance getRoomInstance() {
        return roomHandler.getRoomInstance();
    }

    @NotNull
    public DungeonInstance getDungeonInstance() {
        return roomHandler.getDungeonHandler().getDungeonInstance();
    }
}
