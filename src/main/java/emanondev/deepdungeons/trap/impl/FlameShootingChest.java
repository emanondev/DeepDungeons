package emanondev.deepdungeons.trap.impl;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.gui.FButton;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.message.DMessage;
import emanondev.core.util.ParticleUtility;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.InteractListener;
import emanondev.deepdungeons.room.RoomType;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.trap.TrapType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class FlameShootingChest extends TrapType {

    public FlameShootingChest() {
        super("flameshootingchest");
    }

    @NotNull
    @Override
    public TrapInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub) {
        return new FlameChestInstance(instance, sub);
    }

    @NotNull
    @Override
    public TrapInstanceBuilder getBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
        return new FlameChestInstanceBuilder(room);
    }

    public class FlameChestInstanceBuilder extends TrapInstanceBuilder {

        private Block block;
        private boolean directioned = true;
        private int maxTicks = 100;
        private int addedTicks = 8;
        private int maxUses = 1;
        private double freezeChance = 0;

        public FlameChestInstanceBuilder(RoomType.RoomInstanceBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            section.set("chest", Util.toString(block.getLocation().toVector().toBlockVector().subtract(getRoomOffset())));
            section.set("directionedFlame", directioned);
            section.set("maxTicks", maxTicks);
            section.set("addedTicks", addedTicks);
            section.set("maxUses", maxUses == 0 ? -1 : maxUses);
            section.set("freezeChance", freezeChance);
        }

        @Override
        protected void handleInteractImpl(@NotNull PlayerInteractEvent event) {
            Player p = event.getPlayer();
            PlayerInventory inv = p.getInventory();
            switch (inv.getHeldItemSlot()) {
                case 1 -> {
                    if (event.getClickedBlock() == null)
                        return;
                    if (!getRoomBuilder().contains(event.getClickedBlock().getLocation())) {
                        //TODO invalid block
                        return;
                    }
                    if (!(event.getClickedBlock().getState() instanceof Chest)) {
                        //TODO not chest kind
                        return;
                    }
                    block = event.getClickedBlock();
                    getRoomBuilder().setupTools();
                }
                case 2 -> {
                    MapGui map = new MapGui(new DMessage(DeepDungeons.get(), p).appendLang("aaa"),
                            1, p, null, DeepDungeons.get());//TODO lang
                    map.setButton(0, new FButton(map, () -> new ItemBuilder(Material.ARROW).build() //TODO lang
                            , (evt) -> {
                        directioned = !directioned;
                        return true;
                    }));
                    map.setButton(2, new NumberEditorFButton<>(map, 1, 1, 10, () -> maxTicks,
                            (value) -> maxTicks = Math.max(1, value),
                            () -> new ItemBuilder(Material.REPEATER).build(), true)); //TODO lang
                    map.setButton(4, new NumberEditorFButton<>(map, 1, 1, 10, () -> addedTicks,
                            (value) -> addedTicks = Math.max(1, value),
                            () -> new ItemBuilder(Material.REPEATER).build(), true)); //TODO lang
                    map.setButton(6, new NumberEditorFButton<>(map, 1, 1, 10, () -> maxUses,
                            (value) -> maxUses = Math.max(0, value),
                            () -> new ItemBuilder(Material.REPEATER).build(), true)); //TODO lang, if 0 then unlimited
                    map.setButton(8, new NumberEditorFButton<>(map, 1D, 0.1D, 10D, () -> (freezeChance * 100),
                            (value) -> freezeChance = Math.max(0D, Math.min(1, value / 100D)),
                            () -> new ItemBuilder(Material.REPEATER).build(), true)); //TODO lang

                    //directioned = !directioned;
                    //getRoomBuilder().setupTools();
                }
                case 6 -> { //confirm
                    if (block != null) {
                        inv.setHeldItemSlot(0);
                        this.complete();
                        return;
                    }
                    //TODO msg error!
                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            Player p = getPlayer();
            PlayerInventory inv = p.getInventory();
            inv.setItem(0, new ItemBuilder(Material.PAPER).build());//TODO lang info
            inv.setItem(1, new ItemBuilder(Material.STICK).build());//TODO lang stick tool for chest selector
            inv.setItem(2, new ItemBuilder(Material.ARROW).build());//TODO lang flag for directionedFlame
            inv.setItem(6, new ItemBuilder(Material.LIME_DYE).build());//TODO lang confirm
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            if (block != null && getRoomBuilder().getTickCounter() % 2 == 0) {
                Particle.DustOptions info = new Particle.DustOptions(color, 0.4F);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX(), block.getY(), block.getZ(), BlockFace.UP.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX() + 1, block.getY(), block.getZ(), BlockFace.UP.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX(), block.getY(), block.getZ() + 1, BlockFace.UP.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX() + 1, block.getY(), block.getZ() + 1, BlockFace.UP.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX(), block.getY(), block.getZ(), BlockFace.EAST.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX(), block.getY(), block.getZ(), BlockFace.SOUTH.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX(), block.getY() + 1, block.getZ(), BlockFace.EAST.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX(), block.getY() + 1, block.getZ(), BlockFace.SOUTH.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX() + 1, block.getY(), block.getZ() + 1, BlockFace.WEST.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX() + 1, block.getY(), block.getZ() + 1, BlockFace.NORTH.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX() + 1, block.getY() + 1, block.getZ() + 1, BlockFace.WEST.getDirection(), 1, 0.25D, info);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, block.getX() + 1, block.getY() + 1, block.getZ() + 1, BlockFace.NORTH.getDirection(), 1, 0.25D, info);
            }
        }
    }

    public class FlameChestInstance extends TrapInstance {

        private final BlockVector where;
        private final boolean directioned;
        private final int maxTicks;
        private final int addedTicks;
        private final int maxUses;
        private final double freezeChance;

        public FlameChestInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            this.where = Util.toBlockVector(section.getString("chest"));
            this.directioned = section.getBoolean("directionedFlame", false);
            this.maxTicks = section.getInt("maxTicks", 100);
            this.addedTicks = section.getInt("addedTicks", 8);
            this.maxUses = Math.max(-1, section.getInt("maxUses", 1));
            this.freezeChance = Math.max(0, Math.min(1, section.getDouble("freezeChance", 0)));
        }

        @Override
        public TrapHandler createTrapHandler(@NotNull RoomInstance.RoomHandler roomHandler) {
            return new FlameChestHandler(roomHandler);
        }

        public class FlameChestHandler extends TrapHandler implements InteractListener {

            private Block whereHandler;
            private int uses = maxUses;
            private final boolean isFire = Math.random() > freezeChance;

            public FlameChestHandler(@NotNull RoomInstance.RoomHandler roomHandler) {
                super(roomHandler);
            }

            @Override
            public void setupOffset() {
                whereHandler = getWorld().getBlockAt(getRoom().getLocation().add(where));
            }

            @Override
            public void onFirstPlayerEnter(@NotNull Player player) {

            }

            @Override
            public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
                if (uses == 0)
                    return;
                if (!whereHandler.equals(event.getClickedBlock()))
                    return;
                Location center = whereHandler.getLocation().add(0.5, 10 / 16F, 0.5);
                Vector direction;
                if (!(whereHandler.getState() instanceof Chest chest))
                    return;
                if (directioned)
                    direction = event.getPlayer().getBoundingBox().getCenter().subtract(center.toVector()).normalize();
                else
                    direction = ((Directional) whereHandler.getBlockData()).getFacing().getDirection();

                getWorld().playSound(center, Sound.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1F, 1F);
                if (uses > 0) //-1 = unlimited
                    uses--;
                chest.open();
                new BukkitRunnable() {
                    private int counter = 0;

                    @Override
                    public void run() {
                        counter++;
                        if (!(whereHandler.getState() instanceof Chest chest)) {
                            this.cancel();
                            return;
                        }
                        if (counter >= maxTicks + 5) {
                            this.cancel();
                            //if (chest.getInventory().getViewers().isEmpty())
                            chest.close();
                            return;
                        }
                        List<Vector> spreaded = new ArrayList<>();
                        for (int i = 0; i < 2; i++) {
                            Vector dir = direction.clone().add(new Vector(
                                    (Math.random() - 0.5) / 3, (Math.random() - 0.5) / 3, (Math.random() - 0.5) / 3));
                            spreaded.add(dir);
                            getWorld().spawnParticle(isFire ? Particle.FLAME : Particle.CLOUD, center, 0, dir.getX(), dir.getY(), dir.getZ(), 0.28);
                        }
                        if (counter < maxTicks - 5 && counter % 5 == 0)
                            getWorld().playSound(center, isFire ? Sound.BLOCK_FIRE_AMBIENT : Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.BLOCKS, 1F, 0.3F);
                        if (counter > 5) { //give particle time to get to target
                            HashSet<Entity> target = new HashSet<>();
                            spreaded.forEach(dir -> {
                                RayTraceResult ray = getWorld().rayTraceEntities(center, dir, 5.5, 0.1);
                                if (ray == null || ray.getHitEntity() == null)
                                    return;
                                RayTraceResult back = getWorld().rayTraceBlocks(ray.getHitPosition().toLocation(getWorld()),
                                        dir.multiply(-1), 5.5, FluidCollisionMode.ALWAYS, true);
                                if (back == null || !whereHandler.equals(back.getHitBlock()))
                                    return;
                                target.add(ray.getHitEntity());
                            });

                            target.forEach(t -> {
                                if (isFire)
                                    t.setFireTicks(addedTicks + Math.max(t.getFireTicks(), 10));
                                else
                                    t.setFreezeTicks(addedTicks + Math.max(t.getFreezeTicks(), 130));
                            });
                        }

                    }
                }.runTaskTimer(DeepDungeons.get(), 2L, 1L);
            }
        }
    }
}
