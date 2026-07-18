package emanondev.deepdungeons.command;

import emanondev.core.ItemBuilder;
import emanondev.core.command.CoreCommand;
import emanondev.core.gui.PagedListFGui;
import emanondev.core.message.DMessage;
import emanondev.core.utility.InventoryUtility;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Perms;
import emanondev.deepdungeons.interfaces.PaperPopulatorType;
import emanondev.deepdungeons.interfaces.PaperPopulatorType.PaperPopulatorBuilder;
import emanondev.deepdungeons.populator.PopulatorTypeManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DungeonPopulatorCommand extends CoreCommand {
    public DungeonPopulatorCommand() {
        super("dungeonpopulator", DeepDungeons.get(), Perms.DUNGEONMONSTERSPAWNER_COMMAND, "create blueprint", List.of("dpopulator"));
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

    @Override
    @Nullable
    public List<String> onComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args, @Nullable Location location) {
        return switch (args.length) {
            case 1 -> this.complete(args[0], "create");
            case 2 -> {
                if (args[0].equalsIgnoreCase("create"))
                    yield this.complete(args[1], PopulatorTypeManager.getInstance().getPaperIds());
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    private void help(CommandSender sender, String label, String[] args) {
        new DMessage(getPlugin(), sender).appendLang("commands.dpopulator.help").send();
    }

    private void create(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            this.playerOnlyNotify(sender);
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        switch (args.length) {
            case 1 -> {
                PaperPopulatorBuilder builder = PopulatorTypeManager.getInstance().getPaperPopulatorBuilder(item);
                if (builder == null && !InventoryUtility.isAirOrNull(item)) {
                    new DMessage(getPlugin(), sender).appendLang("commands.dpopulator.wrong_hand").send();
                    return;
                }
                if (builder == null) {
                    PagedListFGui<PaperPopulatorType> gui = new PagedListFGui<>(new DMessage(DeepDungeons.get()).append("&9Populator Type Selector").toLegacy(),
                            6, player, null, DeepDungeons.get(), false,
                            (InventoryClickEvent click, PaperPopulatorType type) -> {
                                PaperPopulatorBuilder bb = type.getPaperBuilder();
                                bb.openGui(player);
                                player.getInventory().setItemInMainHand(bb.toItem());
                                return false;
                            },
                            (PaperPopulatorType type) -> new ItemBuilder(Material.CHEST)
                                    .setDescription(new DMessage(DeepDungeons.get(), player)
                                            .append("<blue>Type: <gold>" + type.getId() + "</gold></blue>").newLine().append(
                                                    type.getDescription(player)
                                            )).build());
                    List<PaperPopulatorType> values = new ArrayList<>();
                    PopulatorTypeManager.getInstance().getAll(pop -> pop instanceof PaperPopulatorType).forEach(pop -> values.add((PaperPopulatorType) pop));
                    gui.addElements(values);
                    gui.open(player);
                    return;
                }
                player.getInventory().setItemInMainHand(builder.toItem());
                builder.openGui(player);
                return;
            }
            case 2 -> {
                PaperPopulatorType typeArg = PopulatorTypeManager.getInstance().getPaper(args[1]);
                if (typeArg == null) {
                    StringBuilder allowedTypes = new StringBuilder();
                    for (String type : PopulatorTypeManager.getInstance().getPaperIds()) {
                        allowedTypes.append(type).append(", ");
                    }
                    new DMessage(getPlugin(), sender).appendLang("commands.dpopulator.wrong_type", "%types%", allowedTypes.toString()).send();

                    return;
                }
                PaperPopulatorType typeItem = PopulatorTypeManager.getInstance().getPaperPopulatorType(item);
                if (typeItem == null) {
                    if (InventoryUtility.isAirOrNull(item)) {
                        typeItem = typeArg;
                        item = typeArg.getPaperBuilder().toItem();
                        player.getInventory().setItemInMainHand(item);
                    } else {
                        new DMessage(getPlugin(), sender).appendLang("commands.dpopulator.wrong_hand").send();
                        return;
                    }
                }
                if (typeArg != typeItem) {
                    new DMessage(getPlugin(), sender).appendLang("commands.dpopulator.populator_mismatch").send();
                    return;
                }
                PaperPopulatorBuilder builder = PopulatorTypeManager.getInstance().getPaperPopulatorBuilder(item);
                builder.openGui(player);
            }
        }
        new DMessage(getPlugin(), sender).appendLang("commands.dpopulator.help").send();
    }
}
