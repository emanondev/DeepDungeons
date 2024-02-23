package emanondev.deepdungeons.room;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class RoomHandler {

    private final RoomInstance instance;
    private final Location location;

    public RoomHandler(@NotNull RoomInstance instance, @NotNull Location location){
        this.instance = instance;
        this.location = location;
    }

    public @NotNull Location getLocation() {
        return location;
    }

    public @NotNull RoomInstance getRoomInstance() {
        return instance;
    }
}
