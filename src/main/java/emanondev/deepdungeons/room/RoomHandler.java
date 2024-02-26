package emanondev.deepdungeons.room;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class RoomHandler {

    private final RoomType.RoomInstance instance;
    private final Location location;

    public RoomHandler(@NotNull RoomType.RoomInstance instance, @NotNull Location location) {
        this.instance = instance;
        this.location = location;
        //TODO
    }

    public @NotNull Location getLocation() {
        return location;
    }

    public @NotNull RoomType.RoomInstance getRoomInstance() {
        return instance;
    }
}
