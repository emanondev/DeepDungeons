package emanondev.deepdungeons.reward;

import java.util.List;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import emanondev.core.gui.Gui;
import emanondev.deepdungeons.generic.Provider;
import emanondev.deepdungeons.room.handler.RoomHandler;

public interface RewardProvider extends Provider {

	public void populate(List<String> info, Inventory inventory, RoomHandler handler);

	public Gui setupGui(Player user, ArmorStand stand);

}
