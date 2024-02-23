package emanondev.deepdungeons.treasure;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;

public class TreasureTypeManager extends DRegistry<TreasureType> {

    private static TreasureTypeManager instance;

    public static TreasureTypeManager getInstance() {
        return instance;
    }

    public TreasureTypeManager() {
        super(DeepDungeons.get(), "TreasureManager", true);
    }

}
