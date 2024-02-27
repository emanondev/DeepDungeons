package emanondev.deepdungeons.door;

import emanondev.core.YMLSection;
import emanondev.core.util.DRegistryElement;
import emanondev.deepdungeons.DInstance;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.room.RoomType;
import org.bukkit.Location;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class DoorType extends DRegistryElement {

    public DoorType(@NotNull String id) {
        super(id);
    }

    public abstract @NotNull DoorInstance read(@NotNull RoomType.RoomInstance instance, @NotNull YMLSection section);

    public abstract @NotNull DoorInstanceBuilder getBuilder(@NotNull RoomType.RoomInstanceBuilder room);

    public abstract class DoorInstanceBuilder extends DInstance<DoorType> {

        private final RoomType.RoomInstanceBuilder roomBuilder;
        private BoundingBox boundingBox = new BoundingBox();
        private Vector spawnOffset = new Vector();
        private float spawnYaw;
        private float spawnPitch;
        private final CompletableFuture<DoorInstanceBuilder> completableFuture = new CompletableFuture<>();

        public @NotNull CompletableFuture<DoorInstanceBuilder> getCompletableFuture() {
            return completableFuture;
        }

        public abstract void start();

        public void abort(){
            completableFuture.completeExceptionally(new Exception("aborted"));
        }

        public void complete(){
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
            section.set("box", Util.toString(boundingBox.getMin().toBlockVector()) + " " + Util.toString(boundingBox.getMax().toBlockVector()));
            section.set("spawnOffset", Util.toString(spawnOffset));
            section.set("spawnYaw", spawnYaw);
            section.set("spawnPitch", spawnPitch);
            writeToImpl(section);
        }

        public BoundingBox getBoundingBox() {
            return boundingBox;
        }

        public void setBoundingBox(BoundingBox box) {
            this.boundingBox = box;
        }

        public Vector getSpawnOffset() {
            return spawnOffset;
        }

        public void setSpawnOffset(Vector spawnOffset) {
            this.spawnOffset = spawnOffset;
        }

        public float getSpawnYaw() {
            return spawnYaw;
        }

        public void setSpawnYaw(float spawnYaw) {
            this.spawnYaw = spawnYaw;
        }

        public float getSpawnPitch() {
            return spawnPitch;
        }

        public void setSpawnPitch(float spawnPitch) {
            this.spawnPitch = spawnPitch;
        }

        protected abstract void writeToImpl(@NotNull YMLSection section);

    }

    public abstract class DoorInstance extends DInstance<DoorType> {

        private final RoomType.RoomInstance roomInstance;
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

        public abstract class DoorHandler {

            public final DoorInstance getInstance() {
                return DoorInstance.this;
            }

        }

    }
}
