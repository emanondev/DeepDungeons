package emanondev.deepdungeons.dungeon;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import org.jetbrains.annotations.NotNull;

public class DungeonInstanceManager extends DRegistry<DungeonType.DungeonInstance> {

    private static final DungeonInstanceManager instance = new DungeonInstanceManager();

    private DungeonInstanceManager() {
        super(DeepDungeons.get(), "DungeonInstanceManager", false);
    }

    public static @NotNull DungeonInstanceManager getInstance() {
        return instance;
    }
}
