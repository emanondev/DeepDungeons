package emanondev.deepdungeons.interfaces;

import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public interface InteractListener {
    void onPlayerInteract(@NotNull PlayerInteractEvent event);
}
