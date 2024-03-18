package emanondev.deepdungeons.door;

import emanondev.core.ItemBuilder;
import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.message.DMessage;
import emanondev.core.message.SimpleMessage;
import emanondev.core.util.DRegistryElement;
import emanondev.core.util.ParticleUtility;
import emanondev.core.util.WorldEditUtility;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance.DungeonHandler;
import emanondev.deepdungeons.interfaces.AreaHolder;
import emanondev.deepdungeons.interfaces.MoveListener;
import emanondev.deepdungeons.party.PartyManager;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import emanondev.deepdungeons.room.RoomType.RoomInstanceBuilder;
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
    public abstract DoorInstanceBuilder getBuilder(@NotNull RoomInstanceBuilder room);

    public abstract class DoorInstanceBuilder extends DInstance<DoorType> {

        private final RoomInstanceBuilder roomBuilder;
        private final CompletableFuture<DoorInstanceBuilder> completableFuture = new CompletableFuture<>();
        private BoundingBox area;
        private Vector spawnOffset;
        private float spawnYaw;
        private BlockFace doorFace = BlockFace.NORTH;
        private float spawnPitch;
        private boolean hasConfirmedSpawnLocation = false;
        private int cooldownLenghtSeconds = 5;

        public DoorInstanceBuilder(@NotNull RoomInstanceBuilder room) {
            super(DoorType.this);
            this.roomBuilder = room;
        }

        public BlockFace getDoorFace() {
            return doorFace;
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
        public CompletableFuture<DoorInstanceBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public void abort() {
            completableFuture.completeExceptionally(new Exception("aborted"));
        }

        public void complete() {
            completableFuture.complete(this);
        }

        @NotNull
        public RoomInstanceBuilder getRoomBuilder() {
            return roomBuilder;
        }

        public final void writeTo(@NotNull YMLSection section) {
            section.set("type", getType().getId());
            section.set("box", Util.toString(area.getMin().toBlockVector()) + " " + Util.toString(area.getMax().toBlockVector()));
            section.setEnumAsString("doorFace", doorFace);
            section.set("spawnOffset", Util.toString(spawnOffset));
            section.set("spawnYaw", spawnYaw);
            section.set("spawnPitch", spawnPitch);
            section.set("cooldownLengthSeconds", cooldownLenghtSeconds);
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

        public float getSpawnYaw() {
            return spawnYaw;
        }

        public float getSpawnPitch() {
            return spawnPitch;
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
                inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_area_info")).build());
                inv.setItem(1, new ItemBuilder(Material.WOODEN_AXE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_area_axe")).build());
                inv.setItem(2, new ItemBuilder(Material.BROWN_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_area_pos")).build());
                inv.setItem(6, new ItemBuilder(Material.GREEN_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_area_confirm")).build());
                return;
            }
            if (!hasConfirmedSpawnLocation) {
                inv.setItem(0, new ItemBuilder(Material.PAPER).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_commondata_info")).build());
                inv.setItem(1, new ItemBuilder(Material.ENDER_PEARL).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_commondata_spawn", "%value%",
                                spawnOffset == null ? "<red>null</red>" : Util.toString(spawnOffset))).build());
                inv.setItem(2, new ItemBuilder(Material.MAGENTA_GLAZED_TERRACOTTA).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_commondata_facing", "%value%", doorFace.name())).build());
                inv.setItem(3, new ItemBuilder(Material.CLOCK).setDescription(new DMessage(DeepDungeons.get(), player)
                        .appendLang("doorbuilder.base_commondata_cooldowntime", "%value%",
                                UtilsString.getTimeStringSeconds(getPlayer(), cooldownLenghtSeconds), "%value_raw%", String.valueOf(cooldownLenghtSeconds))).build());
                if (getSpawnOffset() != null)
                    inv.setItem(6, new ItemBuilder(Material.LIME_DYE).setDescription(new DMessage(DeepDungeons.get(), player)
                            .appendLang("doorbuilder.base_commondata_confirm")).build());
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
                            new SimpleMessage(DeepDungeons.get(), "doorbuilder.base_msg_must_set_area").send(event.getPlayer());
                            return;
                        }
                        if (!roomBuilder.getArea().contains(box)) {
                            new SimpleMessage(DeepDungeons.get(), "doorbuilder.base_msg_area_is_outside_room").send(event.getPlayer());
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
                            new SimpleMessage(DeepDungeons.get(), "doorbuilder.base_msg_spawn_is_outside_room").send(event.getPlayer());
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
                        MapGui mapGui = new MapGui(new DMessage(DeepDungeons.get(), getPlayer()).appendLang("doorbuilder.base_commandata_cooldowntitle"),
                                1, getPlayer(), null, DeepDungeons.get());

                        mapGui.setButton(4, new NumberEditorFButton<>(mapGui, 1, 1, 10000, () -> cooldownLenghtSeconds,
                                (time) -> cooldownLenghtSeconds = Math.min(Math.max(-1, time), 36000),
                                () -> new ItemBuilder(Material.REPEATER).setDescription(new DMessage(DeepDungeons.get(), getPlayer())
                                        .appendLang("doorbuilder.base_commandata_cooldowneditor", "%value%",
                                                UtilsString.getTimeStringSeconds(getPlayer(), cooldownLenghtSeconds),
                                                "%value_raw%", String.valueOf(cooldownLenghtSeconds))
                                ).setGuiProperty().build(), true));
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
                    showWEBound(player);
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
                }
            }
        }

        protected abstract void tickTimerImpl(@NotNull Player player, @NotNull Color color);

        protected void showWEBound(@NotNull Player player) {
            try {
                ParticleUtility.spawnParticleBoxFaces(player, roomBuilder.getTickCounter() / 6 + 6, 4, Particle.REDSTONE,
                        WorldEditUtility.getSelectionBoxExpanded(player), new Particle.DustOptions(Color.WHITE, 0.3F));
            } catch (Exception ignored) {
            }
        }
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
                World world = getRoom().getDungeonHandler().getWorld();
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
                    text.setText(new DMessage(DeepDungeons.get(), player).appendLang("door.cooldown_info",
                            "%left%", "∞").toLegacy()); //TODO doesn't update if player change language

                cooldownItems.put(player.getUniqueId(), item);
                cooldownText.put(player.getUniqueId(), text);

                if (!(cooldownSeconds > 0 && (this.cooldownTask == null || cooldownTask.isCancelled())))
                    return;

                cooldownTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (getRoom().getDungeonHandler().getState() != DungeonHandler.State.STARTED) {
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
                                cooldownText.get(uuid).setText(new DMessage(DeepDungeons.get(), player).appendLang("door.cooldown_info",
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
            public final DoorInstance getInstance() {
                return DoorInstance.this;
            }

            @Contract(pure = true)
            @NotNull
            public RoomHandler getRoom() {
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
                this.boundingBox = getInstance().getBoundingBox().shift(this.getRoom().getLocation().toVector());
                this.spawn = getInstance().getSpawnLocation(getRoom().getLocation());
                this.spawn.setPitch(getSpawnPitch());
                this.spawn.setYaw(getSpawnYaw());
            }

            public void onPlayerMove(@NotNull PlayerMoveEvent event) {
                DoorHandler link = this.getLink();
                if (link == null) {
                    if (this.equals(this.getRoom().getDungeonHandler().getEntrance())) {
                        //TODO help exit the dungeon confirm with gui
                        return;
                    }
                    if (this.equals(this.getRoom().getEntrance())) {
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
