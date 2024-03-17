package emanondev.deepdungeons.interfaces;

import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public interface BlockBreakListener {
    void onBlockBreak(@NotNull BlockBreakEvent event);
}
