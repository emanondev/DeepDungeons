package emanondev.deepdungeons.reward;

import emanondev.deepdungeons.generic.AStandGui;
import emanondev.deepdungeons.parameter.Parameters;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class RewardStandGui extends AStandGui {


    public RewardStandGui(Player player, ArmorStand stand, RewardProvider provider) {
        super(RewardManager.NAME, player, stand, provider);
    }

    public void registerParams() {
        registerParam(Parameters.CHANCE, 17);
    }

    public RewardProvider getProvider() {
        return (RewardProvider) super.getProvider();
    }
}