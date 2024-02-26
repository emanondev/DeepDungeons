package emanondev.deepdungeons.dungeon;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import org.jetbrains.annotations.NotNull;

public class DungeonTypeManager extends DRegistry<DungeonType> {

    private static final DungeonTypeManager instance = new DungeonTypeManager();

    public DungeonTypeManager() {
        super(DeepDungeons.get(), "DungeonTypeManager", true);
    }

    public static @NotNull DungeonTypeManager getInstance() {
        return instance;
    }
}
