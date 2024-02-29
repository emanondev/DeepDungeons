package emanondev.deepdungeons.room;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.room.impl.SimpleType;
import org.jetbrains.annotations.NotNull;

public class RoomTypeManager extends DRegistry<RoomType> {

    private static final RoomTypeManager instance = new RoomTypeManager();

    private RoomTypeManager() {
        super(DeepDungeons.get(), "RoomTypeManager", true);
        register(new SimpleType());
    }

    public static @NotNull RoomTypeManager getInstance() {
        return instance;
    }

}
