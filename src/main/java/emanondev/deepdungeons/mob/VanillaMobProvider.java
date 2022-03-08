package emanondev.deepdungeons.mob;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.generic.AProvider;
import emanondev.deepdungeons.parameter.Parameters;
import emanondev.deepdungeons.room.handler.RoomHandler;

public class VanillaMobProvider extends AProvider implements MobProvider {

	public VanillaMobProvider() {
		super("vanilla");
	}

	@Override
	public void spawn(List<String> info, Location loc, RoomHandler handler) {
		try {
			int chance = Parameters.CHANCE.readValue(info);
			if (chance<100 && Math.random()*100>chance)
				return;
			EntityType type = Parameters.ENTITYTYPE.readValue(info);
			int amount = Parameters.AMOUNT.readValue(info);
			double spread = Parameters.SPREAD.readValue(info);

			for (int i = 0; i < amount; i++)
				loc.getWorld().spawn(spread(loc, spread), type.getEntityClass());
		} catch (Exception e) {
			DeepDungeons.get().logIssue("Unable to spawn Vanilla Mob &e"+Arrays.toString(info.toArray()));
		}
	}

	private Location spread(Location loc, double spread) {
		if (spread == 0)
			return loc;
		return loc.clone().add(Math.random() * 2 * spread - spread, 0, Math.random() * 2 * spread - spread);
	}

	@Override
	public MobStandGui setupGui(Player user, ArmorStand stand) {
		return new VanillaStandGui(user,stand);
	}

	public class VanillaStandGui extends MobStandGui {

		public VanillaStandGui(Player player, ArmorStand stand) {
			super(player, stand, VanillaMobProvider.this);
		}

		public void registerParams() {
			super.registerParams();
			this.registerParam(Parameters.ENTITYTYPE,0);
		}

	}

}
