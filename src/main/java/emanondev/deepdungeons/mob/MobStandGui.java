package emanondev.deepdungeons.mob;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import emanondev.deepdungeons.generic.AStandGui;
import emanondev.deepdungeons.parameter.*;

public abstract class MobStandGui extends AStandGui {
	

	public MobStandGui(Player player, ArmorStand stand, MobProvider provider) {
		super(MobManager.NAME, player, stand, provider);
	}

	public void registerParams() {
		registerParam(Parameters.AMOUNT,9);
		registerParam(Parameters.SPREAD,10);
		registerParam(Parameters.CHANCE,17);
	}
	
	public MobProvider getProvider() {
		return (MobProvider) super.getProvider();
	}

}