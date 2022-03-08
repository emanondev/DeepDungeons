package emanondev.deepdungeons.mob;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import emanondev.deepdungeons.generic.Provider;
import emanondev.deepdungeons.generic.StandGui;
import emanondev.deepdungeons.room.handler.RoomHandler;

public interface MobProvider extends Provider {

	public void spawn(List<String> info, Location loc, RoomHandler handler);

	public StandGui setupGui(Player user,ArmorStand stand);
	
	
}
