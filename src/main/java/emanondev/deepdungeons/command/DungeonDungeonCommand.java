package emanondev.deepdungeons.command;

import emanondev.core.command.CoreCommand;
import emanondev.deepdungeons.BuilderMode;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.dungeon.DungeonTypeManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DungeonDungeonCommand extends CoreCommand {
    public DungeonDungeonCommand() {
        super("dungeondungeon", DeepDungeons.get(), Perms.DUNGEONDUNGEON_COMMAND, "create blueprint", List.of("ddungeon"));
    }

    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            help(sender, label, args);
            return;
        }
        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "create" -> {
                create(sender, label, args);
                return;
            }
            case "pause" -> {
                pause(sender, label, args);
                return;
            }
            case "continue" -> {
                continueCreate(sender, label, args);
                return;
            }
        }
        help(sender, label, args);
    }

    private void continueCreate(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!BuilderMode.getInstance().unpauseBuilder(player))
            sender.sendMessage("Message not implemented yet (not on paused mode)");//TODO//TODO check
    }

    private void pause(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!BuilderMode.getInstance().pauseBuilder(player))
            sender.sendMessage("Message not implemented yet (not on builder mode)");//TODO
        //TODO check
    }

    private void help(CommandSender sender, String label, String[] args) {
        sender.sendMessage("Message not implemented yet (command help)");//TODO
    }

    private void create(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (args.length != 3) {
            sender.sendMessage("Message not implemented yet (wrong arguments /ddungeon create <type> <id>)");//TODO
            return;
        }

        // type name
        DungeonType type = DungeonTypeManager.getInstance().get(args[1]);
        if (type == null) {
            sender.sendMessage("Message not implemented yet (selected type do not exist)");//TODO
            return;
        }
        String name = args[2].toLowerCase(Locale.ENGLISH);
        if (DungeonInstanceManager.getInstance().get(name) != null) {
            sender.sendMessage("Message not implemented yet (id already used)");//TODO
            return;
        }
        DungeonType.DungeonInstanceBuilder builder = type.getBuilder(name, player);
        if (!BuilderMode.getInstance().enterBuilderMode(player, builder)) {
            sender.sendMessage("Message not implemented yet (can't start, already on builder mode or on pause (do /ddungeon continue)?)");//TODO
            return;
        }
    }

    @Override
    public @Nullable List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        if (!(sender instanceof Player player)) return Collections.emptyList();


        return switch (args.length) {
            case 1 -> {
                if (BuilderMode.getInstance().isOnEditorMode(player))
                    yield this.complete(args[0], new String[]{"pause"});
                if (BuilderMode.getInstance().isOnPausedEditorMode(player))
                    yield this.complete(args[0], new String[]{"continue"});
                yield this.complete(args[0], new String[]{"create"});
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("create"))
                    yield this.complete(args[1], DungeonTypeManager.getInstance().getIds());
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }
}
