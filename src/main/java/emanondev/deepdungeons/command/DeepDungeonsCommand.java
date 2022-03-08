package emanondev.deepdungeons.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;

import emanondev.core.CoreCommand;
import emanondev.core.PermissionBuilder;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DeepDungeons;

public class DeepDungeonsCommand extends CoreCommand {
	
	private static final DeepDungeons plugin = DeepDungeons.get();

	public DeepDungeonsCommand() {
		super("DeepDungeons", plugin, PermissionBuilder.ofCommand(plugin, "DeepDungeons").buildAndRegister(plugin),"Allow to use the command /DeepDungeons");
	}

	@Override
	public void onExecute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
		Player p = (Player) sender;
		BukkitPlayer wp = BukkitAdapter.adapt(p);
		
		switch (args[0].toLowerCase()) {
		case "crea":
			try {
				Region sel = WorldEdit.getInstance().getSessionManager().get(wp).getSelection(BukkitAdapter.adapt(p.getWorld()));
				Clipboard clip = WorldEditUtility.copy(new Location(p.getWorld(),
						sel.getMinimumPoint().getBlockX(),
						sel.getMinimumPoint().getBlockY(),
						sel.getMinimumPoint().getBlockZ()), new Location(p.getWorld(),
								sel.getMaximumPoint().getBlockX(),
								sel.getMaximumPoint().getBlockY(),
								sel.getMaximumPoint().getBlockZ()), true,true);
				plugin.getRoomManager().createRoom(args[1],clip);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		case "paste":
			k=k+90;
			if (DeepDungeons.DEBUG)
				DeepDungeons.get().log(this.getClass().getSimpleName()+" -> rotation "+k);
			
			plugin.getRoomManager().getRoom(args[1]).paste(p.getLocation(),k);
			return;
		default:
			throw new IllegalArgumentException();
		}
	}
	private int k = 0;

	@Override
	public List<String> onComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args,
			@Nullable Location loc) {
		switch (args.length) {
		case 0:
			return this.complete(args[0], new String[]{"crea","paste"});
		}
		return null;
	}

}
