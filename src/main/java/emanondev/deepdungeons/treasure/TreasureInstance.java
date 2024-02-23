package emanondev.deepdungeons.treasure;

import emanondev.deepdungeons.DInstance;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;

public abstract class TreasureInstance extends DInstance<TreasureType> {

    public TreasureInstance(@NotNull TreasureType type){
        super(type);
    }


    /*public final @NotNull Collection<ItemStack> getTreasure(){
        return this.getTreasure(new Random());
    }*/

    /**
     * Return a collection of not null ItemStacks, collection may be empty
     * <br><br>Implementation note: generated treasures should be consistent with given random
     * @param random seed for generation
     * @param location where the treasure is generated
     * @param who optional who's getting the treasure
     * @return A collection of not null ItemStacks, collection may be empty
     */
    public abstract @NotNull Collection<ItemStack> getTreasure(@NotNull Random random, @NotNull Location location, @Nullable Player who);

}
