package emanondev.deepdungeons.mob;

import emanondev.deepdungeons.generic.AProviderManager;

public class MobManager extends AProviderManager<MobProvider> {

    public static final String NAME = "Mob";

    public MobManager() {
        try {
            this.register(new VanillaMobProvider());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.register(new MythicMobsMobProvider());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
