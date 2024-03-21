package emanondev.deepdungeons.interfaces;

import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface ItemPopulator extends RoomPopulator {
    default Collection<ItemStack> getItems(@NotNull RoomHandler handler, @NotNull Location location, @Nullable Player who) {
        return getItems(handler, location, who, new Random());
    }

    Collection<ItemStack> getItems(@NotNull RoomHandler handler, @NotNull Location location, @Nullable Player who, @NotNull Random random);

    default void populate(@NotNull RoomHandler handler, @Nullable Player who) {
        populate(handler, who, new Random());

    }

    default void populate(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
        Location to = handler.getLocation().add(getOffset());
        if (to.getBlock().getState() instanceof Container container) {
            Inventory inv = container.getInventory();
            inv.addItem(getItems(handler, to, who, new Random()).toArray(new ItemStack[0]));
            ItemStack[] stacks = inv.getContents();
            List<ItemStack> contained = Arrays.asList(stacks);
            Collections.shuffle(contained);
            inv.setContents(contained.toArray(stacks));
        } else
            getItems(handler, to, who, new Random()).forEach(itemStack -> to.getWorld().dropItem(to, itemStack));
    }
}
