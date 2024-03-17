package emanondev.deepdungeons.command;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsInventory;
import emanondev.core.command.CoreCommand;
import emanondev.core.gui.PagedListFGui;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.treasure.TreasureType;
import emanondev.deepdungeons.treasure.TreasureTypeManager;
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

public class DungeonTreasureCommand extends CoreCommand {
    public DungeonTreasureCommand() {
        super("dungeontreasure", DeepDungeons.get(), Perms.DUNGEONTREASURE_COMMAND, "create blueprint", List.of("dtreasure"));
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
                TreasureType.TreasureInstanceBuilder builder = TreasureTypeManager.getInstance().getTreasureInstance(item);
                if (builder == null && !UtilsInventory.isAirOrNull(item)) {
                    sender.sendMessage("Message not implemented yet (hand must be a treasure blueprint or empty)");//TODO
                    return;
                }
                if (builder == null) {
                    new PagedListFGui<>(new DMessage(DeepDungeons.get()).append("&9Treasure Type Selector").toLegacy(),
                            6, player, null, DeepDungeons.get(), false,
                            (InventoryClickEvent click, TreasureType type) -> {
                                TreasureType.TreasureInstanceBuilder bb = type.getBuilder();
                                bb.openGui(player);
                                player.getInventory().setItemInMainHand(bb.toItem());
                                return false;
                            },
                            (TreasureType type) -> new ItemBuilder(Material.CHEST)
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
                TreasureType typeArg = TreasureTypeManager.getInstance().get(args[1]);
                if (typeArg == null) {
                    sender.sendMessage("Message not implemented yet (invalid type)");//TODO
                    return;
                }
                TreasureType typeItem = TreasureTypeManager.getInstance().getTreasureType(item);
                if (typeItem == null) {
                    if (UtilsInventory.isAirOrNull(item)) {
                        typeItem = typeArg;
                        item = typeArg.getBuilder().toItem();
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        sender.sendMessage("Message not implemented yet (hand must be treasure blueprint or empty)");//TODO
                        return;
                    }
                }
                if (typeArg != typeItem) {
                    sender.sendMessage("Message not implemented yet (hand treasure blueprint and argument treasure mismatch)");//TODO
                    return;
                }
                TreasureType.TreasureInstanceBuilder builder = TreasureTypeManager.getInstance().getTreasureInstance(item);
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
                    yield this.complete(args[1], TreasureTypeManager.getInstance().getIds());
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }
}
