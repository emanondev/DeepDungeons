package emanondev.deepdungeons.door.impl;

import emanondev.core.YMLSection;
import emanondev.core.util.ParticleUtility;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.door.DoorType;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
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
import java.util.List;

public class PressureType extends DoorType {
    public PressureType() {
        super("pressure");
    }

    @Override
    @NotNull
    public PressureInstance read(@NotNull RoomInstance room, @NotNull YMLSection section) {
        return new PressureInstance(room, section);
    }

    @Override
    @NotNull
    public PressureBuilder getBuilder(@NotNull RoomBuilder room) {
        return new PressureBuilder(room);
    }

    public final class PressureBuilder extends DoorBuilder {

        private final List<BlockVector> blocks = new ArrayList<>();
        private boolean completedPressurePlates = false;


        public PressureBuilder(@NotNull RoomBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            for (int i = 0; i < blocks.size(); i++)
                section.set("pressureplates." + (i + 1), Util.toString(blocks.get(i).subtract(getRoomOffset())));
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                case 1 -> {
                    if (event.getClickedBlock() == null)
                        return;
                    if (!getRoomBuilder().contains(event.getClickedBlock().getLocation()))
                        return;
                    if (!Tag.PRESSURE_PLATES.isTagged(event.getClickedBlock().getType()))
                        return;
                    BlockVector loc = event.getClickedBlock().getLocation().toVector().toBlockVector();
                    if (!blocks.remove(loc))
                        blocks.add(loc);

                }
                case 6 -> {
                    if (blocks.size() > 0) {
                        completedPressurePlates = true;
                        event.getPlayer().getInventory().setHeldItemSlot(0);
                        setupTools();
                    }
                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            if (!completedPressurePlates) {
                Player player = getPlayer();
                PlayerInventory inv = player.getInventory();
                CUtils.setSlot(player, 0, inv, Material.PAPER, "doorbuilder.pressure_plates_info");
                CUtils.setSlot(player, 1, inv, Material.STICK, "doorbuilder.pressure_plates_selector",
                        "%value%", String.valueOf(blocks.size()));
                if (blocks.size() > 0)
                    CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "doorbuilder.pressure_plates_confirm");
                return;
            }
            this.getCompletableFuture().complete(this);
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            if (getRoomBuilder().getTickCounter() % 2 == 0)
                blocks.forEach((block) -> ParticleUtility.spawnParticleCircle(player, Particle.REDSTONE, block.clone()
                                .add(new Vector(0.5D, getRoomBuilder().getTickCounter() % 4 == 0 ? 0.1D : 0.2D, 0.5D)), getRoomBuilder().getTickCounter() % 4 == 0 ? 0.3D : 0.1D,
                        getRoomBuilder().getTickCounter() % 4 == 0, new Particle.DustOptions(color, 0.4F)));

        }

    }

    public class PressureInstance extends DoorInstance {

        private final List<BlockVector> pressurePlates = new ArrayList<>();

        public PressureInstance(@NotNull RoomInstance roomInstance, @NotNull YMLSection section) {
            super(roomInstance, section);
            section.getKeys("pressureplates").forEach((key) -> this.pressurePlates.add(Util.toBlockVector(section.getString("pressureplates." + key))));
        }

        @Override
        @NotNull
        public PressureHandler createDoorHandler(@NotNull RoomHandler roomHandler) {
            return new PressureHandler(roomHandler);
        }

        public class PressureHandler extends DoorHandler {

            private final List<Block> pressurePlates = new ArrayList<>();
            private boolean unlocked = false;
            private ItemDisplay item;
            private TextDisplay text;

            public PressureHandler(@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

            public void setupOffset() {
                super.setupOffset();
                for (BlockVector vector : PressureInstance.this.pressurePlates)
                    pressurePlates.add(this.getRoomHandler().getLocation().add(vector).getBlock());
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
                item.setItemStack(new ItemStack(Material.OAK_PRESSURE_PLATE));
                Transformation tr = item.getTransformation();
                tr.getScale().mul(1.3F, 1.3F, 0.1F);
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
                        final int[] counter = {0};
                        pressurePlates.forEach(block -> {
                            if (Tag.PRESSURE_PLATES.isTagged(block.getType()) && ((Powerable) block.getBlockData()).isPowered())
                                counter[0]++;
                        });
                        if (counter[0] >= pressurePlates.size()) {
                            text.remove();
                            item.remove();
                            unlocked = true;
                            this.cancel();
                            return;
                        }
                        //TODO it's not player language specific
                        text.setText(CUtils.craftMsg(null, "door.pressure_info",
                                "%current%", String.valueOf(counter[0]), "%max%", String.valueOf(pressurePlates.size())).toLegacy());
                    }
                }.runTaskTimer(DeepDungeons.get(), 10L, 10L);
            }
        }

    }
}
