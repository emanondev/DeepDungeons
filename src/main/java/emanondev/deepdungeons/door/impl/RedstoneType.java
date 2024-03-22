package emanondev.deepdungeons.door.impl;

import emanondev.core.YMLSection;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedstoneType extends DoorType {
    public RedstoneType() {
        super("redstone");
    }

    @Override
    @NotNull
    public RedstoneInstance read(@NotNull RoomInstance room, @NotNull YMLSection section) {
        return new RedstoneInstance(room, section);
    }

    @Override
    @NotNull
    public RedstoneBuilder getBuilder(@NotNull RoomBuilder room) {
        return new RedstoneBuilder(room);
    }

    public final class RedstoneBuilder extends DoorBuilder {

        private final List<BlockVector> blocks = new ArrayList<>();
        private boolean completedRedstonePlates = false;
        private boolean atSameTime = true;


        public RedstoneBuilder(@NotNull RoomBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            List<String> blockList = new ArrayList<>();
            blocks.forEach((value) -> blockList.add(Util.toString(value.subtract(getRoomOffset()))));
            section.set("powered_blocks", blockList);
            section.set("all_powered_at_same_time", atSameTime);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    if (event.getClickedBlock() == null)
                        return;
                    if (!getRoomBuilder().contains(event.getClickedBlock().getLocation()))
                        return;
                    BlockVector loc = event.getClickedBlock().getLocation().toVector().toBlockVector();
                    if (!blocks.remove(loc))
                        blocks.add(loc);
                }
                case 2 -> {
                    atSameTime = !atSameTime;
                    setupTools();
                }
                case 6 -> {
                    if (blocks.size() > 0) {
                        completedRedstonePlates = true;
                        event.getPlayer().getInventory().setHeldItemSlot(0);
                        setupTools();
                    }
                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            if (!completedRedstonePlates) {
                Player player = getPlayer();
                PlayerInventory inv = player.getInventory();
                CUtils.setSlot(player, 0, inv, Material.PAPER, "doorbuilder.redstone_blocks_info");
                CUtils.setSlot(player, 1, inv, Material.STICK, "doorbuilder.redstone_blocks_selector",
                        "%value%", String.valueOf(blocks.size()));
                CUtils.setSlot(player, 2, inv, Material.CLOCK, "doorbuilder.redstone_blocks_atsametime",
                        "%value%", String.valueOf(atSameTime));
                if (blocks.size() > 0)
                    CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "doorbuilder.redstone_blocks_confirm");
                return;
            }
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            if (getRoomBuilder().getTickCounter() % 2 == 0)
                blocks.forEach((block) -> CUtils.markBlock(player, block, color));
        }

    }

    public class RedstoneInstance extends DoorInstance {

        private final List<BlockVector> poweredBlocks = new ArrayList<>();
        private final boolean atSameTime;

        public RedstoneInstance(@NotNull RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
            section.getStringList("powered_blocks", Collections.emptyList()).forEach(
                    (key) -> this.poweredBlocks.add(Util.toBlockVector(key)));
            this.atSameTime = section.getBoolean("all_powered_at_same_time", true);
        }

        @Override
        @NotNull
        public RedstoneHandler createDoorHandler(@NotNull RoomHandler roomHandler) {
            return new RedstoneHandler(roomHandler);
        }

        public class RedstoneHandler extends DoorHandler {

            private final List<Block> poweredBlocksList = new ArrayList<>();
            private boolean unlocked = false;
            private ItemDisplay item;
            private TextDisplay text;

            public RedstoneHandler(@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

            public void setupOffset() {
                super.setupOffset();
                for (BlockVector vector : RedstoneInstance.this.poweredBlocks)
                    poweredBlocksList.add(this.getRoomHandler().getLocation().add(vector).getBlock());
            }

            @Override
            public boolean canUse(@NotNull Player player) {
                if (!super.canUse(player))
                    return false;
                return unlocked;
            }

            @Override
            public void onFirstPlayerEnter(@NotNull Player player) {
                //entities.addAll(getRoom().getMonsters());
                @NotNull World world = getRoomHandler().getDungeonHandler().getWorld();
                Vector center = this.getBoundingBox().getCenter();
                item = (ItemDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() + 0.5, center.getZ())
                        .setDirection(getDoorFace().getOppositeFace().getDirection()), EntityType.ITEM_DISPLAY);
                item.setItemStack(new ItemStack(atSameTime ? Material.REDSTONE_LAMP : Material.TARGET));
                Transformation tr = item.getTransformation();
                tr.getScale().mul(1F, 1F, 0.1F);
                item.setTransformation(tr);
                item.setBrightness(new Display.Brightness(15, 15));
                item.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
                text = (TextDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() - 0.5, center.getZ())
                        .setDirection(getDoorFace().getDirection()), EntityType.TEXT_DISPLAY);
                text.setBrightness(new Display.Brightness(15, 15));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getRoomHandler().getDungeonHandler().getState() != DungeonHandler.State.STARTED) {
                            text.remove();
                            item.remove();
                            this.cancel();
                            return;
                        }
                        if (atSameTime) {
                            final int[] counter = {0};
                            poweredBlocksList.forEach(block -> {
                                if (block.isBlockFacePowered(BlockFace.SELF))
                                    counter[0]++;
                            });
                            if (counter[0] >= poweredBlocksList.size()) {
                                text.remove();
                                item.remove();
                                unlocked = true;
                                this.cancel();
                                return;
                            }
                            //TODO it's not player language specific
                            text.setText(CUtils.craftMsg(null, "door.redstone_sametime_info",
                                    "%current%", String.valueOf(counter[0]), "%max%", String.valueOf(poweredBlocksList.size())).toLegacy());
                        } else {
                            poweredBlocksList.removeIf(block -> block.isBlockFacePowered(BlockFace.SELF));
                            if (poweredBlocksList.isEmpty()) {
                                text.remove();
                                item.remove();
                                unlocked = true;
                                this.cancel();
                                return;
                            }
                            //TODO it's not player language specific
                            text.setText(CUtils.craftMsg(null, "door.redstone_notsametime_info",
                                    "%value%", String.valueOf(poweredBlocksList.size())).toLegacy());
                        }
                    }
                }.runTaskTimer(DeepDungeons.get(), 8L, 8L);//snowball & small projectiles activate the Target for 8 ticks
            }
        }

    }
}
