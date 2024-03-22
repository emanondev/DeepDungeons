package emanondev.deepdungeons.event;

import emanondev.deepdungeons.interfaces.PopulatorType.PopulatorInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PrePopulateEvent extends RoomEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ArrayList<PopulatorInstance> populators;
    private boolean cancelled;

    public PrePopulateEvent(@NotNull RoomHandler roomHandler, @NotNull Collection<PopulatorInstance> populators) {
        super(roomHandler);
        this.populators = new ArrayList<>(populators);
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
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

    @Override
    public void setCancelled(boolean value) {
        this.cancelled = value;
    }

    /**
     * Returns an immutable view to the list of populators to apply to the room<p>
     * Populators returned by this method are already filtered by their {@link PopulatorInstance#getChance() chance} value<br>
     * List order doesn't match populate calls done to populate the room<br>
     * To add and remove populators use {@link #addPopulator(PopulatorInstance) addPopulator()} and {@link #removePopulator(PopulatorInstance) removePopulator()}
     *
     * @return an immutable view to the list of populators to apply to the room
     */
    @NotNull
    public List<PopulatorInstance> getPopulators() {
        return Collections.unmodifiableList(populators);
    }

    /**
     * <p>
     * You can add the same populator multiple times
     *
     * @param populator
     * @throws IllegalArgumentException if populator roominstance doesn't match event roominstance
     */
    public void removePopulator(@NotNull PopulatorInstance populator) {
        populators.remove(populator);
    }

    /**
     * <p>
     * You can add the same populator multiple times
     *
     * @param populator
     * @throws IllegalArgumentException if populator roominstance doesn't match event roominstance
     */
    public void addPopulator(@NotNull PopulatorInstance populator) {
        if (!populator.getRoomInstance().equals(getRoomInstance()))
            throw new IllegalArgumentException("Unmatching Room");
        populators.add(populator);
    }
}
