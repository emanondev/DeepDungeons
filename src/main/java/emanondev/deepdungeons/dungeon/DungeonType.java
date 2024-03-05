package emanondev.deepdungeons.dungeon;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLConfig;
import emanondev.core.YMLSection;
import emanondev.core.gui.Gui;
import emanondev.core.message.DMessage;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.ActiveBuilder;
import emanondev.deepdungeons.DRInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.BuilderMode;
import emanondev.deepdungeons.room.RoomInstanceManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class DungeonType extends DRegistryElement {

    public DungeonType(@NotNull String id) {
        super(id);
    }


    public final @NotNull DungeonType.DungeonInstance read(@NotNull String id, @NotNull YMLSection section) {
        return readImpl(id, section);
    }

    public abstract @NotNull DungeonType.DungeonInstanceBuilder getBuilder(@NotNull String id, @NotNull Player player);

    protected abstract @NotNull DungeonType.DungeonInstance readImpl(@NotNull String id, @NotNull YMLSection section);

    public abstract class DungeonInstanceBuilder extends DRInstance<DungeonType> implements ActiveBuilder {
        private final CompletableFuture<DungeonType.DungeonInstanceBuilder> completableFuture = new CompletableFuture<>();
        private int tickCounter = 0;

        public @NotNull UUID getPlayerUuid() {
            return playerUuid;
        }
        public @Nullable Player getPlayer() {
            return Bukkit.getPlayer(playerUuid);
        }

        private final UUID playerUuid;

        public DungeonInstanceBuilder(@NotNull String id, @NotNull Player player) {
            super(id, DungeonType.this);
            this.playerUuid = player.getUniqueId();
        }


        @Override
        public void setupTools() {
            Player player = getPlayer();
            if (player == null || !player.isValid())
                return;
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Gui)
                return;
            Inventory inv = player.getInventory();
            for (int i = 0; i < 8; i++) //clear
                inv.setItem(i, null);
            inv.setItem(8, new ItemBuilder(Material.BARRIER).setDescription(new DMessage(DeepDungeons.get(), player)
                    .append("Click to exit/abort building")).build());//TODO configurable

            setupToolsImpl();
        }

        protected abstract void setupToolsImpl();

        public int getTickCounter() {
            return tickCounter;
        }

        public void timerTick() {
            tickCounter++;
            timerTickImpl();
        }

        private void timerTickImpl() {
        }

        public @NotNull CompletableFuture<DungeonType.DungeonInstanceBuilder> getCompletableFuture() {
            return completableFuture;
        }

        @Override
        public void write() throws Exception {
            if (!getCompletableFuture().isDone() || getCompletableFuture().isCompletedExceptionally())
                throw new IllegalArgumentException("cannot build a builder not correctly completed");
            if (RoomInstanceManager.getInstance().get(getId()) != null)
                throw new IllegalArgumentException("room id " + getId() + " is already used");

            YMLSection section = new YMLConfig(DeepDungeons.get(), "rooms" + File.separator + getId());
            section.set("type", getType().getId());
            writeToImpl(section);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        @Override
        public void handleInteract(@NotNull PlayerInteractEvent event) {
            int heldSlot = event.getPlayer().getInventory().getHeldItemSlot();

            if (heldSlot == 8) {
                BuilderMode.getInstance().exitBuilderMode(event.getPlayer());
                return;
            }

            handleInteractImpl(event);
        }

        protected abstract void handleInteractImpl(@NotNull PlayerInteractEvent event);
    }

    public class DungeonInstance extends DRInstance<DungeonType> {

        public DungeonInstance(@NotNull String id,@NotNull YMLSection section) {
            super(id, DungeonType.this);
        }

        public class DungeonHandler {

        }

    }
}
