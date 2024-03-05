package emanondev.deepdungeons;

import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface ActiveBuilder {
    void setupTools();

    void timerTick();

    @NotNull CompletableFuture<? extends ActiveBuilder> getCompletableFuture();

    void write() throws Exception;

    void handleInteract(@NotNull PlayerInteractEvent event);
}
