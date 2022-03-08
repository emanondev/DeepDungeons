package emanondev.deepdungeons.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import emanondev.core.CoreCommand;
import emanondev.core.MessageBuilder;
import emanondev.core.PermissionBuilder;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.room.RoomBuilder;
import net.md_5.bungee.api.chat.ClickEvent.Action;

public class DungeonRoomCommand extends CoreCommand {

	public DungeonRoomCommand() {
		super("DungeonRoom", DeepDungeons.get(),
				PermissionBuilder.ofCommand(DeepDungeons.get(), "DungeonRoom").buildAndRegister(DeepDungeons.get()),
				"room managing", Arrays.asList("droom"));
	}

	@Override
	public void onExecute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
		if (!(sender instanceof Player)) {
			this.playerOnlyNotify(sender);
			return;
		}
		Player p = (Player) sender;
		if (args.length == 0) {
			help(p, alias, args);
			return;
		}
		switch (args[0].toLowerCase()) {
		case "next":
			try {
				RoomBuilder.next(p);
			} catch (IllegalArgumentException e) {
				new MessageBuilder(getPlugin(), p).addTextTranslation("command.DungeonRoom.next.error",
						Arrays.asList("&4[&cDungeonRoom&4] &cYou are not creating a room")).send();
			}
			return;
		case "abort":
			try {
				RoomBuilder.abort(p);
			} catch (IllegalArgumentException e) {
				new MessageBuilder(getPlugin(), p).addTextTranslation("command.DungeonRoom.abort.error",
						Arrays.asList("&4[&cDungeonRoom&4] &cYou are not creating a room")).send();
			}
			return;
		case "start":
			try {
				new RoomBuilder(p);
			} catch (IllegalArgumentException e) {
				new MessageBuilder(getPlugin(), p).addTextTranslation("command.DungeonRoom.start.error",
						Arrays.asList("&4[&cDungeonRoom&4] &cYou are already creating a room")).send();
			}
			return;
		default: {
			help(p, alias, args);
			return;
		}
		}
	}

	private void help(Player p, @NotNull String alias, @NotNull String[] args) {
		new MessageBuilder(getPlugin(), p)
		.addFullComponentTranslation("command.DungeonRoom.help.start", Arrays.asList("&9/%alias% &bstart"),
				Arrays.asList("&6Click to run"), "/%alias% start", Action.RUN_COMMAND, "%alias%", alias)
		.addText("\n")
				.addFullComponentTranslation("command.DungeonRoom.help.next", Arrays.asList("&9/%alias% &bnext"),
						Arrays.asList("&6Click to run"), "/%alias% next", Action.RUN_COMMAND, "%alias%", alias)
				.addText("\n")
				.addFullComponentTranslation("command.DungeonRoom.help.abort", Arrays.asList("&9/%alias% &babort"),
						Arrays.asList("&6Click to run"), "/%alias% abort", Action.RUN_COMMAND, "%alias%", alias)
				.send();
	}

	@Override
	public List<String> onComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args,
			@Nullable Location loc) {
		switch (args.length) {
		case 1:
			return this.complete(args[0], Arrays.asList("next", "abort","start"));
		}
		return Collections.emptyList();
	}

}
