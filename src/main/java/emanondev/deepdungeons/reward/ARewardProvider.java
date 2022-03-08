package emanondev.deepdungeons.reward;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import emanondev.deepdungeons.generic.AProvider;

public abstract class ARewardProvider  extends AProvider implements RewardProvider {

	public ARewardProvider(String id) {
		super(id);
	}

	@Override
	public abstract RewardStandGui setupGui(Player user, ArmorStand stand);

}
