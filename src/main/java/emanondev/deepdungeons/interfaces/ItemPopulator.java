package emanondev.deepdungeons.interfaces;

import emanondev.deepdungeons.event.PopulatorGenerateLootEvent;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public interface ItemPopulator extends PopulatorType.PopulatorInstance {
    default Map<Location, Collection<ItemStack>> getItems(@NotNull RoomHandler handler, @Nullable Player who) {
        return getItems(handler, who, new Random());
    }

    Map<Location, Collection<ItemStack>> getItems(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random);

    default void populate(@NotNull RoomHandler handler, @Nullable Player who) {
        populate(handler, who, new Random());

    }

    default void populate(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
        PopulatorGenerateLootEvent event = new PopulatorGenerateLootEvent(handler, this, getItems(handler, who, random));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        event.getDrops().forEach((to, drops) -> {
            if (to.getBlock().getState() instanceof Container container) {
                Inventory inv = container.getInventory();
                drops.forEach(item -> {
                    if (item != null && !item.getType().isAir())
                        inv.addItem(item);
                });
                ItemStack[] stacks = inv.getContents();
                List<ItemStack> contained = Arrays.asList(stacks);
                Collections.shuffle(contained);
                inv.setContents(contained.toArray(stacks));
            } else
                drops.forEach(item -> {
                    if (item != null && !item.getType().isAir())
                        to.getWorld().dropItem(to, item);
                });
        });
    }
}
