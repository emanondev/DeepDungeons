package emanondev.deepdungeons.interfaces;

import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

public interface InteractEntityListener {
    void onPlayerEntityInteract(@NotNull PlayerInteractEntityEvent event);
}
