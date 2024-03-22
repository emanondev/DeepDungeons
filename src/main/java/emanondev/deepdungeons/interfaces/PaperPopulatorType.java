package emanondev.deepdungeons.interfaces;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PaperPopulatorType extends PopulatorType {


    @NotNull
    PaperPopulatorBuilder getPaperBuilder();

    interface PaperPopulatorBuilder {

        boolean preserveContainer();

        @NotNull
        default ItemStack toItem() {
            return new ItemBuilder(Material.PAPER).setDescription(toItemLines()).build();
        }

        /**
         * @return a mutable list with prefilled first two lines
         */
        @NotNull
        List<String> toItemLines();

        @Nullable
        Location getOffset();

        void setOffset(@NotNull Location offset);

        void writeTo(@NotNull YMLSection section) throws Exception;

        @Contract("_ -> this")
        PaperPopulatorBuilder fromItemLines(@NotNull List<String> lines);

        void openGui(Player player);
    }
}
