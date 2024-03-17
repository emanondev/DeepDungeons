package emanondev.deepdungeons.interfaces;

import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public interface MoveListener {

    void onPlayerMove(@NotNull PlayerMoveEvent event);
}
