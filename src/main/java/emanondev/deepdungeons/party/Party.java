package emanondev.deepdungeons.party;


import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.area.AreaManager;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.event.PlayerEnteredDungeonEvent;
import emanondev.deepdungeons.event.PlayerEnteringDungeonEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Party extends DRegistryElement {

    private static final HashMap<UUID, Party> parties = new HashMap<>();
    private final HashSet<UUID> users = new HashSet<>();
    private UUID leader;
    private DungeonHandler dungeon;
    @Getter
    private boolean partyPublic = true;

    /**
     * @param leader
     * @see PartyManager#createParty(Player)
     */
    @ApiStatus.Internal
    Party(@NotNull Player leader) {
        super("p" + UUID.randomUUID().toString().replace("-", ""));
        this.leader = leader.getUniqueId();
        users.add(this.leader);
        parties.put(leader.getUniqueId(), this);
    }

    @Nullable
    static Party getParty(@NotNull OfflinePlayer player) {
        return getParty(player.getUniqueId());
    }

    @Nullable
    static Party getParty(@NotNull UUID player) {
        return parties.get(player);
    }

    public void togglePartyPublic() {
        partyPublic = !partyPublic;
    }

    @NotNull
    public Set<Player> getPlayers() {
        HashSet<Player> players = new HashSet<>();
        this.users.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                players.add(p);
        });
        return players;
    }

    public void invite(@Nullable Player sender, @NotNull OfflinePlayer target) {
        PartyManager.getInstance().getDungeonPlayer(target).receiveInvite(this);
    }

    @NotNull
    public Set<UUID> getPlayersUUID() {
        return Collections.unmodifiableSet(users);
    }

    public boolean start(@NotNull DungeonHandler dungeon, Player player) {
        return start(dungeon, List.of(player));
    }

    public boolean start(@NotNull DungeonHandler dungeon, @NotNull Collection<Player> players) {
        if (getLeader() == null)
            return false;
        if (isExploringDungeon())
            return false;
        this.dungeon = dungeon;
        boolean result = false;
        for (Player player : players) {
            result |= enterDungeon(player);
        }
        if (!result)
            this.dungeon=null;
        return result;
    }

    public boolean enterDungeon(Player player) {
        if (dungeon==null)
            throw new IllegalStateException();
        DungeonPlayer dp = PartyManager.getInstance().getDungeonPlayer(player);
        dp.setPreEnterSnapshot(player);
        PlayerEnteringDungeonEvent event = new PlayerEnteringDungeonEvent(player,dungeon.getEntrance().getRoomHandler());
        Bukkit.getPluginManager().callEvent(event);
        boolean result = !event.isCancelled() && dungeon.getEntrance().teleportIn(player);
        if (!result)
            dp.getAndDeletePreEnterSnapshot();
        else {
            PlayerEnteredDungeonEvent event2 = new PlayerEnteredDungeonEvent(player, dungeon.getEntrance().getRoomHandler());
            Bukkit.getPluginManager().callEvent(event2);
        }
        return result;
    }

    public boolean isExploringDungeon() {
        return dungeon != null;
    }


    public boolean addPlayer(Player player) {
        if (parties.containsKey(player.getUniqueId()))
            throw new IllegalStateException();
        users.add(player.getUniqueId());
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
     * true if you are <b>online</b> and inside the dungeon
     * <p>
     * N.B. if you are offline but logged out inside the dungeon you are considered outside
     *
     * @param player
     * @return
     */
    public boolean isInsideDungeon(@NotNull OfflinePlayer player) {
        return PartyManager.getInstance().getDungeonPlayer(player).hasPreEnterSnapshot();
    }

    @NotNull
    public UUID getLeaderUUID() {
        return leader;
    }

    @Nullable
    public Player getLeader() {
        return Bukkit.getPlayer(leader);
    }

    @NotNull
    public OfflinePlayer getOfflineLeader() {
        return Bukkit.getOfflinePlayer(leader);
    }

    @Nullable
    public DungeonHandler getDungeon() {
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

    void onPlayerQuit(Player player) {
        if (!isInsideDungeon(player))
            return;
        DungeonPlayer dp = PartyManager.getInstance().getDungeonPlayer(player);
        dp.setLogoutSnapshot(player);
        dp.getAndDeletePreEnterSnapshot().apply(player);
    }

    void onPlayerJoin(Player player) {
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

    public void chatMessage(@NotNull Player sender, @NotNull String message) {
        getPlayers().forEach(player -> player.sendMessage(CUtils.craftMsg(player, "party.chat_prefix",
                "%sender%", sender.getName(), "%msg%", "%msg%").toLegacy().replace("%msg%", message)));
        //TODO choosable partychat color
    }
}
