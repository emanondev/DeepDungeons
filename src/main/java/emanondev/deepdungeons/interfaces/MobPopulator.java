package emanondev.deepdungeons.interfaces;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;

public interface MobPopulator extends RoomPopulator {
    @NotNull
    default Collection<Entity> spawnMobs(@NotNull RoomHandler handler, @Nullable Player who) {
        return spawnMobs(handler, who, new Random());
    }

    @NotNull
    Collection<Entity> spawnMobs(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random);

    boolean spawnGuardians();

    @Override
    default void populate(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
        Collection<Entity> entities = spawnMobs(handler, who, random);
        if (spawnGuardians())
            handler.addGuardians(entities);
    }
}
