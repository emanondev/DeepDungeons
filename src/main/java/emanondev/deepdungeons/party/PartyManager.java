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
    private static final PartyManager instance = new PartyManager();
    private final HashMap<UUID, Party> parties = new HashMap<>();
    private final HashMap<UUID, DungeonPlayer> dungeonPlayers = new HashMap<>();

    public PartyManager() {
        super(DeepDungeons.get(), "PartyManager", true);
        getPlugin().registerListener(this);
    }

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

    public @Nullable Party getParty(OfflinePlayer player) {
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

    public @NotNull DungeonPlayer getDungeonPlayer(@NotNull OfflinePlayer player) {
        return getDungeonPlayer(player.getUniqueId());
    }

    public @NotNull DungeonPlayer getDungeonPlayer(@NotNull UUID uuid) {
        DungeonPlayer value = dungeonPlayers.get(uuid);
        if (value != null)
            return value;
        value = new DungeonPlayer();
        dungeonPlayers.put(uuid, value);
        return value;
    }

    public class Party extends DRegistryElement {

        private final HashSet<UUID> users = new HashSet<>();
        private UUID leader;
        private DungeonType.DungeonInstance.DungeonHandler dungeon;
        private boolean isPartyPublic = true;

        Party(@NotNull Player leader) {
            super("p" + UUID.randomUUID().toString().replace("-", ""));
            this.leader = leader.getUniqueId();
            users.add(this.leader);//TODO setparty
            //getDu
            parties.put(leader.getUniqueId(), this);
        }

        public boolean isPartyPublic() {
            return isPartyPublic;
        }

        public void togglePartyPublic() {
            isPartyPublic = !isPartyPublic;
        }

        public @NotNull Set<Player> getPlayers() {
            HashSet<Player> players = new HashSet<>();
            this.users.forEach(uuid -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null)
                    players.add(p);
            });
            return players;
        }

        public void invite(@Nullable Player sender, @NotNull OfflinePlayer target) {
            getDungeonPlayer(target).receiveInvite(this);

        }

        public @NotNull Set<UUID> getPlayersUUID() {
            return Collections.unmodifiableSet(users);
        }

        public boolean start(@NotNull DungeonType.DungeonInstance.DungeonHandler dungeon) {
            if (getLeader() == null)
                return false;
            if (isExploringDungeon())
                return false;
            Collection<Player> players = getPlayers();
            this.dungeon = dungeon;
            for (Player player : players) {
                DungeonPlayer dp = PartyManager.getInstance().getDungeonPlayer(player);
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
            users.add(player.getUniqueId());//TODO set party
            parties.put(player.getUniqueId(), this);
            return true;
        }

        public void removePlayer(@NotNull UUID player) {
            if (!parties.containsKey(player) || !users.contains(player))
                throw new IllegalStateException();
            if (player.equals(leader))
                throw new IllegalArgumentException();
            Player p = Bukkit.getPlayer(player);
            if (p != null && this.isInsideDungeon(p))
                PartyManager.getInstance().getDungeonPlayer(player).getAndDeletePreEnterSnapshot().apply(p);
            users.remove(player);
            parties.remove(player);
        }

        public void removePlayer(@NotNull OfflinePlayer player) {
            removePlayer(player.getUniqueId());
        }

        public void disband() {
            users.forEach((player) -> {
                        DungeonPlayer d = PartyManager.getInstance().getDungeonPlayer(player);
                        Player p = Bukkit.getPlayer(player);
                        if (p != null && this.isInsideDungeon(p))
                            d.getPreEnterSnapshot().apply(p);
                        users.remove(player);
                        parties.remove(player);
                    }
            );
            //parties.remove(this);
        }

        /**
         * true if you are <b>online</b> & inside the dungeon
         * <p>
         * N.B. if you are offline but logged out inside the dungeon you are considered outside
         *
         * @param player
         * @return
         */
        public boolean isInsideDungeon(@NotNull OfflinePlayer player) {
            return PartyManager.getInstance().getDungeonPlayer(player).hasPreEnterSnapshot();
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
            DungeonPlayer dp = PartyManager.getInstance().getDungeonPlayer(player);
            dp.getAndDeletePreEnterSnapshot().apply(player);
            dp.clearDungeonData();
            for (UUID uuid : users) {
                DungeonPlayer dungeonPlayer = PartyManager.getInstance().getDungeonPlayer(uuid);
                if (dungeonPlayer.hasPreEnterSnapshot())
                    return;
            }

            //dungeon completed
            AreaManager.getInstance().flagComplete(dungeon);
            dungeon = null;

            for (UUID uuid : users) {
                DungeonPlayer dungeonPlayer = PartyManager.getInstance().getDungeonPlayer(uuid);
                dungeonPlayer.clearDungeonData();
            }
        }

        private void onPlayerQuit(Player player) {
            if (!isInsideDungeon(player))
                return;
            DungeonPlayer dp = PartyManager.getInstance().getDungeonPlayer(player);
            dp.setLogoutSnapshot(player);
            dp.getAndDeletePreEnterSnapshot().apply(player);
        }

        private void onPlayerJoin(Player player) {
            DungeonPlayer dp = PartyManager.getInstance().getDungeonPlayer((player));
            if (dp.hasLogoutSnapshot()) {
                dp.setPreEnterSnapshot(player);
                dp.getAndDeleteLogoutSnapshot().apply(player);
            }
        }

        public void setLeader(@NotNull Player newLeader) {
            if (!this.equals(getParty(newLeader)))
                throw new IllegalArgumentException();
            leader = newLeader.getUniqueId();
        }

        /*
        public DungeonPlayer getDungeonPlayer(OfflinePlayer player) {
            return getDungeonPlayer(player.getUniqueId());
        }

        public DungeonPlayer getDungeonPlayer(UUID player) {
            return dungeonPlayers.get(player);
        }*/
    }
}
