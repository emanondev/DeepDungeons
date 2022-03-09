package emanondev.deepdungeons.reward;

import emanondev.core.gui.Gui;
import emanondev.deepdungeons.generic.Provider;
import emanondev.deepdungeons.room.handler.RoomHandler;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public interface RewardProvider extends Provider {

    void populate(List<String> info, Inventory inventory, RoomHandler handler);

    Gui setupGui(Player user, ArmorStand stand);

}
