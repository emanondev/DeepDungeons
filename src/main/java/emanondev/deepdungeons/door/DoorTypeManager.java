package emanondev.deepdungeons.door;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.door.impl.StandardType;

public class DoorTypeManager extends DRegistry<DoorType> {

    private static DoorTypeManager instance;

    public static DoorTypeManager getInstance() {
        return instance;
    }

    public DoorTypeManager() {
        super(DeepDungeons.get(), "RoomManager", true);
        this.register(new StandardType());
    }

    public DoorType getStandard(){
        return this.get("standard");
    }

}
