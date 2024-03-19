package emanondev.deepdungeons.area;

import emanondev.core.UtilsWorld;
import emanondev.core.YMLConfig;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Class to manage Areas
 */
public class AreaManager implements Listener {

    private final HashMap<World, WeakHashMap<DungeonHandler, BoundingBox>> usedZones = new HashMap<>();
    private final HashMap<DungeonInstance, List<DungeonHandler>> ready = new HashMap<>();
    private final HashMap<World, List<DungeonHandler>> started = new HashMap<>();
    private final HashMap<DungeonInstance, Integer> cacheSize = new HashMap<>();
    private final HashMap<DungeonInstance, Integer> cacheDone = new HashMap<>();
    private static final AreaManager areaManager = new AreaManager();

    private AreaManager() {
        DeepDungeons.get().registerListener(this);
        YMLConfig cache = DeepDungeons.get().getConfig("dungeonCache.yml");
        for (String id : DungeonInstanceManager.getInstance().getIds()) {
            DungeonInstance dungeonInst = DungeonInstanceManager.getInstance().get(id);
            if (dungeonInst != null)
                cacheSize.put(dungeonInst, cache.loadInteger(dungeonInst.getId(), 3));
        }
        new BukkitRunnable() {

            @Override
            public void run() {
                for (DungeonInstance dung:cacheSize.keySet()){
                    if (ready.get(dung)==null||cacheDone.getOrDefault(dung,0)<cacheSize.get(dung)) {
                        dung.createHandler(null);
                        cacheDone.put(dung,cacheDone.getOrDefault(dung,0)+1);
                        break;
                    }
                }
            }
        }.runTaskTimer(DeepDungeons.get(), 10L, 80L);
    }

    /**
     * @return manager instance
     */
    @NotNull
    public static AreaManager getInstance() {
        return areaManager;
    }

    /**
     * Returns location on min corner where the boxArea could be pasted.<br>
     * Grants that area won't overlap other used areas,<br>
     * area may be freed and reused when holder is on COMPLETED phase
     *
     * @param world   which world, if null default is used
     * @param boxArea how much area is required
     * @param holder  who holds the area
     * @return location on min corner where the boxArea could be pasted
     */
    @NotNull
    public Location findLocation(@Nullable World world, @NotNull BoundingBox boxArea, @NotNull DungeonHandler holder) {
        if (!boxArea.getMin().isZero())
            throw new IllegalArgumentException("box must be zeroed on min");
        if (world == null)
            world = getStandardWorld();
        if (!usedZones.containsKey(world))
            usedZones.put(world, new WeakHashMap<>());
        WeakHashMap<DungeonHandler, BoundingBox> map = usedZones.get(world);
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

    /**
     * @return default world to use when none is specified on {@link #findLocation(World, BoundingBox, DungeonHandler) findLocation(World, BoundingBox, DungeonHandler)}
     */
    @Contract(pure = true)
    @NotNull
    public World getStandardWorld() {
        String name = DeepDungeons.get().getConfig().getString("dungeon.default_world", "DungeonsWorld");
        World world = Bukkit.getWorld(name);
        if (world != null)
            return world;
        UtilsWorld.create(name, null, null, false, 0L);//Void world
        world = Bukkit.getWorld(name);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DISABLE_RAIDS, true);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.GLOBAL_SOUND_EVENTS, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.UNIVERSAL_ANGER, true);
        return Bukkit.getWorld(name);
    }

    /**
     * Should be called by DungeonHandler when it's ready to use
     *
     * @param handler who
     */
    public void flagReady(@NotNull DungeonHandler handler) {
        this.ready.putIfAbsent(handler.getInstance(), new ArrayList<>());
        this.ready.get(handler.getInstance()).add(handler);
    }

    /**
     * Should be called by dungeonhandler when a party starts it
     *
     * @param handler who
     */
    public void flagStarted(@NotNull DungeonHandler handler) {
        this.ready.get(handler.getInstance()).remove(handler);
        this.started.putIfAbsent(handler.getWorld(), new ArrayList<>());
        this.started.get(handler.getWorld()).add(handler);
        //TODO generate cache?
        cacheDone.put(handler.getInstance(), cacheDone.getOrDefault(handler.getInstance(),0)-1);
    }

    /**
     * Should be called by party when dungeon has been completed
     *
     * @param handler who
     */
    public void flagComplete(@NotNull DungeonHandler handler) {
        handler.flagCompleted();
        this.started.get(handler.getWorld()).remove(handler);
    }

    /**
     * @param instance what kind
     * @return an usable DungeonHandler for selected instance, if any
     */
    @Contract(pure = true)
    @Nullable
    public DungeonHandler getReady(@NotNull DungeonInstance instance) {
        List<DungeonHandler> handlers = ready.get(instance);
        return handlers == null ? null : handlers.isEmpty() ? null : handlers.get(0);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockBreakEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onBlockBreak(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockPlaceEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onBlockPlace(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerTeleportEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerTeleport(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerMoveEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerMove(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerInteractEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerInteract(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerInteractEntityEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerInteractEntity(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerHarvestBlockEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerHarvestBlock(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerFishEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerFish(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerCommandSendEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerCommandSend(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBucketEmptyEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerBucketEmpty(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBucketFillEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerBucketFill(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBucketEntityEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerBucketEntity(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBedLeaveEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerBedLeave(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PlayerBedEnterEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerBedEnter(event);
    }


    @EventHandler(ignoreCancelled = true)
    private void event(PlayerShearEntityEvent event) {
        PartyManager.Party party = PartyManager.getInstance().getParty(event.getPlayer());
        if (party == null || !party.isInsideDungeon(event.getPlayer()))
            return;
        party.getDungeon().onPlayerShearEntity(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockBurnEvent event) {
        List<DungeonHandler> list = started.get(event.getBlock().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(event.getBlock())) {
                handler.onBlockBurn(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(HangingBreakEvent event) {
        List<DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onHangingBreak(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(HangingPlaceEvent event) {
        List<DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onHangingPlace(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(BlockExplodeEvent event) {
        List<DungeonHandler> list = started.get(event.getBlock().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(event.getBlock())) {
                handler.onBlockExplode(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(PortalCreateEvent event) {
        BlockState block = event.getBlocks().get(0);
        List<DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(block)) {
                handler.onPortalCreate(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityDeathEvent event) {
        List<DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onEntityDeath(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityEnterBlockEvent event) {
        Block block = event.getBlock();
        List<DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(block)) {
                handler.onEntityEnterBlock(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityExplodeEvent event) {
        List<DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onEntityExplode(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityInteractEvent event) {
        List<DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onEntityInteract(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityPlaceEvent event) {
        Block block = event.getBlock();
        List<DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(block)) {
                handler.onEntityPlace(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(CreatureSpawnEvent event) {
        List<DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.overlaps(event.getEntity())) {
                handler.onCreatureSpawn(event);
                return;
            }
        }

    }

    @EventHandler(ignoreCancelled = true)
    private void event(SpawnerSpawnEvent event) {
        List<DungeonHandler> list = started.get(event.getSpawner().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(event.getSpawner())) {
                handler.onSpawnerSpawn(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityBreakDoorEvent event) {
        Block block = event.getBlock();
        List<DungeonHandler> list = started.get(block.getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(block)) {
                handler.onEntityBreakDoor(event);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void event(EntityTameEvent event) {
        List<DungeonHandler> list = started.get(event.getEntity().getWorld());
        if (list != null) for (DungeonHandler handler : list) {
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
        List<DungeonHandler> list = started.get(from.getWorld());
        if (list != null) for (DungeonHandler handler : list) {
            if (handler.contains(from)) {
                if (to != null && handler.contains(to))
                    handler.onEntityTeleport(event);
                else
                    handler.onEntityTeleportFrom(event);
                return;
            }
        }
        if (to != null) {
            list = started.get(to.getWorld());
            if (list != null) for (DungeonHandler handler : list) {
                if (handler.contains(to)) {
                    handler.onEntityTeleportTo(event);
                    return;
                }
            }
        }
    }
}
