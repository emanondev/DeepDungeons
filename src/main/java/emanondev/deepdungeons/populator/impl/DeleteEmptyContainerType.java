package emanondev.deepdungeons.populator.impl;

import emanondev.core.YMLSection;
import emanondev.core.gui.PagedMapGui;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.BlockPopulator;
import emanondev.deepdungeons.populator.APaperPopulatorType;
import emanondev.deepdungeons.populator.PopulatorPriority;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DeleteEmptyContainerType extends APaperPopulatorType {

    public DeleteEmptyContainerType() {
        super("deleteemptycontainer");
    }

    @Override
    @NotNull
    public DeleteEmptyContainerInstance read(@NotNull RoomInstance room, @NotNull YMLSection sub) {
        return new DeleteEmptyContainerInstance(room, sub);
    }

    @NotNull
    @Override
    public APopulatorBuilder getBuilder(@NotNull RoomType.RoomBuilder room) {
        return new DeleteEmptyContainerBuilder(room);
    }

    @Override
    @NotNull
    public DeleteEmptyContainerPaperBuilder getPaperBuilder() {
        return new DeleteEmptyContainerPaperBuilder();
    }

    private class DeleteEmptyContainerBuilder extends APopulatorBuilder {
        private final List<Location> offsets = new ArrayList<>();

        public DeleteEmptyContainerBuilder(@NotNull RoomType.RoomBuilder room) {
            super(room);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    if (event.getClickedBlock() == null)
                        return;
                    if (event.getClickedBlock().getState() instanceof Container)
                        this.toggleOffset(event.getClickedBlock().getLocation());
                    else
                        this.toggleOffset(event.getClickedBlock().getRelative(event.getBlockFace()).getLocation());
                    this.getRoomBuilder().setupTools();
                }
                case 6 -> {
                    this.complete();
                    this.getRoomBuilder().setupTools();
                }
            }
        }

        @Override
        protected void setupToolsImpl(@NotNull PlayerInventory inv, @NotNull Player player) {
            inv.setItem(0, CUtils.createItem(player, Material.PAPER, "populatorbuilder.deleteemptycontainer_info"));
            inv.setItem(1, CUtils.createItem(player, Material.STICK, offsets.size(), false, "populatorbuilder.deleteemptycontainer_selector"));
            inv.setItem(6, CUtils.createItem(player, Material.LIME_DYE, "populatorbuilder.base_confirm"));
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            offsets.forEach(loc -> CUtils.markBlock(player, loc.toVector().add(getRoomOffset()).toBlockVector(), color));
        }

        @Override
        protected void craftGuiButtonsImpl(@NotNull PagedMapGui gui, @NotNull Player player) {

        }

        public void toggleOffset(@NotNull Location location) {
            location = location.clone();
            location.setWorld(null);
            location.subtract(getRoomOffset());
            location.setX(location.getBlockX() + 0.5D);
            location.setY(location.getBlockY());
            location.setZ(location.getBlockZ() + 0.5D);
            if (!offsets.remove(location))
                offsets.add(location);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {

        }
    }

    private class DeleteEmptyContainerPaperBuilder extends APaperPopulatorBuilder {

        @Override
        public boolean preserveContainer() {
            return true;
        }

        @Override
        @NotNull
        protected List<String> toItemLinesImpl() {
            return Collections.emptyList();
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            Location offset = getOffset();
            if (offset == null)
                throw new Exception("Location not set");
            section.set("offsets", List.of(Util.toStringNoWorld(offset)));
        }

        @Override
        public void fromItemLinesImpl(@NotNull List<String> lines) {
        }

        @Override
        protected void craftGuiButtonsImpl(PagedMapGui gui, Player player) {

        }

    }

    private class DeleteEmptyContainerInstance extends APopulatorInstance implements BlockPopulator {


        private final List<Location> offsets = new ArrayList<>();

        public DeleteEmptyContainerInstance(RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            section.getStringList("offsets", Collections.emptyList()).forEach(val -> offsets.add(Util.toLocationNoWorld(val)));
        }

        @NotNull
        @Override
        public List<BlockState> getChangingBlocks(@NotNull RoomHandler handler, @Nullable Player who, @NotNull Random random) {
            List<BlockState> states = new ArrayList<>();
            offsets.forEach(offset -> {
                BlockState b = CUtils.sum(handler.getLocation(),offset).getBlock().getState();
                if (b instanceof Container cont && cont.getInventory().isEmpty()) {
                    if (b instanceof Waterlogged waterlogged && waterlogged.isWaterlogged())
                        b.setType(Material.WATER);
                    else
                        b.setType(Material.AIR);
                    states.add(b);
                }
            });
            return states;
        }

        @Override
        public PopulatorPriority getPriority(){
            return PopulatorPriority.HIGHEST;
        }
    }
}
