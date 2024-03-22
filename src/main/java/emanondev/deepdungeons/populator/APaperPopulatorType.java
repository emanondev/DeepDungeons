package emanondev.deepdungeons.populator;

import emanondev.core.UtilsString;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.interfaces.PaperPopulatorType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class APaperPopulatorType extends APopulatorType implements PaperPopulatorType {

    public APaperPopulatorType(@NotNull String id) {
        super(id);
    }

    public abstract class APaperPopulatorBuilder extends BuilderBase implements PaperPopulatorType.PaperPopulatorBuilder {

        private Location offset;

        protected APaperPopulatorBuilder() {
            super();
        }

        @NotNull
        public final ItemStack toItem() {
            return PaperPopulatorBuilder.super.toItem();
        }

        @NotNull
        protected abstract List<String> toItemLinesImpl();

        /**
         * @return a mutable list with prefilled first two lines
         */
        @NotNull
        public final List<String> toItemLines() {
            ArrayList<String> list = new ArrayList<>();
            list.add(PopulatorTypeManager.LINE_ONE);
            list.add("&9Type:&6 " + getType().getId());
            list.addAll(toItemLinesImpl());
            if (hasUseChance())
                list.add("&9UseChance:&e " + UtilsString.formatOptional2Digit(getUseChance()*100D).replace(",","."));
            return list;
        }


        @Override
        @Contract("_ -> this")
        public final APaperPopulatorBuilder fromItemLines(@NotNull List<String> lines) {
            lines = new ArrayList<>(lines);
            lines.remove(0);
            if (hasUseChance())
                setUseChance(Double.parseDouble(lines.remove(lines.size() - 1).split(" ")[1])/100);
            fromItemLinesImpl(lines);
            return this;
        }

        protected abstract void fromItemLinesImpl(@NotNull List<String> lines);


        @Nullable
        public Location getOffset() {
            return offset.clone();
        }

        public void setOffset(@NotNull Location offset) {
            this.offset = offset.clone();
            this.offset.setWorld(null);
        }


        protected PagedMapGui craftGui(@NotNull Player player) {
            PagedMapGui gui = new PagedMapGui(CUtils.craftMsg(player,"populatorbuilder.settings_guititle",
                    "%type%", APaperPopulatorType.this.getId()), 6, player, null, DeepDungeons.get()) {
                @Override
                public void onClose(@NotNull InventoryCloseEvent event) {
                    event.getPlayer().getInventory().setItemInMainHand(toItem());
                }
            };
            craftGuiButtons(gui, player);
            return gui;
        }


        protected final void craftGuiButtons(@NotNull PagedMapGui gui, @NotNull Player player) {
            craftGuiButtonsImpl(gui, player);
            if (hasUseChance())
                gui.addButton(new NumberEditorFButton<>(gui, 1D, 0.01D, 100D,
                        () -> getUseChance() * 100D, (val) -> setUseChance(val / 100),
                        () -> CUtils.createItem(player, Material.REPEATER, "populatorbuilder.settings_usechance",
                                "%value%", CUtils.chanceToText(getUseChance())), true));
        }

        protected abstract void craftGuiButtonsImpl(PagedMapGui gui, Player player);

    }

}
