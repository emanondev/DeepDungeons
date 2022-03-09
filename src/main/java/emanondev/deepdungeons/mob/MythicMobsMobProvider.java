package emanondev.deepdungeons.mob;

import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.generic.AProvider;
import emanondev.deepdungeons.parameter.Parameters;
import emanondev.deepdungeons.room.handler.RoomHandler;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class MythicMobsMobProvider extends AProvider implements MobProvider {

    public MythicMobsMobProvider() {
        super("mythicmobs");
    }

    @Override
    public void spawn(List<String> info, Location loc, RoomHandler handler) {
        try {

            int chance = Parameters.CHANCE.readValue(info);
            if (chance < 100 && Math.random() * 100 > chance)
                return;
            MythicMob type = Parameters.MYTHICMOBTYPE.readValue(info);
            int amount = Parameters.AMOUNT.readValue(info);
            double spread = Parameters.SPREAD.readValue(info);
            int level = Parameters.LEVEL.readValue(info);
            for (int i = 0; i < amount; i++)
                MythicMobs.inst().getAPIHelper().spawnMythicMob(type, spread(loc, spread), level);
        } catch (Exception e) {
            DeepDungeons.get().logIssue("Unable to spawn MythicMob &e" + Arrays.toString(info.toArray()));
        }
    }

    private Location spread(Location loc, double spread) {
        if (spread == 0)
            return loc;
        return loc.clone().add(Math.random() * 2 * spread - spread, 0, Math.random() * 2 * spread - spread);
    }

    private class MythicMobsStandGui extends MobStandGui {

        public MythicMobsStandGui(Player player, ArmorStand stand) {
            super(player, stand, MythicMobsMobProvider.this);
        }

        public void registerParams() {
            super.registerParams();
            this.registerParam(Parameters.MYTHICMOBTYPE, 0);
        }

    }

    @Override
    public MobStandGui setupGui(Player user, ArmorStand stand) {
        return new MythicMobsStandGui(user, stand);
    }

}
