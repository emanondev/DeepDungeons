package emanondev.deepdungeons.trap;

import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.trap.impl.FlameChestType;
import emanondev.deepdungeons.trap.impl.HiddenPassageType;
import org.jetbrains.annotations.NotNull;

public class TrapTypeManager extends DRegistry<TrapType> {

    private static final TrapTypeManager instance = new TrapTypeManager();

    private TrapTypeManager() {
        super(DeepDungeons.get(), "TreasureManager", true);
        register(new FlameChestType());
        register(new HiddenPassageType());
    }

    @NotNull
    public static TrapTypeManager getInstance() {
        return instance;
    }

}
