package emanondev.deepdungeons.party;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class PartyManager extends DRegistry<Party> implements Listener {
    private static final PartyManager instance = new PartyManager();
    private final HashMap<UUID, DungeonPlayer> dungeonPlayers = new HashMap<>();

    public PartyManager() {
        super(DeepDungeons.get(), "PartyManager", true);
        getPlugin().registerListener(this);
    }

    @NotNull
    public static PartyManager getInstance() {
        return instance;
    }

    @NotNull
    public Party createParty(@NotNull Player leader) {
        if (Party.getParty(leader.getUniqueId()) != null)
            throw new IllegalStateException();
        return new Party(leader);
    }

    public void startDungeon(@NotNull Party party, @NotNull DungeonHandler dungeon, @NotNull Collection<Player> players) {
        if (dungeon.getState() != DungeonHandler.State.READY)
            throw new IllegalStateException();
        if (party.getLeader() == null)
            throw new IllegalStateException();
        if (party.isExploringDungeon())
            throw new IllegalStateException();
        dungeon.start(party);
        if (!party.start(dungeon, players)) {
            DeepDungeons.get().logIssue("Unable to start dungeon!");
            dungeon.flagCompleted();
        }
    }

    @Nullable
    public Party getParty(OfflinePlayer player) {
        return Party.getParty(player.getUniqueId());
    }

    @NotNull
    public DungeonPlayer getDungeonPlayer(@NotNull OfflinePlayer player) {
        return getDungeonPlayer(player.getUniqueId());
    }

    @NotNull
    public DungeonPlayer getDungeonPlayer(@NotNull UUID uuid) {
        DungeonPlayer value = dungeonPlayers.get(uuid);
        if (value != null)
            return value;
        value = new DungeonPlayer();
        dungeonPlayers.put(uuid, value);
        return value;
    }

    @EventHandler
    private void event(@NotNull PlayerQuitEvent event) {
        Party party = Party.getParty(event.getPlayer().getUniqueId());
        if (party == null)
            return;
        party.onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    private void event(@NotNull PlayerJoinEvent event) {
        Party party = Party.getParty(event.getPlayer().getUniqueId());
        if (party == null)
            return;
        party.onPlayerJoin(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    private void event(@NotNull AsyncPlayerChatEvent event) {
        if (!event.getPlayer().hasPermission(Perms.PARTY_CHAT))
            return;
        Party party = Party.getParty(event.getPlayer().getUniqueId());
        if (party == null)
            return;
        DungeonPlayer dPlayer = getDungeonPlayer(event.getPlayer());
        if (!dPlayer.isOnPartyChat())
            return;
        event.setCancelled(true);
        party.chatMessage(event.getPlayer(), event.getMessage());
    }
}
