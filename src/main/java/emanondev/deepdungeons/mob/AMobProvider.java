package emanondev.deepdungeons.mob;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import emanondev.deepdungeons.generic.AProvider;

public abstract class AMobProvider extends AProvider implements MobProvider {

	public AMobProvider(String id) {
		super(id);
	}

	@Override
	public abstract MobStandGui setupGui(Player user, ArmorStand stand);
}
