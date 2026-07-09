package emanondev.deepdungeons.command;

import emanondev.core.command.CoreCommand;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.BuilderMode;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonType;
import emanondev.deepdungeons.dungeon.DungeonTypeManager;
import emanondev.deepdungeons.populator.PopulatorTypeManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DungeonDungeonBuilderCommand extends CoreCommand {
    public DungeonDungeonBuilderCommand() {
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

    @Override
    @Nullable
    public List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        if (!(sender instanceof Player player)) return Collections.emptyList();


        return switch (args.length) {
            case 1 -> {
                if (BuilderMode.getInstance().isOnEditorMode(player))
                    yield this.complete(args[0], "pause");
                if (BuilderMode.getInstance().isOnPausedEditorMode(player))
                    yield this.complete(args[0], "continue");
                yield this.complete(args[0], "create");
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("create"))
                    yield this.complete(args[1], DungeonTypeManager.getInstance().getIds());
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    private void continueCreate(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!BuilderMode.getInstance().unpauseBuilder(player))
            new DMessage(getPlugin(), sender).appendLang("commands.ddungeon.cannot_continue").send();
    }

    private void pause(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (!BuilderMode.getInstance().pauseBuilder(player))
            new DMessage(getPlugin(), sender).appendLang("commands.ddungeon.cannot_pause").send();
        //TODO check
    }

    private void help(CommandSender sender, String label, String[] args) {
        new DMessage(getPlugin(), sender).appendLang("commands.ddungeon.help").send();
    }

    private void create(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        if (args.length != 3) {
            new DMessage(getPlugin(), sender).appendLang("commands.ddungeon.create.missing_params").send();
            return;
        }

        // type name
        DungeonType type = DungeonTypeManager.getInstance().get(args[1]);
        if (type == null) {
            StringBuilder allowedTypes=new StringBuilder();
            for(String atype : DungeonTypeManager.getInstance().getIds()) {
                allowedTypes.append(atype).append(", ");
            }

            new DMessage(getPlugin(), sender).appendLang("commands.ddungeon.create.wrong_type","%types%",allowedTypes.toString()).send();
            return;
        }
        String name = args[2].toLowerCase(Locale.ENGLISH);
        if (DungeonInstanceManager.getInstance().get(name) != null) {
            new DMessage(getPlugin(), sender).appendLang("commands.ddungeon.create.invalid_id").send();
            return;
        }
        DungeonType.DungeonBuilder builder = type.getBuilder(name, player);
        if (!BuilderMode.getInstance().enterBuilderMode(player, builder)) {
            sender.sendMessage("Message not implemented yet (can't start, already on builder mode or on pause (do /ddungeon continue)?)");//TODO
            return;
        }
    }
}
