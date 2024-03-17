package emanondev.deepdungeons;

import emanondev.core.util.DRegistryElement;
import org.jetbrains.annotations.NotNull;

public class DInstance<T extends DRegistryElement> {

    private final T type;

    public DInstance(@NotNull T type) {
        this.type = type;
    }

    public final @NotNull
    T getType() {
        return type;
    }
}
