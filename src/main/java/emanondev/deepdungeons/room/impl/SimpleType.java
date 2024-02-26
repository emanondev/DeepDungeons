package emanondev.deepdungeons.room.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.room.RoomType;
import org.jetbrains.annotations.NotNull;

public class SimpleType extends RoomType {
    public SimpleType() {
        super("simple");
    }

    @Override
    public RoomInstanceBuilder getBuilder() {
        return new SimpleInstanceBuilder();
    }

    @Override
    protected @NotNull SimpleInstance readImpl(@NotNull String id, @NotNull YMLSection section) {
        return new SimpleInstance(id, section);
    }

    public class SimpleInstanceBuilder extends RoomInstanceBuilder {
        public SimpleInstanceBuilder() {
            super();
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {

        }
    }

    public class SimpleInstance extends RoomInstance {
        public SimpleInstance(@NotNull String id, @NotNull YMLSection section) {
            super(id, section);
        }
    }

}
