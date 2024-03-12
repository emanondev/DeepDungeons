package emanondev.deepdungeons.party;

import emanondev.core.util.DRegistry;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.area.AreaManager;
import emanondev.deepdungeons.dungeon.DungeonType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PartyManager extends DRegistry<PartyManager.Party> implements Listener {
    public PartyManager() {
        super(DeepDungeons.get(), "PartyManager", true);
        getPlugin().registerListener(this);
    }

    private final HashMap<UUID, Party> parties = new HashMap<>();

    private static final PartyManager instance = new PartyManager();

    public static @NotNull PartyManager getInstance() {
        return instance;
    }

    public @NotNull Party createParty(@NotNull Player leader) {
        if (parties.containsKey(leader.getUniqueId()))
            throw new IllegalStateException();
        return new Party(leader);
    }

    public void startDungeon(@NotNull Party party, @NotNull DungeonType.DungeonInstance.DungeonHandler dungeon) {
        if (dungeon.getState() != DungeonType.DungeonInstance.DungeonHandler.State.READY)
            throw new IllegalStateException();
        if (party.getLeader() == null)
            throw new IllegalStateException();
        if (party.isExploringDungeon())
            throw new IllegalStateException();
        dungeon.start(party);
        party.start(dungeon);
    }

    public @Nullable Party getParty(Player player) {
        return parties.get(player.getUniqueId());
    }

    @EventHandler
    private void event(@NotNull PlayerQuitEvent event) {
        Party party = parties.get(event.getPlayer().getUniqueId());
        if (party == null)
            return;
        party.onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    private void event(@NotNull PlayerJoinEvent event) {
        Party party = parties.get(event.getPlayer().getUniqueId());
        if (party == null)
            return;
        party.onPlayerJoin(event.getPlayer());
    }

    public DungeonPlayer getDungeonPlayer(Player player) {
        Party party = getParty(player);
        if (party==null)
            return null;
        return party.getDungeonPlayer(player);
    }

    public class Party extends DRegistryElement {

        private final HashMap<UUID, DungeonPlayer> dungeonPlayers = new HashMap<>();
        private DungeonType.DungeonInstance.DungeonHandler dungeon;
        private final UUID leader;

        Party(@NotNull Player leader) {
            super("p" + UUID.randomUUID().toString().replace("-", ""));
            this.leader = leader.getUniqueId();
            dungeonPlayers.put(this.leader, new DungeonPlayer());
        }

        public @NotNull Collection<DungeonPlayer> getDungeonPlayers() {
            return Collections.unmodifiableCollection(dungeonPlayers.values());
        }

        public @NotNull Set<Player> getPlayers() {
            HashSet<Player> players = new HashSet<>();
            this.dungeonPlayers.keySet().forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null)
                    players.add(p);
            });
            return players;
        }

        public @NotNull Set<UUID> getPlayersUUID() {
            return Collections.unmodifiableSet(dungeonPlayers.keySet());
        }

        public boolean start(@NotNull DungeonType.DungeonInstance.DungeonHandler dungeon) {
            if (getLeader() == null)
                return false;
            if (isExploringDungeon())
                return false;
            Collection<Player> players = getPlayers();
            this.dungeon = dungeon;
            for (Player player : players) {
                DungeonPlayer dp = dungeonPlayers.get(player.getUniqueId());
                dp.setPreEnterSnapshot(player);
                dungeon.getEntrance().teleportIn(player);//TODO
            }
            //TODO check the teleport has a good end
            return true;
        }

        public boolean isExploringDungeon() {
            return dungeon != null;
        }


        public boolean addPlayer(Player player) {
            if (parties.containsKey(player.getUniqueId()))
                throw new IllegalStateException();
            dungeonPlayers.put(player.getUniqueId(), new DungeonPlayer());
            return true;
        }

        public void removePlayer(@NotNull UUID player) {
            if (!parties.containsKey(player) || !dungeonPlayers.containsKey(player))
                throw new IllegalStateException();
            if (player.equals(leader))
                throw new IllegalArgumentException();
            Player p = Bukkit.getPlayer(player);
            if (p != null && this.isInsideDungeon(p))
                dungeonPlayers.get(player).getAndDeletePreEnterSnapshot().apply(p);
            dungeonPlayers.remove(player);
            parties.remove(player);
        }

        public void removePlayer(@NotNull Player player) {
            removePlayer(player.getUniqueId());
        }

        public void disband() {
            dungeonPlayers.forEach((player, d) -> {
                        Player p = Bukkit.getPlayer(player);
                        if (p != null && this.isInsideDungeon(p))
                            d.getPreEnterSnapshot().apply(p);
                        dungeonPlayers.remove(player);
                        parties.remove(player);
                    }
            );
        }

        public boolean isInsideDungeon(@NotNull Player player) {
            return dungeonPlayers.get(player.getUniqueId()).hasPreEnterSnapshot();
        }

        public @NotNull UUID getLeaderUUID() {
            return leader;
        }

        public @Nullable Player getLeader() {
            return Bukkit.getPlayer(leader);
        }

        public @Nullable DungeonType.DungeonInstance.DungeonHandler getDungeon() {
            return dungeon;
        }

        public void flagPlayerExitDungeon(@NotNull Player player) {
            DungeonPlayer dp = dungeonPlayers.get(player.getUniqueId());
            dp.getAndDeletePreEnterSnapshot().apply(player);
            dp.clearDungeonData();
            for (DungeonPlayer dungeonPlayer : dungeonPlayers.values())
                if (dungeonPlayer.hasPreEnterSnapshot())
                    return;

            //dungeon completed
            AreaManager.getInstance().flagComplete(dungeon);
            dungeon = null;
            dungeonPlayers.values().forEach(DungeonPlayer::clearDungeonData);
        }

        private void onPlayerQuit(Player player) {
            if (!isInsideDungeon(player))
                return;
            DungeonPlayer dp = dungeonPlayers.get(player.getUniqueId());
            dp.setLogoutSnapshot(player);
            dp.getAndDeletePreEnterSnapshot().apply(player);
        }

        private void onPlayerJoin(Player player) {
            DungeonPlayer dp = dungeonPlayers.get(player.getUniqueId());
            if (dp.hasLogoutSnapshot()) {
                dp.setPreEnterSnapshot(player);
                dp.getAndDeleteLogoutSnapshot().apply(player);
            }
        }

        public DungeonPlayer getDungeonPlayer(OfflinePlayer player) {
            return getDungeonPlayer(player.getUniqueId());
        }
        public DungeonPlayer getDungeonPlayer(UUID player) {
            return dungeonPlayers.get(player);
        }
    }
}
