package emanondev.deepdungeons;

import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Class to interact with {@link BuilderMode} and configure things while interacting with the world
 */
public interface ActiveBuilder {

    /**
     * Handle setting the player hotbar
     */
    void setupTools();

    /**
     * Display effects based on time ticks, like particles indicating current area
     */
    void timerTick();

    /**
     * Result will complete when this has completed the configuration, complete with exception if configuration was aborted
     *
     * @return Completable future of this
     */
    @NotNull
    CompletableFuture<? extends ActiveBuilder> getCompletableFuture();

    /**
     * Save all the data of the builder when this has completed the configuration
     *
     * @throws Exception if any issue arise
     */
    void write() throws Exception;

    /**
     * Handle player interaction with hotbar
     *
     * @param event what caused the interaction
     */
    void handleInteract(@NotNull PlayerInteractEvent event);

    /**
     * @return unique id for this
     */
    @NotNull String getId();
}
