package emanondev.deepdungeons.room;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import org.jetbrains.annotations.NotNull;

public class RoomInstanceManager extends DRegistry<RoomType.RoomInstance> {
    private static final RoomInstanceManager instance = new RoomInstanceManager();

    private RoomInstanceManager() {
        super(DeepDungeons.get(), "RoomInstanceManager", false);
    }

    public static @NotNull RoomInstanceManager getInstance() {
        return instance;
    }
}
