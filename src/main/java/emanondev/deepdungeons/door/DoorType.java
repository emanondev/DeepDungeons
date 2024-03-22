package emanondev.deepdungeons.door;

import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.ParticleUtility;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.interfaces.AreaHolder;
import emanondev.deepdungeons.interfaces.MoveListener;
import emanondev.deepdungeons.party.PartyManager;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class DoorType extends DRegistryElement {

    public DoorType(@NotNull String id) {
        super(id);
    }

    @NotNull
    public abstract DoorInstance read(@NotNull RoomInstance instance, @NotNull YMLSection section);

    @NotNull
    public abstract DoorBuilder getBuilder(@NotNull RoomBuilder room);

    public abstract class DoorBuilder extends DInstance<DoorType> {

        private final RoomBuilder roomBuilder;
        private final CompletableFuture<DoorBuilder> completableFuture = new CompletableFuture<>();
        private BoundingBox area;
        private Vector spawnOffset;
        private float spawnYaw;
        private BlockFace doorFace = BlockFace.NORTH;
        private float spawnPitch;
        private boolean hasConfirmedSpawnLocation = false;
        private int cooldownLengthSeconds = 5;

        public DoorBuilder(@NotNull RoomBuilder room) {
            super(DoorType.this);
            this.roomBuilder = room;
        }

        /**
         * @param doorFace must be cardinal
         * @throws IllegalArgumentException if argument is not cardinal
         */
        public void setDoorFace(@NotNull BlockFace doorFace) {
            if (!doorFace.isCartesian())
                throw new IllegalArgumentException();
            this.doorFace = doorFace;
        }

        @NotNull
        public CompletableFuture<DoorBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public void abort() {
            completableFuture.completeExceptionally(new Exception("aborted"));
        }

        public void complete() {
            completableFuture.complete(this);
        }

        @NotNull
        public RoomBuilder getRoomBuilder() {
            return roomBuilder;
        }

        public final void writeTo(@NotNull YMLSection section) {
            section.set("type", getType().getId());
            section.set("box", Util.toString(area.getMin().toBlockVector()) + " " + Util.toString(area.getMax().toBlockVector()));
            section.setEnumAsString("doorFace", doorFace);
            section.set("spawnOffset", Util.toString(spawnOffset));
            section.set("spawnYaw", spawnYaw);
            section.set("spawnPitch", spawnPitch);
            section.set("cooldownLengthSeconds", cooldownLengthSeconds);
            writeToImpl(section);
        }

        @Nullable
        public Vector getSpawnOffset() {
            return spawnOffset == null ? null : spawnOffset.clone();
        }

        public void setSpawn(@NotNull Vector spawnOffset, float spawnYaw, float spawnPitch) {
            this.spawnOffset = spawnOffset;
            this.spawnYaw = spawnYaw;
            this.spawnPitch = spawnPitch;
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

        public void setupTools() {
            Player player = getPlayer();
            /*if (player == null || !player.isValid())
                return;
            for (int i = 0; i < 9; i++) //clear
                inv.setItem(i, null);*/ //should be already done on RoomTypeBuilder
            Inventory inv = player.getInventory();
            if (getArea() == null) {
                CUtils.setSlot(player, 0, inv, Material.PAPER, "doorbuilder.base_area_info");
                CUtils.setSlot(player, 1, inv, Material.WOODEN_AXE, "doorbuilder.base_area_axe");
                CUtils.setSlot(player, 2, inv, Material.BROWN_DYE, "doorbuilder.base_area_pos");
                CUtils.setSlot(player, 6, inv, Material.GREEN_DYE, "doorbuilder.base_area_confirm");
                return;
            }
            if (!hasConfirmedSpawnLocation) {
                CUtils.setSlot(player, 0, inv, Material.PAPER, "doorbuilder.base_commondata_info");
                CUtils.setSlot(player, 1, inv, Material.ENDER_PEARL, "doorbuilder.base_commondata_spawn", "%value%",
                        spawnOffset == null ? "<red>null</red>" : Util.toString(spawnOffset).replace(";", " "));
                CUtils.setSlot(player, 2, inv, Material.MAGENTA_GLAZED_TERRACOTTA, "doorbuilder.base_commondata_facing", "%value%", doorFace.name());
                CUtils.setSlot(player, 3, inv, Material.CLOCK, "doorbuilder.base_commondata_cooldowntime", "%value%",
                        UtilsString.getTimeStringSeconds(getPlayer(), cooldownLengthSeconds), "%value_raw%", String.valueOf(cooldownLengthSeconds));
                if (getSpawnOffset() != null)
                    CUtils.setSlot(player, 6, inv, Material.LIME_DYE, "doorbuilder.base_commondata_confirm");
                return;
            }
            this.setupToolsImpl();
        }

        protected abstract void handleInteractImpl(@NotNull PlayerInteractEvent event);

        protected abstract void setupToolsImpl();

        public void handleInteract(@NotNull PlayerInteractEvent event) {
            if (getArea() == null) {
                switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                    case 2 -> Bukkit.dispatchCommand(event.getPlayer(),
                            event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK ?
                                    "/pos1" : "/pos2");
                    case 6 -> {
                        BoundingBox box = WorldEditUtility.getSelectionBoxExpanded(event.getPlayer());
                        if (box == null) {
                            CUtils.sendMsg(event.getPlayer(), "doorbuilder.base_msg_must_set_area");
                            return;
                        }
                        if (!roomBuilder.getArea().contains(box)) {
                            CUtils.sendMsg(event.getPlayer(), "doorbuilder.base_msg_area_is_outside_room");
                            return;
                        }

                        box.shift(getRoomOffset().multiply(-1));
                        setArea(box);
                        doorFace = guessFace();
                        roomBuilder.setupTools();
                        WorldEditUtility.clearSelection(event.getPlayer());
                        event.getPlayer().getInventory().setHeldItemSlot(0);
                        //TODO adds spawn location default ? guessspawnlocation

                    }
                }
                return;
            }
            if (!hasConfirmedSpawnLocation) {
                switch (event.getPlayer().getInventory().getHeldItemSlot()) {
                    case 1 -> {
                        if (!roomBuilder.getArea().contains(event.getPlayer().getBoundingBox())) {
                            CUtils.sendMsg(event.getPlayer(), "doorbuilder.base_msg_spawn_is_outside_room");
                            return;
                        }
                        setSpawn(event.getPlayer().getLocation().toVector().subtract(getRoomOffset()),
                                event.getPlayer().getLocation().getYaw(),
                                event.getPlayer().getLocation().getPitch());
                        roomBuilder.setupTools();
                    }
                    case 2 -> {
                        BlockFace next = doorFace;
                        next = BlockFace.values()[((BlockFace.values().length) + next.ordinal()
                                + (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ? 1 : -1)) % BlockFace.values().length];
                        while (!next.isCartesian())
                            next = BlockFace.values()[((BlockFace.values().length) + next.ordinal()
                                    + (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ? 1 : -1)) % BlockFace.values().length];
                        setDoorFace(next);
                        roomBuilder.setupTools();
                    }
                    case 3 -> {
                        MapGui mapGui = new MapGui(
                                CUtils.craftMsg(getPlayer(), "doorbuilder.base_commandata_cooldowntitle"),
                                1, getPlayer(), null, DeepDungeons.get());

                        mapGui.setButton(4, new NumberEditorFButton<>(mapGui, 1, 1, 10000, () -> cooldownLengthSeconds,
                                (time) -> cooldownLengthSeconds = Math.min(Math.max(-1, time), 36000),
                                () -> CUtils.createItem(getPlayer(), Material.REPEATER, "doorbuilder.base_commandata_cooldowneditor", "%value%",
                                        UtilsString.getTimeStringSeconds(getPlayer(), cooldownLengthSeconds),
                                        "%value_raw%", String.valueOf(cooldownLengthSeconds)), true));
                        mapGui.open(event.getPlayer());
                    }
                    case 6 -> {
                        if (getSpawnOffset() != null) {
                            hasConfirmedSpawnLocation = true;
                            roomBuilder.setupTools();
                            event.getPlayer().getInventory().setHeldItemSlot(0);
                        }
                    }
                }
                return;
            }
            this.handleInteractImpl(event);
        }

        @NotNull
        private BlockFace guessFace() {
            Vector v = roomBuilder.getArea().shift(roomBuilder.getOffset().multiply(-1)).getCenter();
            if (area.getWidthX() < area.getWidthZ()) {
                if (v.distanceSquared(area.getMin()) > v.distanceSquared(area.getMin().add(new Vector(area.getWidthX(), 0, 0))))
                    return BlockFace.EAST;
                return BlockFace.WEST;
            }
            if (v.distanceSquared(area.getMin()) > v.distanceSquared(area.getMin().add(new Vector(0, 0, area.getWidthZ()))))
                return BlockFace.SOUTH;
            return BlockFace.NORTH;
        }

        @Nullable
        public Player getPlayer() {
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


        public void timerTick(@NotNull Player player, @NotNull Color color) {

            if (roomBuilder.getTickCounter() % 2 == 0) { //reduce particle amount = have a tick 5 time per second instead of 10
                if (area == null) {
                    CUtils.showWEBound(player, roomBuilder.getTickCounter());
                    return;
                }
                ParticleUtility.spawnParticleBoxFaces(player, roomBuilder.getTickCounter() / 6, 3, Particle.REDSTONE,
                        getArea().shift(getRoomOffset()), new Particle.DustOptions(color, 0.6F));
                showFaceArrow(player, color);

                Vector doorSpawn = getSpawnOffset();
                if (doorSpawn != null) {
                    ParticleUtility.spawnParticleCircle(player, Particle.REDSTONE, doorSpawn.add(getRoomOffset()), 0.25D,
                            roomBuilder.getTickCounter() % 4 == 0, new Particle.DustOptions(color, 0.6F));
                }
            }

            tickTimerImpl(player, color);
        }

        private void showFaceArrow(@NotNull Player player, @NotNull Color color) {
            if (doorFace != null && !getCompletableFuture().isDone()) {

                CUtils.showArrow(player, color, doorFace, area.getCenter().add(doorFace.getDirection().multiply(0.5D).multiply(new Vector(area.getWidthX(), area.getHeight(),
                        area.getWidthZ()))).add(getRoomOffset()));
                /* //TODO test arrows on doors then delete this
                Particle.DustOptions dust = new Particle.DustOptions(color, 0.3F);
                Vector r = area.getCenter().add(doorFace.getDirection().multiply(0.5D).multiply(new Vector(area.getWidthX(), area.getHeight(),
                        area.getWidthZ())).add(doorFace.getDirection().multiply(0.7))).add(getRoomOffset());
                Vector dir = doorFace.getOppositeFace().getDirection();
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir,
                        0.7, 0.1, dust);
                if (doorFace == BlockFace.NORTH || doorFace == BlockFace.SOUTH) {
                    dir.add(new Vector(0, 0.4, 0));
                    for (int i = 0; i < 8; i++) {
                        dir.rotateAroundZ(Math.PI / 4);
                        ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir, 0.3, 0.1, dust);
                    }
                } else if (doorFace == BlockFace.EAST || doorFace == BlockFace.WEST) {
                    dir.add(new Vector(0, 0.4, 0));
                    for (int i = 0; i < 8; i++) {
                        dir.rotateAroundX(Math.PI / 4);
                        ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir, 0.3, 0.1, dust);
                    }
                } else {
                    dir.add(new Vector(0.4, 0, 0));
                    for (int i = 0; i < 8; i++) {
                        dir.rotateAroundY(Math.PI / 4);
                        ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir, 0.3, 0.1, dust);
                    }
                }*/
            }
        }

        protected abstract void tickTimerImpl(@NotNull Player player, @NotNull Color color);
    }


    public abstract class DoorInstance extends DInstance<DoorType> {

        private final RoomInstance roomInstance;
        private final BlockFace doorFace;
        private final BoundingBox box;
        private final Vector spawnOffset;
        private final float spawnYaw;
        private final float spawnPitch;
        private final int cooldownLengthSeconds;

        public DoorInstance(@NotNull RoomInstance roomInstance, @NotNull YMLSection section) {
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
            this.cooldownLengthSeconds = section.getInt("cooldownLengthSeconds", 5);
        }

        @Contract(pure = true)
        @NotNull
        public RoomInstance getRoomInstance() {
            return this.roomInstance;
        }

        /**
         * @return a bounding box with offset relative to the room
         */
        @Contract("-> new")
        @NotNull
        public BoundingBox getBoundingBox() {
            return this.box.clone();
        }

        /**
         * @param roomOffset offset where the room is placed, (door offset not included)
         * @return spawn location yaw and picth included
         */
        @Contract("_ -> new")
        @NotNull
        public Location getSpawnLocation(@NotNull Location roomOffset) {
            Location loc = roomOffset.clone().add(spawnOffset);
            loc.setYaw(spawnYaw);
            loc.setPitch(spawnPitch);
            return loc;
        }

        /**
         * @return offset relative to the room
         */
        @Contract("-> new")
        @NotNull
        public Vector getSpawnOffset() {
            return this.spawnOffset.clone();
        }

        public float getSpawnYaw() {
            return this.spawnYaw;
        }

        public float getSpawnPitch() {
            return this.spawnPitch;
        }

        @NotNull
        public abstract DoorHandler createDoorHandler(@NotNull RoomHandler roomHandler);

        @NotNull
        public BlockFace getDoorFace() {
            return this.doorFace;
        }

        public abstract class DoorHandler implements MoveListener, AreaHolder {

            private final RoomHandler roomHandler;
            private final HashMap<UUID, Long> cooldowns = new HashMap<>();
            private final HashSet<UUID> blocked = new HashSet<>();
            private final HashMap<UUID, ItemDisplay> cooldownItems = new HashMap<>();
            private final HashMap<UUID, TextDisplay> cooldownText = new HashMap<>();
            private DoorHandler link;
            private BoundingBox boundingBox;
            private Location spawn;
            private BukkitTask cooldownTask;

            public DoorHandler(@NotNull RoomHandler roomHandler) {
                this.roomHandler = roomHandler;
            }

            public BoundingBox getBoundingBox() {
                return boundingBox.clone();
            }

            public boolean teleportIn(@NotNull Player player) {
                setCooldown(player, cooldownLengthSeconds);
                return player.teleport(this.getSpawn());
            }

            @Override
            @NotNull
            public World getWorld() {
                return roomHandler.getWorld();
            }

            private void setCooldown(Player player, int cooldownSeconds) {
                if (cooldownSeconds == 0)
                    return;
                if (cooldownSeconds > 0)
                    cooldowns.put(player.getUniqueId(), cooldownSeconds * 1000L + System.currentTimeMillis());
                else
                    blocked.add(player.getUniqueId());
                World world = getRoomHandler().getDungeonHandler().getWorld();
                Vector center = this.getBoundingBox().getCenter();
                ItemDisplay item = (ItemDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() + 0.25, center.getZ())
                        .setDirection(getDoorFace().getDirection()), EntityType.ITEM_DISPLAY);
                item.setItemStack(new ItemStack(Material.BARRIER));
                item.setBrightness(new Display.Brightness(15, 15));
                TextDisplay text = (TextDisplay) world.spawnEntity(new Location(world, center.getX(), center.getY() - 0.5, center.getZ())
                        .setDirection(getDoorFace().getDirection()), EntityType.TEXT_DISPLAY);
                text.setBrightness(new Display.Brightness(15, 15));
                PartyManager.getInstance().getParty(player).getPlayers()
                        .forEach(player1 -> {
                            if (player1 != player) {
                                player1.hideEntity(DeepDungeons.get(), item);
                                player1.hideEntity(DeepDungeons.get(), text);
                            }
                        });
                if (cooldownSeconds < 0)
                    text.setText(CUtils.craftMsg(player, "door.cooldown_info",
                            "%left%", "∞").toLegacy()); //TODO doesn't update if player change language

                cooldownItems.put(player.getUniqueId(), item);
                cooldownText.put(player.getUniqueId(), text);

                if (!(cooldownSeconds > 0 && (this.cooldownTask == null || cooldownTask.isCancelled())))
                    return;

                cooldownTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getRoomHandler().getDungeonHandler().getState() != DungeonHandler.State.STARTED) {
                            cooldownItems.values().forEach(Entity::remove);
                            cooldownText.values().forEach(Entity::remove);
                            this.cancel();
                            return;
                        }
                        long now = System.currentTimeMillis();
                        for (UUID uuid : new ArrayList<>(cooldowns.keySet()))
                            if (cooldowns.get(uuid) < now) {
                                cooldowns.remove(uuid);
                                cooldownItems.remove(uuid).remove();
                                cooldownText.remove(uuid).remove();
                            }
                        if (cooldowns.isEmpty()) {
                            this.cancel();
                            return;
                        }

                        cooldowns.keySet().forEach((uuid) -> {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null)
                                cooldownText.get(uuid).setText(CUtils.craftMsg(player, "door.cooldown_info",
                                        "%left%", String.valueOf((cooldowns.get(uuid) - now) / 1000 + 1)).toLegacy());
                        });
                    }
                }.runTaskTimer(DeepDungeons.get(), 10L, 10L);
            }

            @NotNull
            @Contract(pure = true, value = "-> new")
            public Location getSpawn() {
                return this.spawn.clone();
            }

            @Contract(pure = true)
            @NotNull
            public final DoorInstance getDoorInstance() {
                return DoorInstance.this;
            }

            @Contract(pure = true)
            @NotNull
            public RoomHandler getRoomHandler() {
                return this.roomHandler;
            }

            public void link(@Nullable DoorHandler entrance) {
                this.link = entrance;
            }

            @Contract(pure = true)
            @Nullable
            public DoorHandler getLink() {
                return this.link;
            }

            public void setupOffset() {
                if (boundingBox != null)
                    throw new IllegalStateException();
                this.boundingBox = getDoorInstance().getBoundingBox().shift(this.getRoomHandler().getLocation().toVector());
                this.spawn = getDoorInstance().getSpawnLocation(getRoomHandler().getLocation());
                this.spawn.setPitch(getSpawnPitch());
                this.spawn.setYaw(getSpawnYaw());
            }

            public void onPlayerMove(@NotNull PlayerMoveEvent event) {
                DoorHandler link = this.getLink();
                if (link == null) {
                    if (this.equals(this.getRoomHandler().getDungeonHandler().getEntrance())) {
                        //TODO help exit the dungeon confirm with gui
                        return;
                    }
                    if (this.equals(this.getRoomHandler().getEntrance())) {
                        DoorHandler back = PartyManager.getInstance().getDungeonPlayer(event.getPlayer()).getBackRoute(this);
                        if (back != null) {
                            if (canUse(event.getPlayer())) {
                                back.teleportIn(event.getPlayer());
                            }
                            return;
                        }
                        //TODO else player was teleported to a room?
                        return;
                    }
                    if (canUse(event.getPlayer())) {
                        PartyManager.getInstance().getParty(event.getPlayer()).flagPlayerExitDungeon(event.getPlayer());
                    }
                    //TODO dungeon completed by this player ?
                    return;
                }
                if (canUse(event.getPlayer())) {
                    link.teleportIn(event.getPlayer());
                    PartyManager.getInstance().getDungeonPlayer(event.getPlayer()).addDoorHistory(this, link);
                }
            }

            public boolean contains(@NotNull Vector vector) {
                return this.boundingBox.contains(vector);
            }

            public boolean overlaps(@NotNull BoundingBox box) {
                return this.boundingBox.overlaps(box);
            }

            public boolean canUse(@NotNull Player player) {
                if (blocked.contains(player.getUniqueId()))
                    return false;
                Long cooldown = cooldowns.get(player.getUniqueId());
                return cooldown == null || cooldown < System.currentTimeMillis();
            }

            public void onFirstPlayerEnter(@NotNull Player player) {
            }
        }

    }
}
