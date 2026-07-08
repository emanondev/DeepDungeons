package emanondev.deepdungeons.area;

import emanondev.core.UtilsWorld;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.event.*;
import emanondev.deepdungeons.party.DungeonPlayer;
import emanondev.deepdungeons.party.Party;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Slf4j
public class Info implements Listener {

    private static final Info instance = new Info();
    private final HashMap<UUID, Party> playerParty = new HashMap<>();
    private final HashMap<Player, DungeonPlayer> dungeonPlayers = new HashMap<>();
    private final HashMap<Party, DungeonHandler> currentDungeons = new HashMap<>();
    private final HashMap<DungeonHandler, Party> currentParty = new HashMap<>();
    private final HashMap<RoomHandler, Set<Player>> roomPlayers = new HashMap<>();
    private final HashMap<Player, RoomHandler> playerRoom = new HashMap<>();
    private final HashMap<DungeonHandler, HashSet<RegionLoc>> handlerRegions = new HashMap<>();
    private final HashMap<DungeonHandler, World> handlerWorld = new HashMap<>();
    private final HashMap<World, HashMap<RegionLoc, DungeonHandler>> regionDungeon = new HashMap<>();
    private final HashSet<Player> teleporting = new HashSet<>();

    private Info() {
        DeepDungeons.get().registerListener(this);
    }

    public static Info get() {
        return instance;
    }

    @Nullable
    public Party getParty(@NotNull DungeonHandler handler) {
        return currentParty.get(handler);
    }

    @Nullable
    public DungeonHandler getDungeon(@NotNull Party party) {
        return currentDungeons.get(party);
    }

    @Nullable
    public RoomHandler getRoom(@NotNull Player player) {
        return playerRoom.get(player);
    }

    @NotNull
    public Set<Player> getPlayers(@NotNull RoomHandler room) {
        return Collections.unmodifiableSet(roomPlayers.getOrDefault(room, Collections.emptySet()));
    }

    /**
     * @return default world to use when none is specified on {@link #findAndReserveArea(DungeonHandler, BoundingBox)}
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
        if (world == null)
            throw new IllegalStateException("Unable to create/load world " + name);
        world.setGameRule(GameRule.SPAWN_MOBS, false);
        world.setGameRule(GameRule.ADVANCE_WEATHER, false);
        world.setGameRule(GameRule.RAIDS, false);
        world.setGameRule(GameRule.SPAWN_PATROLS, false);
        world.setGameRule(GameRule.SPAWN_PHANTOMS, false);
        world.setGameRule(GameRule.SPAWN_WANDERING_TRADERS, false);
        world.setGameRule(GameRule.GLOBAL_SOUND_EVENTS, false);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.UNIVERSAL_ANGER, true);
        world.setGameRule(GameRule.IMMEDIATE_RESPAWN, true);
        return world;
    }

    @NotNull
    public Location findAndReserveArea(@NotNull DungeonHandler handler, @NotNull BoundingBox area) {
        return findAndReserveArea(handler, getStandardWorld(), area);
    }

    @NotNull
    public Location findAndReserveArea(@NotNull DungeonHandler handler, @NotNull World world, @NotNull BoundingBox area) {
        if (!area.getMin().isZero())
            throw new IllegalArgumentException("box must be zeroed on min");
        if (handlerWorld.containsKey(handler))
            throw new IllegalStateException();
        HashSet<RegionLoc> where = new HashSet<>();
        Location min;
        //TODO calculate area
        //BoundingBox regionBox = new BoundingBox(0, 0, 0, )+1, 1, (((int) area.getWidthZ() + 32) <<9)+1);
        int xWidth = ((int) area.getWidthX() + 32) << 9;
        int zWidth = ((int) area.getWidthZ() + 32) << 9;
        boolean found = false;
        int i = 0;
        int xMin = 0, zMin = 0;
        HashMap<RegionLoc, DungeonHandler> map = regionDungeon.get(world);
        while (!found) {
            for (int x = 0; x < i; x++) {
                boolean valid = true;
                for (int rx = 0; rx <= xWidth; rx++)
                    for (int rz = 0; rz <= zWidth; rz++)
                        if (map.containsKey(new RegionLoc(x + rx, i + rz))) {
                            valid = false;
                            xMin = x;
                            zMin = i;
                            break;
                        }
                if (valid) {
                    found = true;
                    break;
                }
            }
            if (found)
                break;
            for (int z = 0; z <= i; z++) {
                boolean valid = true;
                for (int rx = 0; rx <= xWidth; rx++)
                    for (int rz = 0; rz <= zWidth; rz++)
                        if (map.containsKey(new RegionLoc(i + rx, z + rz))) {
                            valid = false;
                            xMin = i;
                            zMin = z;
                            break;
                        }
                if (valid) {
                    found = true;
                    break;
                }
            }
            i++;
        }
        for (int rx = 0; rx < xWidth; rx++)
            for (int rz = 0; rz < zWidth; rz++)
                where.add(new RegionLoc(xMin + rx, zMin + rz));


        if (where.isEmpty())
            throw new IllegalStateException();

        //DONE now fix data
        regionDungeon.putIfAbsent(world, new HashMap<>());
        HashMap<RegionLoc, DungeonHandler> regionsHandler = regionDungeon.get(world);
        handlerRegions.put(handler, where);
        handlerWorld.put(handler, world);
        for (RegionLoc region : where) {
            regionsHandler.put(region, handler);
        }


        //TODO remove entities/clear chunks

        min = new Location(world, (xMin + xWidth / 2D) * 512 + 256 - area.getWidthX() / 2, 64 + area.getHeight() / 2,
                (zMin + zWidth / 2D) * 512 + 256 - area.getWidthZ() / 2);
//TODO feedback to console
        return min;
    }

    public void clearReservedArea(@NotNull DungeonHandler handler) {
        HashSet<RegionLoc> regions = handlerRegions.remove(handler);
        if (regions == null)
            return;
        World world = handlerWorld.remove(handler);
        HashMap<RegionLoc, DungeonHandler> map = regionDungeon.get(world);
        regions.forEach(map::remove);

        //TODO remove entities/clear chunks?
    }

    @Nullable
    public DungeonHandler getDungeon(@NotNull Location loc) {
        HashMap<RegionLoc, DungeonHandler> map = regionDungeon.get(loc.getWorld());
        if (map == null)
            return null;
        return map.get(locToRegion(loc));
    }

    @Nullable
    public DungeonHandler getDungeon(@NotNull Player player) {
        RoomHandler room = getRoom(player);
        return room == null ? null : room.getDungeonHandler();
    }

    public boolean moveToRoom(@NotNull Player player, @NotNull Location to, @Nullable RoomHandler toRoom) {
        Party party = getParty(player);
        if (party == null && toRoom != null) {
            log.error("No party no dungeon", new Exception());
            return false;
        }
        RoomHandler fromRoom = getRoom(player);
        if (Objects.equals(fromRoom, toRoom)) {
            log.error("Wrong use of moveToRoom", new Exception());
            return false;
        }
        if (toRoom != null && !toRoom.contains(to)) {
            log.error("target location doesn't match room location", new Exception());
            return false;
        }
        if (toRoom == null && getDungeon(to) != null) {
            log.error("target location has no room but has dungeon", new Exception()); //TODO quit may take you to another dungeon
            return false;
        }
        if (toRoom != null)
            switch (toRoom.getDungeonHandler().getState()) {
                case LOADING, COMPLETED -> {
                    log.error("Invalid dungeon state", new Exception());
                    return false;
                }
            }
        if (fromRoom != null && toRoom != null) {
            PlayerChangingRoomEvent event = new PlayerChangingRoomEvent(player, fromRoom, toRoom, to);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return false;
            Location from = player.getLocation();
            teleporting.add(player);
            if (!player.teleport(to)) {
                teleporting.remove(player);
                return false;
            }
            teleporting.remove(player);

            roomPlayers.get(fromRoom).remove(player);
            playerRoom.put(player, toRoom);

            PlayerChangedRoomEvent event2 = new PlayerChangedRoomEvent(player, fromRoom, toRoom, from);
            Bukkit.getPluginManager().callEvent(event2);
            return true;
        }
        if (fromRoom != null) {
            PlayerLeavingDungeonEvent event = new PlayerLeavingDungeonEvent(player, fromRoom, to);
            Bukkit.getPluginManager().callEvent(event);
            Location from = player.getLocation();
            //if (event.isCancelled())
            //    return false;
            teleporting.add(player);
            if (!player.teleport(to)) {
                teleporting.remove(player);
                return false;
            }
            teleporting.remove(player);

            roomPlayers.get(fromRoom).remove(player);
            playerRoom.remove(player);

            PlayerLeftDungeonEvent event2 = new PlayerLeftDungeonEvent(player, fromRoom, from);
            Bukkit.getPluginManager().callEvent(event2);
            return true;
        }
        PlayerEnteringDungeonEvent event = new PlayerEnteringDungeonEvent(player, toRoom /*, to*/);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;
        Location from = player.getLocation();
        teleporting.add(player);
        if (!player.teleport(to)) {
            teleporting.remove(player);
            return false;
        }
        teleporting.remove(player);

        playerRoom.put(player, toRoom);

        PlayerEnteredDungeonEvent event2 = new PlayerEnteredDungeonEvent(player, toRoom /*, from*/);
        Bukkit.getPluginManager().callEvent(event2);

        return true;
    }

    @NotNull
    private RegionLoc locToRegion(@NotNull Location loc) {
        return new RegionLoc(loc.getBlockX() >> 9, loc.getBlockY() >> 9);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void playerMoving(PlayerMoveEvent event) {
        RoomHandler room = getRoom(event.getPlayer());
        if (room == null)
            return;
        if (!room.contains(event.getPlayer().getLocation())) {
            //TODO cannot move outside, is falling? kill it
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void playerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) {
            DeepDungeons.get().logInfo(event.getPlayer().getName() + " teleported to null");
            return; //TODO(?)
        }
        switch (event.getCause()) {
            case ENDER_PEARL, UNKNOWN, CHORUS_FRUIT, DISMOUNT, EXIT_BED -> {
                if (event.isCancelled())
                    return;
                RoomHandler room = getRoom(event.getPlayer());
                if (room == null)
                    return;
                if (!room.contains(event.getTo())) {
                    DeepDungeons.get().logInfo(event.getPlayer().getName() + " teleport for " + event.getCause() + " was cancelled (getting outside room)");
                    event.setCancelled(true);
                }
            }
            case COMMAND, PLUGIN -> {
                if (teleporting.contains(event.getPlayer())) {
                    if (event.isCancelled()) {
                        DeepDungeons.get().logInfo(event.getPlayer().getName() + " teleport for " + event.getCause() + " cancellation was disabled");
                        event.setCancelled(false);
                    }
                    return;
                }
                DungeonHandler playerDungeon = getDungeon(event.getPlayer());
                DungeonHandler locationDungeon = getDungeon(event.getTo());
                if (playerDungeon == null && locationDungeon == null)
                    return; //not related to dungeons
                if (playerDungeon == null) {
                    if (event.getPlayer().hasPermission(Perms.BYPASS_LOCATION_RESTRICTIONS))
                        return;
                    event.setCancelled(true);
                    DeepDungeons.get().logInfo(event.getPlayer().getName() + " teleport for " + event.getCause() + " was cancelled (not allowed to enter dungeon this way)");
                    return;
                }
                if (locationDungeon == null) {
                    //is exiting dungeon
                    event.setCancelled(true);
                    DeepDungeons.get().logInfo(event.getPlayer().getName() + " teleport for " + event.getCause() + " was cancelled (not allowed to exit dungeon this way?)");
                    return;
                }
                //if (!Objects.equals(playerDungeon, locationDungeon)) {
                DeepDungeons.get().logInfo(event.getPlayer().getName() + " teleport for " + event.getCause() + " was cancelled (not allowed to teleport inside dungeon this way?)");
                event.setCancelled(true);//might be
                //    return;
                //}

            }
            case NETHER_PORTAL, END_GATEWAY, END_PORTAL -> {
                if (event.isCancelled())
                    return;
                RoomHandler room = getRoom(event.getPlayer());
                if (room != null) {
                    DeepDungeons.get().logInfo(event.getPlayer().getName() + " teleport for " + event.getCause() + " was cancelled (portal inside dungeon)");
                    event.setCancelled(true);
                }
            }
            case SPECTATE -> {
                //ignored
            }
        }
    }

    private Party getParty(Player player) {
        return playerParty.get(player.getUniqueId());
    }

    private record RegionLoc(int x, int z) {
    }


}
