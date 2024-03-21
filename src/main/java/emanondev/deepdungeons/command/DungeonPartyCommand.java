package emanondev.deepdungeons.command;

import emanondev.core.CooldownAPI;
import emanondev.core.command.CoreCommand;
import emanondev.core.message.DMessage;
import emanondev.core.message.SimpleMessage;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.party.DungeonPlayer;
import emanondev.deepdungeons.party.PartyManager;
import emanondev.deepdungeons.party.PartyManager.Party;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DungeonPartyCommand extends CoreCommand {
    private static final int LIST_PARTY_PER_PAGE = 8;

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
            case "leader" -> leader(sender, label, args);
            case "leave" -> leave(sender, label, args);
            case "open" -> open(sender, label, args);
            case "close" -> close(sender, label, args);
            case "info" -> info(sender, label, args);
            case "list" -> list(sender, label, args);
            //case "revokeinvite" -> revokeinvite(sender, label, args);
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
            CUtils.sendMsg(player, "party.join_arguments", "%label%", label);
            return;
        }
        Party party = PartyManager.getInstance().getParty(player);
        if (party != null) {
            CUtils.sendMsg(player, "party.has_party", "%label%", label);
            return;
        }
        OfflinePlayer target = readOfflinePlayer(args[1]);
        if (target == null) {
            CUtils.sendMsg(player, "party.target_do_not_exist", "%label%", label, "%name%", args[1]);
            return;
        }
        Party targetParty = PartyManager.getInstance().getParty(target);
        if (targetParty == null) {
            CUtils.sendMsg(player, "party.target_has_no_party", "%label%", label, "%name%", args[1]);
            return;
        }
        DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
        if (!targetParty.isPartyPublic() && !dPlayer.hasInvite(targetParty)) {
            CUtils.sendMsg(player, "party.party_closed_cannot_join", "%label%", label, "%name%", args[1]);
            return;
        }
        dPlayer.revokeInvite(targetParty);
        new SimpleMessage(DeepDungeons.get(), "party.notify_player_join").send(targetParty.getPlayers(),
                "%label%", label, "%name%", player.getName());
        targetParty.addPlayer(player);
        CUtils.sendMsg(player, "party.success_join", "%label%", label, "%name%", player.getName());
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
            CUtils.sendMsg(player, "party.kick_arguments", "%label%", label);
            return;
        }
        Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            CUtils.sendMsg(player, "party.has_no_party", "%label%", label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            CUtils.sendMsg(player, "party.require_leader", "%label%", label);
            return;
        }
        OfflinePlayer target = readOfflinePlayer(args[1]);
        if (target == null) {
            CUtils.sendMsg(player, "party.target_do_not_exist", "%label%", label, "%name%", args[1]);
            return;
        }
        Party targetParty = PartyManager.getInstance().getParty(target);
        if (!party.equals(targetParty)) {
            CUtils.sendMsg(player, "party.target_not_in_your_party", "%label%", label, "%name%", args[1]);
            return;
        }
        if (target.equals(player)) {
            CUtils.sendMsg(player, "party.cannot_target_yourself", "%label%", label);
            return;
        }
        if (party.isInsideDungeon(target)) {
            CUtils.sendMsg(player, "party.kick_not_in_dungeons", "%label%", label);
            return;
        }
        party.removePlayer(target);
        new SimpleMessage(DeepDungeons.get(), "party.notify_player_kick").send(targetParty.getPlayers(),
                "%label%", label, "%name%", target.getName());
        if (target.isOnline())
            CUtils.sendMsg(player, "party.kicked", "%label%", label, "%who%", player.getName());
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
            CUtils.sendMsg(player, "party.invite_arguments", "%label%", label);
            return;
        }
        Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            CUtils.sendMsg(player, "party.has_no_party", "%label%", label);
            return;
        }
        if (!party.isPartyPublic() && !player.equals(party.getLeader())) {
            CUtils.sendMsg(player, "party.invite_require_leader", "%label%", label);
            return;
        }
        Player target = readPlayer(player, args[1]);
        if (target == null) {
            CUtils.sendMsg(player, "party.target_not_online", "%label%", label, "%name%", args[1]);
            return;
        }
        Party targetParty = PartyManager.getInstance().getParty(target);
        if (party.equals(targetParty)) {
            CUtils.sendMsg(player, "party.target_in_your_party", "%label%", label, "%name%", target.getName());
            return;
        }
        if (targetParty != null) {
            CUtils.sendMsg(player, "party.target_has_party", "%label%", label, "%name%", target.getName());
            return;
        }

        CooldownAPI cooldown = DeepDungeons.get().getCooldownAPI(false);
        String leaderName = party.getOfflineLeader().getName();
        if (party.isPartyPublic()) {
            if (cooldown.hasCooldown(player, "p_i_s_" + target.getName())) {
                //TODO already sended
                return;
            }
            if (cooldown.hasCooldown(target, "p_i_r_" + party.getId())) {
                //TODO already reiceved
                return;
            }
            cooldown.setCooldownSeconds(player, "p_i_s_" + target.getName(), 60);
            cooldown.setCooldownSeconds(target, "p_i_r_" + party.getId(), 30);
            CUtils.sendMsg(target, "party.public_invite", "%label%", label, "%name%", player.getName(), "%leader%", leaderName);
            return;
        }
        if (cooldown.hasCooldown(target, "p_i_p_" + party.getId())) {
            //TODO already sended
            return;
        }
        cooldown.setCooldownSeconds(target, "p_i_p_" + party.getId(), 30);
        CUtils.sendMsg(target, "party.private_invite", "%label%", label, "%name%", player.getName());
        new SimpleMessage(DeepDungeons.get(), "party.notify_private_invite").send(party.getPlayers(),
                "%label%", label, "%name%", target.getName(), "%who%", player.getName());
        DungeonPlayer dTarget = PartyManager.getInstance().getDungeonPlayer(target);
        dTarget.receiveInvite(party);
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
            CUtils.sendMsg(player, "party.leader_arguments", "%label%", label);
            return;
        }
        Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            CUtils.sendMsg(player, "party.has_no_party", "%label%", label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            CUtils.sendMsg(player, "party.require_leader", "%label%", label);
            return;
        }
        Player newLeader = readPlayer(sender, args[1]);
        if (newLeader == null) {
            CUtils.sendMsg(player, "party.target_not_online", "%label%", label, "%name%", args[1]);
            return;
        }
        if (!party.equals(PartyManager.getInstance().getParty(newLeader))) {
            CUtils.sendMsg(player, "party.target_not_in_your_party", "%label%", label, "%name%", args[1]);
            return;
        }
        if (newLeader.equals(player)) {
            CUtils.sendMsg(player, "party.cannot_target_yourself", "%label%", label);
            return;
        }
        party.setLeader(newLeader);
        new SimpleMessage(DeepDungeons.get(), "party.notify_leader_change").send(party.getPlayers(), "%label%", label,
                "%from%", player.getName(), "%to%", newLeader.getName());
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
        Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            CUtils.sendMsg(player, "party.has_no_party", "%label%", label);
            return;
        }
        if (player.equals(party.getLeader())) {
            CUtils.sendMsg(player, "party.leave_leader_must_disband", "%label%", label);
            return;
        }
        party.removePlayer(player);
        new SimpleMessage(DeepDungeons.get(), "party.notify_player_leave").send(party.getPlayers(), "%label%", label,
                "%name%", player.getName());
        CUtils.sendMsg(player, "party.success_leave", "%label%", label);
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
        Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            CUtils.sendMsg(player, "party.has_no_party", "%label%", label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            CUtils.sendMsg(player, "party.require_leader", "%label%", label);
            return;
        }
        if (party.isPartyPublic()) {
            CUtils.sendMsg(player, "party.already_opened", "%label%", label);
            return;
        }
        party.togglePartyPublic();
        new SimpleMessage(DeepDungeons.get(), "party.notify_party_public").send(party.getPlayers(), "%label%", label);
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
        Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            CUtils.sendMsg(player, "party.has_no_party", "%label%", label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            CUtils.sendMsg(player, "party.require_leader", "%label%", label);
            return;
        }
        if (!party.isPartyPublic()) {
            CUtils.sendMsg(player, "party.already_closed", "%label%", label);
            return;
        }
        party.togglePartyPublic();
        new SimpleMessage(DeepDungeons.get(), "party.notify_party_private").send(party.getPlayers(), "%label%", label);
    }

    private void list(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(Perms.PARTY_LIST)) {
            this.permissionLackNotify(sender, Perms.PARTY_LIST);
            return;
        }
        TreeSet<Party> parties = new TreeSet<>((c1, c2) -> {
            int pl1 = c1.getPlayers().size();
            int pl2 = c2.getPlayers().size();
            if (pl1 != pl2)
                return pl1 - pl2;
            pl1 = c1.getPlayersUUID().size();
            pl2 = c1.getPlayersUUID().size();
            if (pl1 != pl2)
                return pl1 - pl2;
            return c1.getOfflineLeader().getName().compareToIgnoreCase(c2.getOfflineLeader().getName());
        });
        PartyManager.getInstance().getAll(party -> party.getPlayers().size() > 0);
        ArrayList<Party> list = new ArrayList<>(parties);
        if (list.isEmpty()) {
            CUtils.sendMsg(sender, "party.list_no_parties", "%label%", label);
            return;
        }


        int page = 0;
        try {
            if (args.length > 1)
                page = Math.max(0, Integer.parseInt(args[1]) - 1);
        } catch (Exception ignored) {
        }
        page = Math.min(list.size() / LIST_PARTY_PER_PAGE + (list.size() % LIST_PARTY_PER_PAGE == 0 ? -1 : 0), page);
        DMessage msg = new DMessage(DeepDungeons.get(), sender);
        msg.appendLang("party.list_prefix", "%label%", label, "%page%", String.valueOf(page + 1),
                "%next_page%", String.valueOf(page + 2), "%prev_page%", String.valueOf(page));
        for (int i = 0; i < LIST_PARTY_PER_PAGE && page * LIST_PARTY_PER_PAGE + i < list.size(); i++) {
            Party party = list.get(page * LIST_PARTY_PER_PAGE + i);
            msg.newLine().append("party.list_line", "%label%", label, "%leader%",
                    party.getOfflineLeader().getName(), "%online%", String.valueOf(party.getPlayers().size())
                    , "%members%", String.valueOf(party.getPlayersUUID().size()), "%index%", String.valueOf(page * LIST_PARTY_PER_PAGE + i + 1));
        }
        msg.newLine().appendLang("party.list_postfix", "%label%", label, "%page%", String.valueOf(page + 1),
                "%next_page%", String.valueOf(page + 2), "%prev_page%", String.valueOf(page));
        msg.send();
    }

    //info [player=you]
    private void info(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(Perms.PARTY_INFO)) {
            this.permissionLackNotify(sender, Perms.PARTY_INFO);
            return;
        }
        if (args.length <= 1 && !(sender instanceof Player)) {
            CUtils.sendMsg(sender, "party.info_arguments_console", "%label%", label, "%name%", args[1]);
            return;
        }
        OfflinePlayer target = args.length > 1 ? readOfflinePlayer(args[1]) : (Player) sender;
        if (target == null) {
            CUtils.sendMsg(sender, "party.target_do_not_exist", "%label%", label, "%name%", args[1]);
            return;
        }
        Party party = PartyManager.getInstance().getParty(target);
        if (party == null) {
            if (target.equals(sender)) {
                CUtils.sendMsg(sender, "party.has_no_party", "%label%", label);
                return;
            }
            CUtils.sendMsg(sender, "party.target_has_no_party", "%label%", label, "%name%", args[1]);
            return;
        }

        //TODO
    }

    private void help(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(Perms.PARTY_HELP)) {
            this.permissionLackNotify(sender, Perms.PARTY_HELP);
            return;
        }
        int page = 1;
        try {
            if (args.length > 1 && args[0].equalsIgnoreCase("help"))
                page = Math.max(1, Math.min(2, Integer.parseInt(args[1])));
        } catch (Exception ignored) {
        }
        DMessage msg = new DMessage(DeepDungeons.get(), sender);
        msg.appendLang("party.help_prefix", "%label%", label, "%page%", String.valueOf(page),
                "%next_page%", String.valueOf(page + 1), "%prev_page%", String.valueOf(page - 1));
        Player player = sender instanceof Player p ? p : null;
        Party party = player != null ? PartyManager.getInstance().getParty(player) : null;
        switch (page) {
            case 1 -> {
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_CREATE) && player != null && party == null ?
                        "party.help_create_allowed" : "party.help_create_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_JOIN) && player != null && party == null ?
                        "party.help_join_allowed" : "party.help_join_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_INFO) ?
                        "party.help_info_allowed" : "party.help_info_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_LIST) ?
                        "party.help_list_allowed" : "party.help_list_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_LEAVE) && party != null ?
                        "party.help_leave_allowed" : "party.help_leave_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_INVITE) && party != null ?
                        "party.help_invite_allowed" : "party.help_invite_unallowed", "%label%", label);//check public (?) nah
            }
            case 2 -> {
                boolean leader = party != null && sender.equals(party.getLeader());
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_KICK) && leader ?
                        "party.help_kick_allowed" : "party.help_kick_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_LEADER) && leader ?
                        "party.help_leader_allowed" : "party.help_leader_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_TOGGLE_PUBLIC) && leader && !party.isPartyPublic() ?
                        "party.help_open_allowed" : "party.help_open_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_TOGGLE_PUBLIC) && leader && party.isPartyPublic() ?
                        "party.help_close_allowed" : "party.help_close_unallowed", "%label%", label);
                msg.newLine().appendLang(sender.hasPermission(Perms.PARTY_TOGGLE_PUBLIC) && leader ?
                        "party.help_disband_allowed" : "party.help_disband_unallowed", "%label%", label);
            }
        }
        msg.newLine().appendLang("party.help_postfix", "%label%", label, "%page%", String.valueOf(page),
                "%next_page%", String.valueOf(page + 1), "%prev_page%", String.valueOf(page - 1));
        msg.send();
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
        Party party = PartyManager.getInstance().getParty(player);
        if (party == null) {
            CUtils.sendMsg(player, "party.has_no_party", "%label%", label);
            return;
        }
        if (!player.equals(party.getLeader())) {
            CUtils.sendMsg(player, "party.require_leader", "%label%", label);
            return;
        }
        Set<Player> players = party.getPlayers();
        party.disband();
        new SimpleMessage(DeepDungeons.get(), "party.notify_party_disband").send(players, "%label%", label,
                "%name%", player.getName());
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
            CUtils.sendMsg(player, "party.has_party", "%label%", label);
            return;
        }
        PartyManager.getInstance().createParty(player);
        CUtils.sendMsg(player, "party.success_create", "%label%", label);
    }

    @Override
    @Nullable
    public List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        if (!(sender instanceof Player player))
            return Collections.emptyList();
        return switch (args.length) {
            case 1 -> {
                Party party = PartyManager.getInstance().getParty(player);
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
                if (player.hasPermission(Perms.PARTY_HELP))
                    list.addAll(this.complete(args[0], Collections.singleton("help")));
                yield list;
            }
            case 2 -> switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "help" -> player.hasPermission(Perms.PARTY_HELP) ? this.complete(args[1], List.of("1", "2")) : Collections.emptyList();
                case "info" -> player.hasPermission(Perms.PARTY_INFO) ? this.completePlayerNames(sender, args[1],
                        (p) -> PartyManager.getInstance().getParty(p) != null) : Collections.emptyList();
                case "list" -> player.hasPermission(Perms.PARTY_LIST) ? this.complete(args[1], List.of("1", "2", "3", "4", "5")) : Collections.emptyList();
                case "leader" -> {
                    Party party = PartyManager.getInstance().getParty(player);
                    if (party != null && player.equals(party.getLeader()) && player.hasPermission(Perms.PARTY_LEADER)) {
                        yield this.completePlayerNames(player, args[1], party.getPlayers(), (p) -> !p.equals(player));
                    }
                    yield Collections.emptyList();
                }
                case "invite" -> {
                    Party party = PartyManager.getInstance().getParty(player);
                    if (party != null && player.hasPermission(Perms.PARTY_INVITE) && (player.equals(party.getLeader())
                            || party.isPartyPublic())) {
                        yield this.completePlayerNames(player, args[1], (p) -> PartyManager.getInstance().getParty(p) == null);
                    }
                    yield Collections.emptyList();
                }
                case "kick" -> {
                    Party party = PartyManager.getInstance().getParty(player);
                    if (party != null && player.equals(party.getLeader()) && player.hasPermission(Perms.PARTY_KICK)) {
                        yield this.complete(args[1], party.getPlayersUUID(), (uuid) -> Bukkit.getOfflinePlayer(uuid).getName(),
                                (uuid) -> !uuid.equals(party.getLeaderUUID()));
                    }
                    yield Collections.emptyList();
                }
                case "join" -> {
                    Party party = PartyManager.getInstance().getParty(player);
                    if (party == null && player.hasPermission(Perms.PARTY_JOIN)) {
                        DungeonPlayer dPlayer = PartyManager.getInstance().getDungeonPlayer(player);
                        yield this.completePlayerNames(sender, args[1], (p) -> {
                            Party partyT = PartyManager.getInstance().getParty(p);
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