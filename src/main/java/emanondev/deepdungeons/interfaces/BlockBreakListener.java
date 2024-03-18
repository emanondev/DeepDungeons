package emanondev.deepdungeons.interfaces;

import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A class handing BlockBreakEvent on it's area
 */
public interface BlockBreakListener {

    /**
     * Handle the event
     *
     * @param event what
     */
    void onBlockBreak(@NotNull BlockBreakEvent event);
}
