package emanondev.deepdungeons;

import emanondev.core.util.DRegistryElement;
import org.jetbrains.annotations.NotNull;

public class DRInstance<T extends DRegistryElement> extends DRegistryElement{

    private final T type;

    public DRInstance(@NotNull String id, @NotNull T type){
        super(id);
        this.type = type;
    }

    public final @NotNull T getType(){
        return type;
    }
}
