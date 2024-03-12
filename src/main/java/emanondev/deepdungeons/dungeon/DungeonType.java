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
import emanondev.deepdungeons.area.AreaManager;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.party.PartyManager;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
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
        private final UUID playerUuid;
        private int tickCounter = 0;

        public DungeonInstanceBuilder(@NotNull String id, @NotNull Player player) {
            super(id, DungeonType.this);
            this.playerUuid = player.getUniqueId();
        }

        public @NotNull UUID getPlayerUuid() {
            return playerUuid;
        }

        public @Nullable Player getPlayer() {
            return Bukkit.getPlayer(playerUuid);
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
            if (DungeonInstanceManager.getInstance().get(getId()) != null)
                throw new IllegalArgumentException("dungeon id " + getId() + " is already used");

            YMLSection section = new YMLConfig(DeepDungeons.get(), "dungeons" + File.separator + getId());
            section.set("type", getType().getId());
            writeToImpl(section);
            DungeonInstanceManager.getInstance().readInstance(section.getFile());
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        @Override
        public void handleInteract(@NotNull PlayerInteractEvent event) {
            //int heldSlot = event.getPlayer().getInventory().getHeldItemSlot();
            handleInteractImpl(event);
        }

        protected abstract void handleInteractImpl(@NotNull PlayerInteractEvent event);
    }

    public abstract class DungeonInstance extends DRInstance<DungeonType> {

        public DungeonInstance(@NotNull String id, @NotNull YMLSection section) {
            super(id, DungeonType.this);
        }

        public abstract @NotNull DungeonHandler createHandler(@Nullable World world);

        public abstract class DungeonHandler {

            public @NotNull DungeonInstance getInstance() {
                return DungeonInstance.this;
            }

            @NotNull
            public abstract List<RoomType.RoomInstance.RoomHandler> getRooms();

            @Contract(pure = true)
            public abstract @NotNull DoorType.DoorInstance.DoorHandler getEntrance();

            @Contract(value = "-> new", pure = true)
            public abstract @NotNull Location getLocation();

            @Contract(value = "-> new", pure = true)
            public abstract @NotNull BoundingBox getBoundingBox();

            @Contract(pure = true)
            public abstract @NotNull State getState();

            public abstract @NotNull World getWorld();

            public boolean contains(@NotNull Block block) {
                return contains(block.getLocation());
            }

            public boolean contains(@NotNull BlockState block) {
                return contains(block.getLocation());
            }

            public boolean contains(@NotNull Location loc) {
                return getWorld().equals(loc.getWorld()) && contains(loc.toVector());
            }

            public abstract boolean contains(@NotNull Vector vector);

            public abstract boolean overlaps(@NotNull BoundingBox box);

            public boolean overlaps(@NotNull Entity box) {
                return overlaps(box.getBoundingBox());
            }

            public void onEntityTeleportTo(@NotNull EntityTeleportEvent event) {
            }

            public void onEntityTeleportFrom(@NotNull EntityTeleportEvent event) {
            }

            public void onEntityTeleport(@NotNull EntityTeleportEvent event) {
            }

            public void onEntityTame(@NotNull EntityTameEvent event) {
            }

            public void onEntityBreakDoor(@NotNull EntityBreakDoorEvent event) {
            }

            public void onSpawnerSpawn(@NotNull SpawnerSpawnEvent event) {
            }

            public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {
                switch (event.getSpawnReason()) {
                    case NETHER_PORTAL, TRAP, RAID, VILLAGE_DEFENSE, VILLAGE_INVASION, REINFORCEMENTS, PATROL, NATURAL -> event.setCancelled(true);
                    default -> {
                        for (RoomType.RoomInstance.RoomHandler room : getRooms())
                            if (room.contains(event.getLocation())) {
                                room.onCreatureSpawn(event);
                                return;
                            }
                        event.setCancelled(true);
                    }
                }
            }

            public void onEntityPlace(@NotNull EntityPlaceEvent event) {
            }

            public void onEntityInteract(@NotNull EntityInteractEvent event) {
            }

            public void onEntityExplode(@NotNull EntityExplodeEvent event) {
            }

            public void onEntityEnterBlock(@NotNull EntityEnterBlockEvent event) {
            }

            public void onEntityDeath(@NotNull EntityDeathEvent event) {
            }

            public void onPortalCreate(@NotNull PortalCreateEvent event) {
            }

            public void onBlockExplode(@NotNull BlockExplodeEvent event) {
            }

            public void onBlockBurn(@NotNull BlockBurnEvent event) {
            }

            public void onHangingBreak(@NotNull HangingBreakEvent event) {
            }

            public void onHangingPlace(@NotNull HangingPlaceEvent event) {
            }

            public void onPlayerShearEntity(@NotNull PlayerShearEntityEvent event) {
            }

            public void onPlayerBedEnter(@NotNull PlayerBedEnterEvent event) {
            }

            public void onPlayerBedLeave(@NotNull PlayerBedLeaveEvent event) {
            }

            public void onPlayerBucketEntity(@NotNull PlayerBucketEntityEvent event) {
            }

            public void onPlayerBucketEmpty(@NotNull PlayerBucketEmptyEvent event) {
                for (RoomType.RoomInstance.RoomHandler room : this.getRooms()) {
                    if (room.contains(event.getBlock())) {
                        room.onPlayerBucketEmpty(event);
                        return;
                    }
                }
                event.setCancelled(true);
            }

            public void onPlayerBucketFill(@NotNull PlayerBucketFillEvent event) {
                for (RoomType.RoomInstance.RoomHandler room : this.getRooms()) {
                    if (room.contains(event.getBlock())) {
                        room.onPlayerBucketFill(event);
                        return;
                    }
                }
                event.setCancelled(true);
            }

            public void onPlayerCommandSend(@NotNull PlayerCommandSendEvent event) {
            }

            public void onPlayerFish(@NotNull PlayerFishEvent event) {
            }

            public void onPlayerHarvestBlock(@NotNull PlayerHarvestBlockEvent event) {
            }

            public void onPlayerInteractEntity(@NotNull PlayerInteractEntityEvent event) {
            }

            public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
            }

            public void onPlayerMove(@NotNull PlayerMoveEvent event) {
                Location to = event.getTo();
                if (to == null) {
                    event.setCancelled(true);
                    return;
                }
                for (RoomType.RoomInstance.RoomHandler room : this.getRooms()) {
                    if (room.contains(to)) {
                        room.onPlayerMove(event);
                        return;
                    }
                }
                event.setCancelled(true);
            }

            public void onPlayerTeleport(@NotNull PlayerTeleportEvent event) {
                Location to = event.getTo();
                if (to == null) {
                    event.setCancelled(true);
                    return;
                }
                for (RoomType.RoomInstance.RoomHandler room : this.getRooms()) {
                    if (room.contains(to)) {
                        room.onPlayerTeleport(event);
                        return;
                    }
                }
                //
                event.setCancelled(true);//TODO ?
            }

            public void onBlockPlace(@NotNull BlockPlaceEvent event) {
                for (RoomType.RoomInstance.RoomHandler room : this.getRooms()) {
                    if (room.contains(event.getBlock())) {
                        room.onBlockPlace(event);
                        return;
                    }
                }
                event.setCancelled(true);
            }

            public void onBlockBreak(@NotNull BlockBreakEvent event) {
                for (RoomType.RoomInstance.RoomHandler room : this.getRooms()) {
                    if (room.contains(event.getBlock())) {
                        room.onBlockBreak(event);
                        return;
                    }
                }
                event.setCancelled(true);
            }

            public enum State {
                LOADING,
                READY,
                STARTED,
                COMPLETED
            }

            public void start(@NotNull PartyManager.Party party) {
                startImpl(party);
                if (this.getState() != State.STARTED)
                    throw new IllegalStateException("startImpl should flag this as started");
                AreaManager.getInstance().flagStarted(this);
            }

            /**
             * at the end of this call getState() should return STARTED
             *
             * @param party
             */
            protected abstract void startImpl(@NotNull PartyManager.Party party);
        }

    }
}
