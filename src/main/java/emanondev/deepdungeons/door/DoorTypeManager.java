package emanondev.deepdungeons.door;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.impl.StandardType;
import org.jetbrains.annotations.NotNull;

public class DoorTypeManager extends DRegistry<DoorType> {

    private static final DoorTypeManager instance = new DoorTypeManager();

    private DoorTypeManager() {
        super(DeepDungeons.get(), "DoorTypeManager", true);
        this.register(new StandardType());
    }

    public static DoorTypeManager getInstance() {
        return instance;
    }

    public @NotNull DoorType getStandard() {
        return this.get("standard");
    }

}
