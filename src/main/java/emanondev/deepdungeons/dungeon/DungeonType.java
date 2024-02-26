package emanondev.deepdungeons.dungeon;

import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DRInstance;
import org.jetbrains.annotations.NotNull;

public class DungeonType extends DRegistryElement {

    public DungeonType(@NotNull String id) {
        super(id);
    }

    public class DungeonInstance extends DRInstance<DungeonType> {

        public DungeonInstance(@NotNull String id) {
            super(id, DungeonType.this);
        }
    }
}
