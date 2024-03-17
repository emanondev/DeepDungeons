package emanondev.deepdungeons.command;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsInventory;
import emanondev.core.command.CoreCommand;
import emanondev.core.gui.PagedListFGui;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.spawner.MonsterSpawnerType;
import emanondev.deepdungeons.spawner.MonsterSpawnerTypeManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DungeonMonsterSpawnerCommand extends CoreCommand {
    public DungeonMonsterSpawnerCommand() {
        super("dungeonmonsterspawner", DeepDungeons.get(), Perms.DUNGEONMONSTERSPAWNER_COMMAND, "create blueprint", List.of("dmonsterspawner"));
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
        }
        help(sender, label, args);
    }

    private void help(CommandSender sender, String label, String[] args) {
        sender.sendMessage("Message not implemented yet (command help)");//TODO
    }

    private void create(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        switch (args.length) {
            case 1 -> {
                MonsterSpawnerType.MonsterSpawnerInstanceBuilder builder = MonsterSpawnerTypeManager.getInstance().getMonsterSpawnerInstance(item);
                if (builder == null && !UtilsInventory.isAirOrNull(item)) {
                    sender.sendMessage("Message not implemented yet (hand must be a monsterspawner blueprint or empty)");//TODO
                    return;
                }
                if (builder == null) {
                    new PagedListFGui<>(new DMessage(DeepDungeons.get()).append("&9MonsterSpawner Type Selector").toLegacy(),
                            6, player, null, DeepDungeons.get(), false,
                            (InventoryClickEvent click, MonsterSpawnerType type) -> {
                                MonsterSpawnerType.MonsterSpawnerInstanceBuilder bb = type.getBuilder();
                                bb.openGui(player);
                                player.getInventory().setItemInMainHand(bb.toItem());
                                return false;
                            },
                            (MonsterSpawnerType type) -> new ItemBuilder(Material.CHEST)
                                    .setDescription(new DMessage(DeepDungeons.get(), player).append("<blue>Type: <gold>" + type.getId() + "</gold></blue>").newLine().append(
                                            type.getDescription(player)
                                    )).build());
                    return;
                }
                player.getInventory().setItemInMainHand(builder.toItem());
                builder.openGui(player);
                return;
            }
            case 2 -> {
                MonsterSpawnerType typeArg = MonsterSpawnerTypeManager.getInstance().get(args[1]);
                if (typeArg == null) {
                    sender.sendMessage("Message not implemented yet (invalid type)");//TODO
                    return;
                }
                MonsterSpawnerType typeItem = MonsterSpawnerTypeManager.getInstance().getMonsterSpawnerType(item);
                if (typeItem == null) {
                    if (UtilsInventory.isAirOrNull(item)) {
                        typeItem = typeArg;
                        item = typeArg.getBuilder().toItem();
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        sender.sendMessage("Message not implemented yet (hand must be monsterspawner blueprint or empty)");//TODO
                        return;
                    }
                }
                if (typeArg != typeItem) {
                    sender.sendMessage("Message not implemented yet (hand monsterspawner blueprint and argument monsterspawner mismatch)");//TODO
                    return;
                }
                MonsterSpawnerType.MonsterSpawnerInstanceBuilder builder = MonsterSpawnerTypeManager.getInstance().getMonsterSpawnerInstance(item);
                builder.openGui(player);
            }
        }
        sender.sendMessage("Message not implemented yet (create command help");//TODO
    }

    @Override
    public @Nullable
    List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        return switch (args.length) {
            case 1 -> this.complete(args[0], new String[]{"create"});
            case 2 -> {
                if (args[0].equalsIgnoreCase("create"))
                    yield this.complete(args[1], MonsterSpawnerTypeManager.getInstance().getIds());
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }
}
