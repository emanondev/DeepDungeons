package emanondev.deepdungeons.mob;

import emanondev.deepdungeons.generic.Provider;
import emanondev.deepdungeons.generic.StandGui;
import emanondev.deepdungeons.room.handler.RoomHandler;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;

public interface MobProvider extends Provider {

    void spawn(List<String> info, Location loc, RoomHandler handler);

    StandGui setupGui(Player user, ArmorStand stand);


}
