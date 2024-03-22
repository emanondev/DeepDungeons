package emanondev.deepdungeons.event;

import emanondev.deepdungeons.interfaces.MobPopulator;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class PopulatorSpawnEntitiesEvent extends RoomEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final List<Entity> entities;
    private final MobPopulator populator;
    private boolean cancelled;

    public PopulatorSpawnEntitiesEvent(@NotNull RoomHandler roomHandler, @NotNull MobPopulator populator, @NotNull Collection<Entity> entities) {
        super(roomHandler);
        this.populator = populator;
        this.entities = List.copyOf(entities);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public List<Entity> getEntities() {
        return entities;
    }

    @NotNull
    public MobPopulator getPopulator() {
        return populator;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Returns true if event is cancelled<p>
     * If cancelled all the entities are removed<br>
     * To remove single entities get the {@link #getEntities() list} and {@link Entity#remove() remove} unwanted entities
     *
     * @param value
     */
    @Override
    public void setCancelled(boolean value) {
        this.cancelled = value;
    }
}