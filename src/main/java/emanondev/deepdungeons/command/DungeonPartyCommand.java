package emanondev.deepdungeons.command;

import emanondev.core.command.CoreCommand;
import emanondev.core.message.SimpleMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.party.DungeonPlayer;
import emanondev.deepdungeons.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DungeonPartyCommand extends CoreCommand {
    public DungeonPartyCommand() {
        super("dungeonparty", DeepDungeons.get(), null, "manage party", List.of("party"));
    }

    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            help(sender, label, args);
            return;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "create" -> create(sender, label, args);
            case "disband" -> disband(sender, label, args);
            case "join" -> join(sender, label, args);
            case "kick" -> kick(sender, label, args);
            case "invite" -> invite(sender, label, args);
            //case "reinvite" -> invite(sender, label, args);
            case "leader" -> leader(sender, label, args);
            case "leave" -> leave(sender, label, args);
            case "open" -> open(sender, label, args);
            case "close" -> close(sender, label, args);
            case "info" -> info(sender, label, args);
            case "list" -> list(sender, label, args);
            default -> help(sender, label, args);
        }

    }

    //join <player>
    private void join(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_JOIN)) {
            this.permissionLackNotify(player, Perms.PARTY_JOIN);
            return;
        }
        if (args.length != 2) {
            new SimpleMessage(DeepDungeons.get(),"party.join_arguments").send(player,"%label%",label);
            return;
        }
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party != null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_party").send(player,"%label%",label);
            return;
        }
        OfflinePlayer target = readOfflinePlayer(args[1]);
        if (target == null) {
            new SimpleMessage(DeepDungeons.get(),"party.target_do_not_exist").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        PartyManager.Party targetParty = PartyManager.getInstance().getParty(target);
        if (targetParty == null) {
            new SimpleMessage(DeepDungeons.get(),"party.target_has_no_party").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
        if (!targetParty.isPartyPublic() && !dPlayer.hasInvite(targetParty)) {
            new SimpleMessage(DeepDungeons.get(),"party.party_closed_cannot_join").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        dPlayer.revokeInvite(targetParty);
        targetParty.addPlayer(player);
        //TODO feedback
    }

    //kick <player>
    private void kick(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_KICK)) {
            this.permissionLackNotify(player, Perms.PARTY_KICK);
            return;
        }
        if (args.length != 2) {
            new SimpleMessage(DeepDungeons.get(),"party.kick_arguments").send(player,"%label%",label);
            return;
        }
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_no_party").send(player,"%label%",label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            new SimpleMessage(DeepDungeons.get(),"party.require_leader").send(player,"%label%",label);
            return;
        }
        OfflinePlayer target = readOfflinePlayer(args[1]);
        if (target == null) {
            new SimpleMessage(DeepDungeons.get(),"party.target_do_not_exist").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        PartyManager.Party targetParty = PartyManager.getInstance().getParty(target);
        if (!party.equals(targetParty)) {
            new SimpleMessage(DeepDungeons.get(),"party.target_not_in_your_party").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        if (target.equals(player)) {
            new SimpleMessage(DeepDungeons.get(),"party.cannot_target_yourself").send(player,
                    "%label%",label);
            return;
        }
        if (party.isInsideDungeon(target)) {
            new SimpleMessage(DeepDungeons.get(),"party.kick_not_in_dungeons").send(player,
                    "%label%",label);
            return;
        }
        party.removePlayer(target);
        //TODO feedback
    }

    //invite <player>
    private void invite(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_INVITE)) {
            this.permissionLackNotify(player, Perms.PARTY_INVITE);
            return;
        }
        if (args.length != 2) {
            new SimpleMessage(DeepDungeons.get(),"party.invite_arguments").send(player,"%label%",label);
            return;
        }
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_no_party").send(player,"%label%",label);
            return;
        }
        if (!party.isPartyPublic() && !player.equals(party.getLeader())) {
            new SimpleMessage(DeepDungeons.get(),"party.invite_require_leader").send(player,"%label%",label);
            return;
        }
        Player target = readPlayer(player, args[1]);
        if (target == null) {
            new SimpleMessage(DeepDungeons.get(),"party.target_not_online").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        PartyManager.Party targetParty = PartyManager.getInstance().getParty(target);
        if (party.equals(targetParty)) {
            new SimpleMessage(DeepDungeons.get(),"party.target_in_your_party").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        if (targetParty != null) {
            new SimpleMessage(DeepDungeons.get(),"party.target_has_party").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }

        if (party.isPartyPublic()) {
            //TODO invia messaggio per joinare senza invito
            //& imposta cooldown antispam
            return;
        }
        DungeonPlayer dTarget = PartyManager.getInstance().getDungeonPlayer(target);
        dTarget.receiveInvite(party);
        //TODO feedback
        //& imposta cooldown antispam
    }

    //leader <leader name>
    private void leader(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_LEADER)) {
            this.permissionLackNotify(player, Perms.PARTY_LEADER);
            return;
        }
        if (args.length != 2) {
            new SimpleMessage(DeepDungeons.get(),"party.leader_arguments").send(player,"%label%",label);
            return;
        }
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_no_party").send(player,"%label%",label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            new SimpleMessage(DeepDungeons.get(),"party.require_leader").send(player,"%label%",label);
            return;
        }
        Player newLeader = readPlayer(sender, args[1]);
        if (newLeader == null) {
            new SimpleMessage(DeepDungeons.get(),"party.target_not_online").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        if (!party.equals(PartyManager.getInstance().getParty(newLeader))) {
            new SimpleMessage(DeepDungeons.get(),"party.target_not_in_your_party").send(player,
                    "%label%",label,"%name%",args[1]);
            return;
        }
        if (newLeader.equals(player)) {
            new SimpleMessage(DeepDungeons.get(),"party.cannot_target_yourself").send(player,"%label%",label);
            return;
        }
        party.setLeader(newLeader);
        //TODO feedback
    }

    private void leave(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_LEAVE)) {
            this.permissionLackNotify(player, Perms.PARTY_LEAVE);
            return;
        }
        //DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_no_party").send(player,"%label%",label);
            return;
        }
        if (player.equals(party.getLeader())) {
            new SimpleMessage(DeepDungeons.get(),"party.leave_leader_must_disband").send(player,"%label%",label);
            return;
        }
        party.removePlayer(player);
        //TODO feedback
    }

    private void open(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_TOGGLE_PUBLIC)) {
            this.permissionLackNotify(player, Perms.PARTY_TOGGLE_PUBLIC);
            return;
        }
        //DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_no_party").send(player,"%label%",label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            new SimpleMessage(DeepDungeons.get(),"party.require_leader").send(player,"%label%",label);
            return;
        }
        if (party.isPartyPublic()) {
            new SimpleMessage(DeepDungeons.get(),"party.already_opened").send(player,"%label%",label);
            return;
        }
        party.togglePartyPublic();
        //TODO feedback
    }

    private void close(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_TOGGLE_PUBLIC)) {
            this.permissionLackNotify(player, Perms.PARTY_TOGGLE_PUBLIC);
            return;
        }
        //DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_no_party").send(player,"%label%",label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            new SimpleMessage(DeepDungeons.get(),"party.require_leader").send(player,"%label%",label);
            return;
        }
        if (!party.isPartyPublic()) {
            new SimpleMessage(DeepDungeons.get(),"party.already_closed").send(player,"%label%",label);
            return;
        }
        party.togglePartyPublic();
        //TODO feedback
    }

    private void list(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(Perms.PARTY_LIST)) {
            this.permissionLackNotify(sender, Perms.PARTY_LIST);
            return;
        }
        //TODO
    }

    private void info(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(Perms.PARTY_INFO)) {
            this.permissionLackNotify(sender, Perms.PARTY_INFO);
            return;
        }
        //TODO
    }

    private void help(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(Perms.PARTY_HELP)) {
            this.permissionLackNotify(sender, Perms.PARTY_HELP);
            return;
        }
        //TODO
    }

    private void disband(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_DISBAND)) {
            this.permissionLackNotify(player, Perms.PARTY_DISBAND);
            return;
        }
        //DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_no_party").send(player,"%label%",label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            new SimpleMessage(DeepDungeons.get(),"party.require_leader").send(player,"%label%",label);
            return;
        }
        party.disband();
        //TODO feedback
    }

    //create ... ?
    private void create(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!player.hasPermission(Perms.PARTY_CREATE)) {
            this.permissionLackNotify(player, Perms.PARTY_CREATE);
            return;
        }
        //DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
        if (PartyManager.getInstance().getParty(player) != null) {
            new SimpleMessage(DeepDungeons.get(),"party.has_party").send(player,"%label%",label);
            return;
        }
        PartyManager.getInstance().createParty(player);
        //TODO feedback
    }

    @Override
    public @Nullable List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        if (!(sender instanceof Player player))
            return Collections.emptyList();
        return switch (args.length) {
            case 1 -> {
                PartyManager.Party party = PartyManager.getInstance().getParty(player);
                ArrayList<String> list = new ArrayList<>();
                if (party == null) {
                    if (player.hasPermission(Perms.PARTY_CREATE))
                        list.addAll(this.complete(args[0], Collections.singleton("create")));
                    if (player.hasPermission(Perms.PARTY_JOIN))
                        list.addAll(this.complete(args[0], Collections.singleton("join")));
                } else {
                    boolean leader = player.equals(party.getLeader());
                    if (leader && player.hasPermission(Perms.PARTY_DISBAND))
                        list.addAll(this.complete(args[0], Collections.singleton("disband")));
                    if (leader && player.hasPermission(Perms.PARTY_KICK))
                        list.addAll(this.complete(args[0], Collections.singleton("kick")));
                    if (leader && player.hasPermission(Perms.PARTY_LEADER))
                        list.addAll(this.complete(args[0], Collections.singleton("leader")));
                    if (leader && player.hasPermission(Perms.PARTY_TOGGLE_PUBLIC))
                        list.addAll(this.complete(args[0], Collections.singleton("open")));
                    if (leader && player.hasPermission(Perms.PARTY_TOGGLE_PUBLIC))
                        list.addAll(this.complete(args[0], Collections.singleton("close")));
                    if ((leader || party.isPartyPublic()) && player.hasPermission(Perms.PARTY_INVITE))
                        list.addAll(this.complete(args[0], Collections.singleton("invite")));
                    if (!leader && player.hasPermission(Perms.PARTY_LEAVE))
                        list.addAll(this.complete(args[0], Collections.singleton("leave")));
                }
                if (player.hasPermission(Perms.PARTY_INFO))
                    list.addAll(this.complete(args[0], Collections.singleton("info")));
                if (player.hasPermission(Perms.PARTY_LIST))
                    list.addAll(this.complete(args[0], Collections.singleton("list")));
                yield list;
            }
            case 2 -> switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "info" -> player.hasPermission(Perms.PARTY_INFO) ? this.completePlayerNames(sender, args[1],
                        (p) -> PartyManager.getInstance().getParty(p) != null) : Collections.emptyList();
                case "list" -> player.hasPermission(Perms.PARTY_LIST) ? this.complete(args[1], List.of("1", "2", "3", "4", "5")) : Collections.emptyList();
                case "leader" -> {
                    PartyManager.Party party = PartyManager.getInstance().getParty(player);
                    if (party != null && player.equals(party.getLeader()) && player.hasPermission(Perms.PARTY_LEADER)) {
                        yield this.completePlayerNames(player, args[1], party.getPlayers(), (p) -> !p.equals(player));
                    }
                    yield Collections.emptyList();
                }
                case "invite" -> {
                    PartyManager.Party party = PartyManager.getInstance().getParty(player);
                    if (party != null && player.hasPermission(Perms.PARTY_INVITE) && (player.equals(party.getLeader())
                            || party.isPartyPublic())) {
                        yield this.completePlayerNames(player, args[1], (p) -> PartyManager.getInstance().getParty(p) == null);
                    }
                    yield Collections.emptyList();
                }
                case "kick" -> {
                    PartyManager.Party party = PartyManager.getInstance().getParty(player);
                    if (party != null && player.equals(party.getLeader()) && player.hasPermission(Perms.PARTY_KICK)) {
                        yield this.complete(args[1], party.getPlayersUUID(), (uuid) -> Bukkit.getOfflinePlayer(uuid).getName(),
                                (uuid) -> !uuid.equals(party.getLeaderUUID()));
                    }
                    yield Collections.emptyList();
                }
                case "join" -> {
                    PartyManager.Party party = PartyManager.getInstance().getParty(player);
                    if (party == null && player.hasPermission(Perms.PARTY_JOIN)) {
                        DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
                        yield this.completePlayerNames(sender, args[1], (p) -> {
                            PartyManager.Party partyT = PartyManager.getInstance().getParty(p);
                            return partyT != null && (partyT.isPartyPublic() || dPlayer.hasInvite(partyT));
                        });
                    }
                    yield Collections.emptyList();
                }
                default -> Collections.emptyList();
            };
            default -> Collections.emptyList();
        };
    }
}
