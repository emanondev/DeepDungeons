package emanondev.deepdungeons.reward;

import emanondev.deepdungeons.generic.AProvider;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public abstract class ARewardProvider extends AProvider implements RewardProvider {

    public ARewardProvider(String id) {
        super(id);
    }

    @Override
    public abstract RewardStandGui setupGui(Player user, ArmorStand stand);

}
