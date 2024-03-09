package emanondev.deepdungeons.area;

import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.dungeon.DungeonType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

public class AreaManager implements Listener {

    private static final AreaManager areaManager = init();

    private static AreaManager init() {
        AreaManager area = new AreaManager();
        DeepDungeons.get().registerListener(area);
        return area;
    }

    private final HashMap<World, WeakHashMap<DungeonType.DungeonInstance.DungeonHandler, BoundingBox>> usedZones = new HashMap<>();
    private final HashMap<DungeonType.DungeonInstance, List<DungeonType.DungeonInstance.DungeonHandler>> ready = new HashMap<>();
    private final HashMap<World, List<DungeonType.DungeonInstance.DungeonHandler>> started = new HashMap<>();

    public static AreaManager getInstance() {
        return areaManager;
    }

    public @NotNull Location findLocation(@Nullable World world, @NotNull BoundingBox boxArea, @NotNull DungeonType.DungeonInstance.DungeonHandler holder) {
        if (!boxArea.getMin().isZero())
            throw new IllegalArgumentException("box must be zeroed on min");
        if (world == null)
            world = getStandardWorld();
        if (!usedZones.containsKey(world))
            usedZones.put(world, new WeakHashMap<>());
        WeakHashMap<DungeonType.DungeonInstance.DungeonHandler, BoundingBox> map = usedZones.get(world);
        BoundingBox regionBox = new BoundingBox(0, 0, 0, (int) ((boxArea.getMax().getX() + 31) / 512) + 1, 1, (int) ((boxArea.getMax().getZ() + 31) / 512) + 1);
        boolean found = false;
        int i = 0;
        while (!found) {
            for (int x = 0; x < i; x++) {
                BoundingBox tmp = regionBox.clone().shift(x, 0, i);
                boolean valid = true;
                for (BoundingBox box : map.values())
                    if (box.overlaps(tmp)) {
                        valid = false;
                        break;
                    }
                if (valid) {
                    found = true;
                    regionBox = tmp;
                    break;
                }
            }
            if (!found)
                for (int z = 0; z <= i; z++) {
                    BoundingBox tmp = regionBox.clone().shift(i, 0, z);
                    boolean valid = true;
                    for (BoundingBox box : map.values())
                        if (box.overlaps(tmp)) {
                            valid = false;
                            break;
                        }
                    if (valid) {
                        found = true;
                        regionBox = tmp;
                        break;
                    }
                }
            i++;
        }
        map.put(holder, regionBox.clone());//why clone?
        int x = (int) (regionBox.getCenter().getX() * 512 - (boxArea.getWidthX() / 2));
        int y = (int) (64 - boxArea.getHeight() / 2);
        int z = (int) (regionBox.getCenter().getZ() * 512 - (boxArea.getWidthZ() / 2));
        return new Location(world, x, y, z);
    }

    @Contract(pure = true)
    public @NotNull World getStandardWorld() {
        return Bukkit.getWorlds().get(0);//TODO configurable default world
    }

    public void flagReady(@NotNull DungeonType.DungeonInstance.DungeonHandler handler) {
        this.ready.putIfAbsent(handler.getInstance(), new ArrayList<>());
        this.ready.get(handler.getInstance()).add(handler);
    }

    public void flagStart(@NotNull DungeonType.DungeonInstance.DungeonHandler handler) {
        this.ready.get(handler.getInstance()).remove(handler);
        this.started.putIfAbsent(handler.getWorld(), new ArrayList<>());
        this.started.get(handler.getWorld()).add(handler);
        //TODO generate cache?
    }

    public void flagComplete(@NotNull DungeonType.DungeonInstance.DungeonHandler handler) {
        this.started.get(handler.getWorld()).remove(handler);
    }

    public void flagPlayerJoinDungeon(@NotNull DungeonType.DungeonInstance.DungeonHandler handler, @NotNull Player player) {
        this.players.put(player, handler);
    }

    public void flagPlayerQuitDungeon(@NotNull Player player) {
        this.players.remove(player);
    }

    private final HashMap<Player, DungeonType.DungeonInstance.DungeonHandler> players = new HashMap<>();

    @Contract(pure = true)
    public @Nullable DungeonType.DungeonInstance.DungeonHandler getReady(DungeonType.DungeonInstance instance) {
        List<DungeonType.DungeonInstance.DungeonHandler> handlers = ready.get(instance);
        return handlers == null ? null : handlers.isEmpty() ? null : handlers.get(0);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockBreakEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onBlockBreak(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockPlaceEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onBlockPlace(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerTeleportEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerTeleport(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerMoveEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerMove(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerInteractEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerInteract(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerInteractEntityEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerInteractEntity(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerHarvestBlockEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerHarvestBlock(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerFishEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerFish(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerCommandSendEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerCommandSend(event);
    }

    /*
    @EventHandler(ignoreCancelled = true)
    private void event(PlayerChangedWorldEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerChangedWorld(event);
    }*/

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBucketEmptyEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerBucketEmpty(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBucketFillEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerBucketFill(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBucketEntityEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerBucketEntity(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBedLeaveEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerBedLeave(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBedEnterEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerBedEnter(event);
    }


    @EventHandler(ignoreCancelled = true)
    private void event(PlayerShearEntityEvent event) {
        DungeonType.DungeonInstance.DungeonHandler handler = players.get(event.getPlayer());
        if (handler == null)
            return;
        handler.onPlayerShearEntity(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockBurnEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getBlock().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(event.getBlock())) {
                handler.onBlockBurn(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(HangingBreakEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onHangingBreak(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(HangingPlaceEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onHangingPlace(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockExplodeEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getBlock().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(event.getBlock())) {
                handler.onBlockExplode(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PortalCreateEvent event) {
        BlockState block = event.getBlocks().get(0);
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(block)) {
                handler.onPortalCreate(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityDeathEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onEntityDeath(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityEnterBlockEvent event) {
        Block block = event.getBlock();
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(block)) {
                handler.onEntityEnterBlock(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityExplodeEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onEntityExplode(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityInteractEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onEntityInteract(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityPlaceEvent event) {
        Block block = event.getBlock();
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(block)) {
                handler.onEntityPlace(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(CreatureSpawnEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onCreatureSpawn(event);
                return;
            }
        }

    }

    @EventHandler(ignoreCancelled = true)
    private void event(SpawnerSpawnEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getSpawner().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(event.getSpawner())) {
                handler.onSpawnerSpawn(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityBreakDoorEvent event) {
        Block block = event.getBlock();
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(block)) {
                handler.onEntityBreakDoor(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityTameEvent event) {
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onEntityTame(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        List<DungeonType.DungeonInstance.DungeonHandler> list = started.get(from.getWorld());
        if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
            if (handler.isInside(from)) {
                if (to != null && handler.isInside(to))
                    handler.onEntityTeleport(event);
                else
                    handler.onEntityTeleportFrom(event);
                return;
            }
        }
        if (to != null) {
            list = started.get(to.getWorld());
            if (list != null) for (DungeonType.DungeonInstance.DungeonHandler handler : list) {
                if (handler.isInside(to)) {
                    handler.onEntityTeleportTo(event);
                    return;
                }
            }
        }
    }
}
