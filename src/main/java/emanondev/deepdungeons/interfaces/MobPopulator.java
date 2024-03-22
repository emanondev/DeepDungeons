package emanondev.deepdungeons.interfaces;

import emanondev.deepdungeons.event.PopulatorSpawnEntitiesEvent;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public interface MobPopulator extends PopulatorType.PopulatorInstance {
    @NotNull
    default Collection<Entity> spawnMobs(@NotNull RoomHandler handler, @Nullable Player who) {
        return spawnMobs(handler, who, new Random());
    }

    @NotNull
    Collection<Entity> spawnMobs(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random);

    boolean spawnGuardians();

    @Override
    default void populate(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
        PopulatorSpawnEntitiesEvent event = new PopulatorSpawnEntitiesEvent(handler, this, spawnMobs(handler, who, random));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            event.getEntities().forEach(Entity::remove);
        else if (spawnGuardians()) {
            List<Entity> list = new ArrayList<>();
            event.getEntities().forEach(e -> {
                if (e.isValid())
                    list.add(e);
            });
            handler.addGuardians(list);
        }
    }
}
