package emanondev.deepdungeons.interfaces;

import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A class handing BlockPlaceEvent on it's area
 */
public interface BlockPlaceListener {
    /**
     * Handle the event
     *
     * @param event what
     */
    void onBlockPlace(@NotNull BlockPlaceEvent event);
}
