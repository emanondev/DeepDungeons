package emanondev.deepdungeons.interfaces;

import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

public interface BlockPlaceListener {
    void onBlockPlace(@NotNull BlockPlaceEvent event);
}
