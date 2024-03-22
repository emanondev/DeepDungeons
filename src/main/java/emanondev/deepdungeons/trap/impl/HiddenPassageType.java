package emanondev.deepdungeons.trap.impl;

import emanondev.core.YMLSection;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.MoveListener;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.room.RoomType.RoomInstance.RoomHandler;
import emanondev.deepdungeons.trap.TrapType;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HiddenPassageType extends TrapType {

    public HiddenPassageType() {
        super("hiddenpassage");
    }

    @NotNull
    @Override
    public TrapInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub) {
        return new HiddenPassageType.HiddenPassageInstance(instance, sub);
    }

    @NotNull
    @Override
    public TrapBuilder getBuilder(@NotNull RoomBuilder room) {
        return new HiddenPassageType.HiddenPassageBuilder(room);
    }

    public class HiddenPassageBuilder extends TrapBuilder {

        private final HashSet<Block> blocks = new HashSet<>();
        private double activationRange = 1;

        public HiddenPassageBuilder(RoomBuilder room) {
            super(room);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) throws Exception {
            if (blocks.isEmpty())
                throw new Exception("no locations");
            List<String> blocksTxt = new ArrayList<>();
            blocks.forEach((block) -> blocksTxt.add(Util.toString(block.getLocation().toVector().toBlockVector().subtract(getRoomOffset()))));
            section.set("hiddenBlocks", blocksTxt);
            section.set("activationRange", activationRange);
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
                        CUtils.sendMsg(event.getPlayer(), "trapbuilder.hiddenpassage_msg_block_is_outside_room"); //TODO lang
                        return;
                    }
                    if (!blocks.remove(event.getClickedBlock()))
                        blocks.add(event.getClickedBlock());

                    getRoomBuilder().setupTools();
                }
                case 2 -> {
                    MapGui map = new MapGui(new DMessage(DeepDungeons.get(), p)
                            .appendLang("trapbuilder.hiddenpassage_guisettings_title"), //TODO lang
                            1, p, null, DeepDungeons.get());
                    map.setButton(4, new NumberEditorFButton<>(map, 1D, 0.1D, 10D,
                            () -> activationRange, (value) -> activationRange = CUtils.bound( value,0.1D,50D),
                            () -> CUtils.createItem(p, Material.REPEATER, "trapbuilder.hiddenpassage_settings_activationrange", //TODO lang
                                    "%maxuses%", String.valueOf(activationRange) ), true));
                    map.open(p);
                }
                case 6 -> {
                    if (blocks.size() > 0) {
                        inv.setHeldItemSlot(0);
                        this.complete();
                        return;
                    }
                    CUtils.sendMsg(p, "trapbuilder.hiddenpassage_msg_setup_incomplete"); //TODO lang
                }
            }
        }

        @Override
        protected void setupToolsImpl() {
            Player p = getPlayer();
            PlayerInventory inv = p.getInventory();
            inv.setItem(0, CUtils.createItem(p, Material.PAPER, "trapbuilder.hiddenpassage_info")); //TODO lang
            inv.setItem(1, CUtils.createItem(p, Material.STICK, blocks.size(), false, "trapbuilder.hiddenpassage_chestselector")); //TODO lang
            inv.setItem(2, CUtils.createItem(p, Material.CHEST, "trapbuilder.hiddenpassage_settings")); //TODO lang
            inv.setItem(6, CUtils.createItem(p, Material.LIME_DYE, blocks.size(), false, "trapbuilder.hiddenpassage_confirm")); //TODO lang
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            for (Block block : blocks)
                if (block != null && getRoomBuilder().getTickCounter() % 2 == 0)
                    CUtils.markBlock(player, block, color);
        }
    }

    public class HiddenPassageInstance extends TrapInstance {

        private final HashSet<BlockVector> where = new HashSet<>();
        private final double activationRange;

        public HiddenPassageInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            section.getStringList("hiddenBlocks").forEach(val -> where.add(Util.toBlockVector(val)));
            this.activationRange =  section.getDouble("activationRange", 1);
        }

        @Override
        public TrapHandler createTrapHandler(@NotNull RoomHandler roomHandler) {
            return new HiddenPassageHandler(roomHandler);
        }

        public class HiddenPassageHandler extends TrapHandler implements MoveListener {

            private final List<Block> hiddenBlocks = new ArrayList<>();
            private final List<BoundingBox> hiddenBlocksBoxes = new ArrayList<>();

            public HiddenPassageHandler(@NotNull RoomHandler roomHandler) {
                super(roomHandler);
            }

            @Override
            public void setupOffset() {
                where.forEach(loc -> hiddenBlocks.add(getWorld().getBlockAt(getRoom().getLocation().add(loc))));
            }

            @Override
            public void onFirstPlayerEnter(@NotNull Player player) {
                hiddenBlocks.removeIf(block -> block.getType().isAir()||block.getBoundingBox().getVolume()==0);
                hiddenBlocks.forEach(block -> hiddenBlocksBoxes.add(block.getBoundingBox().expand(activationRange)));
            }


            @Override
            public void onPlayerMove(@NotNull PlayerMoveEvent event) {
                if (hiddenBlocksBoxes.isEmpty()||CUtils.isEqual(event.getFrom(),event.getTo()))
                    return;
                BoundingBox playerBox = event.getPlayer().getBoundingBox();
                playerBox = new BoundingBox().expand(playerBox.getWidthX()/2,playerBox.getHeight()/2,playerBox.getWidthZ()/2).shift(event.getFrom().getX(),
                        event.getFrom().getY()+playerBox.getHeight()/2,event.getFrom().getZ());
                for (int i = 0 ; i< hiddenBlocksBoxes.size();i++){
                    if (playerBox.overlaps(hiddenBlocksBoxes.get(i))){
                        hiddenBlocksBoxes.remove(i);
                        BlockState b = hiddenBlocks.remove(i).getState();
                        BlockData data = b.getBlockData();
                        BoundingBox box = b.getBlock().getBoundingBox();
                        b.setType(Material.AIR);
                        b.update(true,true);
                        b.getWorld().spawnParticle(Particle.BLOCK_CRACK,box.getCenter().toLocation(b.getWorld()),20,
                                box.getWidthX()/2,box.getHeight()/2,box.getWidthZ()/2,data);
                        i--;
                    }
                }
            }
        }
    }
}
