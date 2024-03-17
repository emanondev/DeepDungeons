package emanondev.deepdungeons.dungeon;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.dungeon.impl.RoomsGroupsSequence;
import org.jetbrains.annotations.NotNull;

public class DungeonTypeManager extends DRegistry<DungeonType> {

    private static final DungeonTypeManager instance = new DungeonTypeManager();

    private DungeonTypeManager() {
        super(DeepDungeons.get(), "DungeonTypeManager", true);
        register(new RoomsGroupsSequence());
    }

    public static @NotNull
    DungeonTypeManager getInstance() {
        return instance;
    }
}
