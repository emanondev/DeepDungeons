package emanondev.deepdungeons.command;

import emanondev.core.command.CoreCommand;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.area.AreaManager;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.party.PartyManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DungeonCreatorCommand extends CoreCommand {
    public DungeonCreatorCommand() {
        super("dungeoncreator", DeepDungeons.get(), Perms.DUNGEONCREATOR_COMMAND);
    }

    //create <id>
    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "create" -> {
                create(sender, label, args);
                return;
            }
            case "start" -> {
                start(sender, label, args);
                return;
            }
        }
        sender.sendMessage("Help Message not implemented");
    }

    private void start(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }
        if (args.length != 2) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        String id = args[1];
        DungeonType.@Nullable DungeonInstance dungeon = DungeonInstanceManager.getInstance().get(id);
        DungeonType.DungeonInstance.@Nullable DungeonHandler handler = AreaManager.getInstance().getReady(dungeon);
        PartyManager.Party party = PartyManager.getInstance().getParty(player);
        if (party == null)
            party = PartyManager.getInstance().createParty(player);
        PartyManager.getInstance().startDungeon(party, handler);
    }

    private void create(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        String id = args[1];
        DungeonType.@Nullable DungeonInstance dungeon = DungeonInstanceManager.getInstance().get(id);
        if (dungeon == null) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Help Message not implemented");
            return;
        }
        DungeonType.DungeonInstance.@NotNull DungeonHandler handler = dungeon.createHandler(player.getLocation().getWorld());
        Vector loc = handler.getBoundingBox().getCenter();
        new DMessage(DeepDungeons.get(), sender).append("<blue>Dungeon created at " + loc.getBlockX() + " "
                + loc.getBlockY() + " " + loc.getBlockZ() + " ").append("<click:run_command:'/tp "
                + sender.getName() + " " + loc.getBlockX() + " " + (loc.getBlockY() + handler.getBoundingBox().getHeight() * 0.5 + 10) + " " + loc.getBlockZ()
                + "'><hover:show_text:'teleport to " + loc.getBlockX() + " " + (loc.getBlockY() + handler.getBoundingBox().getHeight() * 0.5 + 10) + " "
                + loc.getBlockZ() + "'><yellow>[TP]</yellow></hover></click>").send();
    }

    @Override
    public @Nullable List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        return switch (args.length) {
            case 1 -> this.complete(args[0], new String[]{"create", "start"});
            case 2 -> this.complete(args[1], DungeonInstanceManager.getInstance().getIds());
            default -> Collections.emptyList();
        };
    }
}
