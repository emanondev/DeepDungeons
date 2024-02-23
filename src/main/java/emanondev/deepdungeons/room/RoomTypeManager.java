package emanondev.deepdungeons.room;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;

public class RoomTypeManager extends DRegistry<RoomType> {

    private static RoomTypeManager instance;

    public static RoomTypeManager getInstance() {
        return instance;
    }

    public RoomTypeManager() {
        super(DeepDungeons.get(), "RooomTypeManager", true);
    }

}
