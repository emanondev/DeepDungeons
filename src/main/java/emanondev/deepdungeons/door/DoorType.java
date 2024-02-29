package emanondev.deepdungeons.door;

import emanondev.core.ItemBuilder;
import emanondev.core.YMLSection;
import emanondev.core.message.DMessage;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.ParticleUtility;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class DoorType extends DRegistryElement {

    public DoorType(@NotNull String id) {
        super(id);
    }

    public abstract @NotNull DoorInstance read(@NotNull RoomType.RoomInstance instance, @NotNull YMLSection section);

    public abstract @NotNull DoorInstanceBuilder getBuilder(@NotNull RoomType.RoomInstanceBuilder room);

    public abstract class DoorInstanceBuilder extends DInstance<DoorType> {

        private final RoomType.RoomInstanceBuilder roomBuilder;
        private BoundingBox area;
        private Vector spawnOffset;
        private float spawnYaw;
        private BlockFace face;
        private float spawnPitch;
        private final CompletableFuture<DoorInstanceBuilder> completableFuture = new CompletableFuture<>();

        public @NotNull CompletableFuture<DoorInstanceBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public void abort() {
            completableFuture.completeExceptionally(new Exception("aborted"));
        }

        public void complete() {
            completableFuture.complete(this);
        }

        public @NotNull RoomType.RoomInstanceBuilder getRoomBuilder() {
            return roomBuilder;
        }

        public DoorInstanceBuilder(@NotNull RoomType.RoomInstanceBuilder room) {
            super(DoorType.this);
            this.roomBuilder = room;
        }

        public final void writeTo(@NotNull YMLSection section) {
            section.set("type", getType().getId());
            section.set("box", Util.toString(area.getMin().toBlockVector()) + " " + Util.toString(area.getMax().toBlockVector()));
            section.set("spawnOffset", Util.toString(spawnOffset));
            section.set("spawnYaw", spawnYaw);
            section.set("spawnPitch", spawnPitch);
            writeToImpl(section);
        }

        public @Nullable Vector getSpawnOffset() {
            return spawnOffset == null ? null : spawnOffset.clone();
        }

        public void setSpawn(@NotNull Vector spawnOffset, float spawnYaw, float spawnPitch) {
            this.spawnOffset = spawnOffset;
            this.spawnYaw = spawnYaw;
            this.spawnPitch = spawnPitch;
        }

        public float getSpawnYaw() {
            return spawnYaw;
        }

        public float getSpawnPitch() {
            return spawnPitch;
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);


        private boolean hasConfirmedSpawnLocation = false;

        public void setupTools() {
            Player player = getPlayer();
            /*if (player == null || !player.isValid())
                return;
            for (int i = 0; i < 9; i++) //clear
                inv.setItem(i, null);*/ //should be already done on RoomTypeBuilder
            Inventory inv = player.getInventory();
            if (getArea() == null) {
                inv.setItem(0, new ItemBuilder(Material.WOODEN_AXE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .append("WorldEdit Wand")).build());
                inv.setItem(1, new ItemBuilder(Material.BROWN_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .append("//pos1 & //pos2")).build());
                inv.setItem(5, new ItemBuilder(Material.GREEN_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .append("Confirm Door Area")).build());
                return;
            }
            if (!hasConfirmedSpawnLocation) {
                inv.setItem(0, new ItemBuilder(Material.ENDER_PEARL).setDescription(new DMessage(DeepDungeons.get(), player)
                        .append("Set Door Spawn")).build());
                if (getSpawnOffset() != null)
                    inv.setItem(4, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                            .append("Confirm Door spawn")).build());
                return;
            }
            this.setupToolsImpl();
        }

        protected abstract void handleInteractImpl(@NotNull PlayerInteractEvent event);

        protected abstract void setupToolsImpl();

        public void handleInteract(PlayerInteractEvent event) {
            if (getArea() == null) {
                switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                    case 1 -> Bukkit.dispatchCommand(event.getPlayer(),
                            event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ?
                                    "/pos1" : "/pos2");
                    case 5 -> {
                        BoundingBox box = WorldEditUtility.getSelectionBoxExpanded(event.getPlayer());
                        if (box == null) {
                            //TODO select something first msg
                            return;
                        }
                        //TODO area size checks && contained inside

                        box.shift(getRoomOffset().multiply(-1));
                        setArea(box);
                        face = guessFace();
                        roomBuilder.setupTools();
                        WorldEditUtility.clearSelection(event.getPlayer());
                        //TODO adds spawn location default

                    }
                }
                return;
            }
            if (!hasConfirmedSpawnLocation) {
                switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                    case 0 -> {
                        //TODO check inside room
                        setSpawn(event.getPlayer().getLocation().toVector().subtract(getRoomOffset()),
                                event.getPlayer().getLocation().getPitch(),
                                event.getPlayer().getLocation().getYaw());
                        roomBuilder.setupTools();
                    }
                    case 4 -> {
                        if (getSpawnOffset() != null) {
                            hasConfirmedSpawnLocation = true;
                            roomBuilder.setupTools();
                        } else {
                            //TODO must set first
                        }
                    }
                }
                return;
            }
            this.handleInteractImpl(event);
        }

        private BlockFace guessFace() {
            if (area.getWidthX() == 1) {
                Vector v = area.getCenter();
                if (v.distanceSquared(area.getMin()) > v.distanceSquared(area.getMin().add(new Vector(area.getWidthX(), 0, 0))))
                    return BlockFace.NORTH;
                return BlockFace.SOUTH;
            } else if (area.getWidthZ() == 1) {
                Vector v = area.getCenter();
                if (v.distanceSquared(area.getMin()) > v.distanceSquared(area.getMin().add(new Vector(area.getWidthX(), 0, 0))))
                    return BlockFace.EAST;
                return BlockFace.WEST;
            }
            return null;
        }

        public @Nullable Player getPlayer() {
            return roomBuilder.getPlayer();
        }


        @Nullable
        public BlockVector getDoorOffset() {
            return area == null ? null : area.getMin().toBlockVector();
        }

        @Nullable
        public BlockVector getRoomOffset() {
            return getRoomBuilder().getOffset();
        }

        @Nullable
        public BoundingBox getArea() {
            return area == null ? null : area.clone();
        }

        protected void setArea(@NotNull BoundingBox box) {
            area = box.clone();
        }


        public void timerTick(Player player, Color color) {

            if (roomBuilder.getTickCounter() % 2 == 0) { //reduce particle amount = have a tick 5 time per second instead of 10
                if (area == null) {
                    showWEBound(player);
                    return;
                }
                ParticleUtility.spawnParticleBoxFaces(player, roomBuilder.getTickCounter() / 6, 3, Particle.REDSTONE,
                        getArea().shift(getRoomOffset()), new Particle.DustOptions(color, 0.6F));
                showFaceArrow(player,color);

                Vector doorSpawn = getSpawnOffset();
                if (doorSpawn != null) {
                    ParticleUtility.spawnParticleCircle(player, Particle.REDSTONE, doorSpawn.add(getRoomOffset()), 0.25D,
                            roomBuilder.getTickCounter() % 2 == 0, new Particle.DustOptions(color, 0.6F));
                }
            }

            tickTimerImpl(player);
        }

        private void showFaceArrow(Player player,Color color){
            if (face != null) {
                Particle.DustOptions dust = new Particle.DustOptions(color, 0.6F);
                Vector r = area.getCenter().add(face.getDirection().multiply(new Vector(area.getWidthX(), area.getHeight(),
                        area.getWidthZ()).multiply(0.5)).add(face.getDirection())).add(getRoomOffset());
                Vector dir = face.getOppositeFace().getDirection();
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE,r.getX(),r.getY(),r.getZ(),dir,
                        1,0.1,dust);
                if (face==BlockFace.NORTH||face==BlockFace.SOUTH) {
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0,0.3)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0.3,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0,-0.3)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,-0.3,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0.21,0.21)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,-0.21,0.21)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0.21,-0.21)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,-0.21,-0.21)),
                            0.3, 0.1, dust);
                }else if (face==BlockFace.EAST||face==BlockFace.WEST) {
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0.3,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0.3,0,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,-0.3,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(-0.3,0,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0.21,0.21,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(-0.21,0.21,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0.21,-0.21,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(-0.21,-0.21,0)),
                            0.3, 0.1, dust);
                }else if (face==BlockFace.DOWN||face==BlockFace.UP) {
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0,0.3)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0.3,0,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0,0,-0.3)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(-0.3,0,0)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0.21,0,0.21)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(-0.21,0,0.21)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(0.21,0,-0.21)),
                            0.3, 0.1, dust);
                    ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir.clone()
                                    .add(new Vector(-0.21,0,-0.21)),
                            0.3, 0.1, dust);
                }
            }
        }

        protected abstract void tickTimerImpl(Player player);

        protected void showWEBound(Player player) {
            try {
                ParticleUtility.spawnParticleBoxFaces(player, roomBuilder.getTickCounter() / 6 + 6, 4, Particle.REDSTONE, WorldEditUtility.getSelectionBoxExpanded(player),
                        new Particle.DustOptions(Color.WHITE, 0.3F));
            } catch (Exception ignored) {
            }
        }
    }


    public abstract class DoorInstance extends DInstance<DoorType> {

        private final RoomType.RoomInstance roomInstance;
        private final BlockFace doorFace;
        private final BoundingBox box;
        private final Vector spawnOffset;
        private final float spawnYaw;
        private final float spawnPitch;

        public DoorInstance(@NotNull RoomType.RoomInstance roomInstance, @NotNull YMLSection section) {
            super(DoorType.this);
            this.roomInstance = roomInstance;
            //this.section = section;
            BoundingBox tempBox;
            try {
                String[] box = section.getString("box").split(" ");
                BlockVector min = Util.toBlockVector(box[0]);
                BlockVector max = Util.toBlockVector(box[1]);
                tempBox = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
            } catch (Exception e) {
                e.printStackTrace();
                tempBox = new BoundingBox();
            }
            this.box = tempBox;
            Vector tempVector;
            try {
                String box = section.getString("spawnOffset");
                tempVector = Util.toVector(box);
            } catch (Exception e) {
                e.printStackTrace();
                tempVector = new Vector();
            }
            this.doorFace = section.loadEnum("doorFace", BlockFace.NORTH, BlockFace.class);
            this.spawnOffset = tempVector;
            this.spawnYaw = (float) section.getDouble("spawnYaw");
            this.spawnPitch = (float) section.getDouble("spawnPitch");
            //TODO
        }

        public @NotNull RoomType.RoomInstance getRoomInstance() {
            return this.roomInstance;
        }

        /**
         * @return a bounding box with offset relative to the room
         */
        @Contract("-> new")
        public @NotNull BoundingBox getBoundingBox() {
            return this.box.clone();
        }

        /**
         * @param roomOffset offset where the room is placed, (door offset not included)
         * @return spawn location yaw and picth included
         */
        @Contract("_ -> new")
        public @NotNull Location getSpawnLocation(@NotNull Location roomOffset) {
            Location loc = roomOffset.clone().add(spawnOffset);
            loc.setYaw(spawnYaw);
            loc.setPitch(spawnPitch);
            return loc;
        }

        /**
         * @return offset relative to the room
         */
        @Contract("-> new")
        public @NotNull Vector getSpawnOffset() {
            return spawnOffset.clone();
        }

        public float getSpawnYaw() {
            return spawnYaw;
        }

        public float getSpawnPitch() {
            return spawnPitch;
        }

        public abstract @NotNull DoorHandler getHandler();

        public @NotNull BlockFace getDoorFace() {
            return doorFace;
        }

        public abstract class DoorHandler {

            public final DoorInstance getInstance() {
                return DoorInstance.this;
            }

        }

    }
}
