package emanondev.deepdungeons.populator;

import emanondev.core.YMLSection;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.message.DMessage;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.interfaces.PopulatorType;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class APopulatorType extends DRegistryElement implements PopulatorType {

    public APopulatorType(@NotNull String id) {
        super(id);
    }

    @NotNull
    public abstract APopulatorInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub);

    @NotNull
    public abstract APopulatorBuilder getBuilder(@NotNull RoomBuilder room);

    @NotNull
    public DMessage getDescription(@NotNull Player player) {
        return new DMessage(DeepDungeons.get(), player).append("<red>Description of <gold>" + getId() + "</gold> not implemented</red>");//TODO
    }

    protected boolean hasUseChance() {
        return true;
    }

    public abstract class APopulatorBuilder extends BuilderBase implements PopulatorBuilder {
        private final CompletableFuture<PopulatorBuilder> completableFuture = new CompletableFuture<>();
        private final RoomBuilder roomBuilder;

        protected APopulatorBuilder(@NotNull RoomBuilder room) {
            super();
            this.roomBuilder = room;
        }

        @Override
        @NotNull
        public CompletableFuture<PopulatorBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public void abort() {
            completableFuture.completeExceptionally(new Exception("aborted"));
        }

        public void complete() {
            completableFuture.complete(this);
        }

        @Override
        @NotNull
        public RoomBuilder getRoomBuilder() {
            return roomBuilder;
        }

        @Override
        public void setupTools() {
            Player player = getPlayer();
            PlayerInventory inv = player.getInventory();
            inv.setItem(3, CUtils.createItem(player, Material.CHEST, "populatorbuilder.base_item_settings"));
            this.setupToolsImpl(inv, player);
        }

        protected abstract void handleInteractImpl(@NotNull PlayerInteractEvent event);

        protected abstract void setupToolsImpl(@NotNull PlayerInventory inv, @NotNull Player player);

        @Override
        public void handleInteract(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 0 -> {
                    return;
                }
                case 3 -> {
                    openGui(event.getPlayer());
                    return;
                }
            }
            this.handleInteractImpl(event);
        }

        @Override
        public void timerTick(@NotNull Player player, @NotNull Color color) {
            this.tickTimerImpl(player, color);
        }

        protected abstract void tickTimerImpl(@NotNull Player player, @NotNull Color color);

        protected void craftGuiButtons(@NotNull PagedMapGui gui, @NotNull Player player) {
            craftGuiButtonsImpl(gui, player);
            if (hasUseChance())
                gui.addButton(new NumberEditorFButton<>(gui, 1D, 0.01D, 100D,
                        () -> getUseChance() * 100D, (val) -> setUseChance(val / 100D),
                        () -> CUtils.createItem(player, Material.REPEATER, "populatorbuilder.settings_usechance",
                                "%value%", CUtils.chanceToText(getUseChance())), true));
        }

        protected abstract void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player);

    }

    public abstract class BuilderBase extends DInstance<APopulatorType> {
        private double useChance = 1;

        public BuilderBase() {
            super(APopulatorType.this);
        }

        public final void writeTo(@NotNull YMLSection section) throws Exception {
            section.set("type", getType().getId());
            writeToImpl(section);
            if (hasUseChance())
                section.set("useChance", useChance);
        }

        protected abstract void writeToImpl(@NotNull YMLSection section) throws Exception;

        public void openGui(Player player) {
            craftGui(player).open(player);
        }

        public double getUseChance() {
            return hasUseChance() ? useChance : 1;
        }

        public void setUseChance(double value) {
            useChance = CUtils.bound(value, 0D, 1D);
        }


        protected PagedMapGui craftGui(@NotNull Player player) {
            PagedMapGui gui = new PagedMapGui(new DMessage(DeepDungeons.get()).appendLang("populatorbuilder.settings_guititle",
                    "%type%", APopulatorType.this.getId()), 6, player, null, DeepDungeons.get());
            craftGuiButtons(gui, player);
            return gui;
        }

        protected abstract void craftGuiButtons(@NotNull PagedMapGui gui, @NotNull Player player);

    }

    public abstract class APopulatorInstance extends DInstance<APopulatorType> implements PopulatorInstance {


        private final RoomInstance room;
        //private final Vector offset;
        //private final Vector direction;
        private final double useChance;

        public APopulatorInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(APopulatorType.this);
            this.room = room;
            this.useChance = hasUseChance() ? CUtils.bound(section.getDouble("useChance", 1), 0D, 1D) : 1;
            //this.offset = Util.toVector(section.getString("offset"));
            //this.direction = Util.toVector(section.getString("direction"));
        }

        public boolean rollUseChance() {
            return !hasUseChance() || Math.random() > getUseChance();
        }

        public double getUseChance() {
            return hasUseChance() ? useChance : 1;
        }


        /*
        @Contract("-> new")
        @NotNull
        public Vector getOffset() {
            return offset.clone();
        }

        @Contract("-> new")
        @NotNull
        public Vector getDirection() {
            return direction.clone();
        }*/

        @NotNull
        @Override
        public RoomInstance getRoomInstance() {
            return room;
        }

    }

}
