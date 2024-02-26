package emanondev.deepdungeons.door;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.impl.StandardType;
import org.jetbrains.annotations.NotNull;

public class DoorTypeManager extends DRegistry<DoorType> {

    private static final DoorTypeManager instance = new DoorTypeManager();

    public DoorTypeManager() {
        super(DeepDungeons.get(), "RoomManager", true);
        this.register(new StandardType());
    }

    public static DoorTypeManager getInstance() {
        return instance;
    }

    public @NotNull DoorType getStandard() {
        return this.get("standard");
    }

}
