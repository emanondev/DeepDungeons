package emanondev.deepdungeons.room;

import emanondev.core.CorePlugin;
import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import org.jetbrains.annotations.NotNull;

public class RoomInstanceManager extends DRegistry<RoomInstance> {
    public RoomInstanceManager(@NotNull CorePlugin plugin, @NotNull String name, boolean doLog) {
        super(DeepDungeons.get(), "RoomInstanceManager", false);
    }
}
