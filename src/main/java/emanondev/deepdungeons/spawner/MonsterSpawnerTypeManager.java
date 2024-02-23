package emanondev.deepdungeons.spawner;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;

public class MonsterSpawnerTypeManager extends DRegistry<MonsterSpawnerType> {

    private static MonsterSpawnerTypeManager instance;

    public static MonsterSpawnerTypeManager getInstance() {
        return instance;
    }

    public MonsterSpawnerTypeManager() {
        super(DeepDungeons.get(), "MonsterSpawnerManager", true);
    }

}
