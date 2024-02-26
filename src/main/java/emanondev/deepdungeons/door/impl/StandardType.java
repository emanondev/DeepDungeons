package emanondev.deepdungeons.door.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.room.RoomType;
import org.jetbrains.annotations.NotNull;

public class StandardType extends DoorType {
    public StandardType() {
        super("standard");
    }

    @Override
    public @NotNull StandardInstance read(@NotNull RoomType.RoomInstance room, @NotNull YMLSection section) {
        return new StandardInstance(room, section);
    }

    @Override
    public StandardInstanceBuilder getBuilder() {
        return new StandardInstanceBuilder();
    }

    public class StandardInstanceBuilder extends DoorInstanceBuilder {

        public StandardInstanceBuilder() {
            super();
        }

    }

    public class StandardInstance extends DoorInstance {

        public StandardInstance(@NotNull RoomType.RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
        }
    }
}
