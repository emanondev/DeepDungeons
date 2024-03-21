package emanondev.deepdungeons.trap.impl;

import emanondev.core.RandomItemContainer;
import emanondev.core.UtilsString;
import emanondev.core.YMLSection;
import emanondev.core.gui.FButton;
import emanondev.core.gui.MapGui;
import emanondev.core.gui.NumberEditorFButton;
import emanondev.core.gui.PagedMapGui;
import emanondev.core.message.DMessage;
import emanondev.deepdungeons.CUtils;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.Util;
import emanondev.deepdungeons.interfaces.InteractListener;
import emanondev.deepdungeons.room.RoomType.RoomBuilder;
import emanondev.deepdungeons.room.RoomType.RoomInstance;
import emanondev.deepdungeons.trap.TrapType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FlameChestType extends TrapType {

    public FlameChestType() {
        super("flameshootingchest");
    }

    @NotNull
    @Override
    public TrapInstance read(@NotNull RoomInstance instance, @NotNull YMLSection sub) {
        return new FlameChestInstance(instance, sub);
    }

    @NotNull
    @Override
    public TrapBuilder getBuilder(@NotNull RoomBuilder room) {
        return new FlameChestBuilder(room);
    }

    public enum Type {
        FIRE(Particle.FLAME, Sound.BLOCK_FIRE_AMBIENT, 0.3F, 0.28),
        FREEZE(Particle.CLOUD, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.3F, 0.28),
        POISON(Particle.SNEEZE, Sound.ENTITY_PANDA_PRE_SNEEZE, 1F, 0.28),
        WITHER(Particle.SMOKE_NORMAL, Sound.ENTITY_WITHER_SHOOT, 0.3F, 0.33),
        NONE(null, null, 1F, 1);

        private final Sound sound;


        private final float soundPitch;
        private final Particle particle;
        private final double speed;

        Type(Particle particle, Sound sound, float soundPitch, double speed) {
            this.particle = particle;
            this.speed = speed;
            this.sound = sound;
            this.soundPitch = soundPitch;
        }

        public static Type fromId(String id) {
            return valueOf(id.toUpperCase(Locale.ENGLISH));
        }

        public float getSoundPitch() {
            return soundPitch;
        }

        public Sound getSound() {
            return sound;
        }

        public Particle getParticle() {
            return particle;
        }

        public double getSpeed() {
            return speed;
        }

        public String getId() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public void apply(Collection<Entity> target, int ticks) {
            switch (this) {
                case FIRE -> target.forEach(t -> t.setFireTicks(ticks + Math.max(t.getFireTicks(), 10)));
                case FREEZE -> target.forEach(t -> t.setFreezeTicks(ticks + Math.max(t.getFreezeTicks(), 130)));
                case POISON -> target.forEach(t -> {
                    if (t instanceof LivingEntity liv) {
                        PotionEffect poison = liv.getPotionEffect(PotionEffectType.POISON);
                        liv.addPotionEffect(new PotionEffect(PotionEffectType.POISON, (poison == null ? 10 :
                                poison.getDuration()) + ticks, 0, false, false, false));
                    }
                });
                case WITHER -> target.forEach(t -> {
                    if (t instanceof LivingEntity liv) {
                        PotionEffect wither = liv.getPotionEffect(PotionEffectType.WITHER);
                        liv.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (wither == null ? 10 :
                                wither.getDuration()) + ticks, 0, false, false, false));
                    }
                });
            }
        }
    }

    public class FlameChestBuilder extends TrapBuilder {

        private final HashSet<Block> blocks = new HashSet<>();
        private final HashMap<Type, Integer> weights = new HashMap<>();
        private boolean takeAim = false;
        private int maxTicks = 100;
        private int addedTicks = 8;
        private int maxUses = 1;

        public FlameChestBuilder(RoomBuilder room) {
            super(room);
            for (Type type : Type.values())
                weights.put(type, type == Type.FIRE ? 10 : 0);
        }

        @Override
        protected void writeToImpl(@NotNull YMLSection section) {
            List<String> blocksTxt = new ArrayList<>();
            blocks.forEach((block) -> blocksTxt.add(Util.toString(block.getLocation().toVector().toBlockVector().subtract(getRoomOffset()))));
            section.set("chests", blocksTxt);
            section.set("directionedFlame", takeAim);
            section.set("maxTicks", maxTicks);
            section.set("addedTicks", addedTicks);
            section.set("maxUses", maxUses == 0 ? -1 : maxUses);
            weights.forEach((t, v) -> {
                if (v > 0)
                    section.set("flameType." + t.getId(), v);
            });
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
                        CUtils.sendMsg(event.getPlayer(), "trapbuilder.flamechest_msg_block_is_outside_room");
                        return;
                    }
                    if (!blocks.remove(event.getClickedBlock())) {
                        if (!(event.getClickedBlock().getState() instanceof Chest)) {
                            CUtils.sendMsg(event.getPlayer(), "trapbuilder.flamechest_msg_block_is_not_a_chest");
                            return;
                        }
                        blocks.add(event.getClickedBlock());
                    }
                    getRoomBuilder().setupTools();
                }
                case 2 -> {
                    MapGui map = new MapGui(new DMessage(DeepDungeons.get(), p)
                            .appendLang("trapbuilder.flamechest_guisettings_title"),
                            1, p, null, DeepDungeons.get());
                    map.setButton(0, new FButton(map, () ->
                            CUtils.createItem(p, Material.ARROW, "trapbuilder.flamechest_settings_takeaim",
                                    "%directioned%", CUtils.toText(takeAim))
                            , (evt) -> {
                        takeAim = !takeAim;
                        return true;
                    }));
                    map.setButton(2, new NumberEditorFButton<>(map, 1, 1, 10,
                            () -> maxTicks, (value) -> maxTicks = Math.max(1, value),
                            () -> CUtils.createItem(p, Material.REPEATER, "trapbuilder.flamechest_settings_maxticks",
                                    "%maxticks%", String.valueOf(maxTicks), "%maxseconds%", String.valueOf(maxTicks / 20)), true));
                    map.setButton(4, new NumberEditorFButton<>(map, 1, 1, 10,
                            () -> addedTicks, (value) -> addedTicks = Math.max(1, value),
                            () -> CUtils.createItem(p, Material.REPEATER, "trapbuilder.flamechest_settings_addedticks",
                                    "%addedticks%", String.valueOf(addedTicks)), true));
                    map.setButton(6, new NumberEditorFButton<>(map, 1, 1, 10,
                            () -> maxUses, (value) -> maxUses = Math.max(0, value),
                            () -> CUtils.createItem(p, Material.REPEATER, "trapbuilder.flamechest_settings_maxuses",
                                    "%maxuses%", maxUses > 0 ? String.valueOf(maxUses) : "∞"), true));


                    map.setButton(8, new FButton(map, () -> CUtils.createItem(p, Material.FLINT_AND_STEEL,
                            "trapbuilder.flamechest_settings_typeweights", getTypeHolders()), (evt) -> {
                        PagedMapGui subGui = new PagedMapGui(CUtils.craftMsg(p,
                                "trapbuilder.flamechest_settings_typeguititle"), 6, p, map, DeepDungeons.get());
                        for (Type type : Type.values()) {
                            subGui.addButton(new NumberEditorFButton<>(subGui, 10, 1, 1000, () -> weights.get(type),
                                    (value) -> {
                                        weights.put(type, Math.max(0, value));
                                        final int[] sum = {0};
                                        weights.values().forEach(v -> sum[0] += v);
                                        if (sum[0] == 0)
                                            weights.put(type, 1);
                                    }, () -> CUtils.createItem(p, Material.REPEATER, weights.get(type), false,
                                    "trapbuilder.flamechest_settings_typeweight_" + type.getId(), getTypeHolders()), true));
                        }
                        subGui.open(p);
                        return false;
                    }));
                    map.open(p);
                }
                case 6 -> {
                    if (blocks.size() > 0) {
                        inv.setHeldItemSlot(0);
                        this.complete();
                        return;
                    }
                    CUtils.sendMsg(p, "trapbuilder.flamechest_msg_setup_incomplete");
                }
            }
        }

        private String[] getTypeHolders() {
            final int[] sum = {0};
            weights.values().forEach(v -> sum[0] += v);
            String[] holders = new String[Type.values().length * 4 + 2];
            for (Type kind : Type.values()) {
                holders[kind.ordinal() * 4] = "%" + kind.getId() + "%";
                holders[kind.ordinal() * 4 + 1] = String.valueOf(weights.get(kind));
                holders[kind.ordinal() * 4 + 2] = "%" + kind.getId() + "_perc%";
                holders[kind.ordinal() * 4 + 3] = UtilsString.formatOptional2Digit(weights.get(kind) * 100D / sum[0]);
            }
            holders[holders.length - 2] = "%max%";
            holders[holders.length - 1] = String.valueOf(sum[0]);
            return holders;
        }

        @Override
        protected void setupToolsImpl() {
            Player p = getPlayer();
            PlayerInventory inv = p.getInventory();
            inv.setItem(0, CUtils.createItem(p, Material.PAPER, "trapbuilder.flamechest_info"));
            inv.setItem(1, CUtils.createItem(p, Material.STICK, blocks.size(), false, "trapbuilder.flamechest_chestselector"));
            inv.setItem(2, CUtils.createItem(p, Material.CHEST, "trapbuilder.flamechest_settings"));
            inv.setItem(6, CUtils.createItem(p, Material.LIME_DYE, blocks.size(), false, "trapbuilder.flamechest_confirm"));
        }

        @Override
        protected void tickTimerImpl(@NotNull Player player, @NotNull Color color) {
            for (Block block : blocks)
                if (block != null && getRoomBuilder().getTickCounter() % 2 == 0)
                    CUtils.markBlock(player, block, color);
        }
    }

    public class FlameChestInstance extends TrapInstance {

        private final HashSet<BlockVector> where = new HashSet<>();
        private final boolean directioned;
        private final int maxTicks;
        private final int addedTicks;
        private final int maxUses;
        private final RandomItemContainer<Type> randomType = new RandomItemContainer<>();

        public FlameChestInstance(@NotNull RoomInstance room, @NotNull YMLSection section) {
            super(room, section);
            section.getStringList("chests").forEach(val -> where.add(Util.toBlockVector(val)));
            this.directioned = section.getBoolean("directionedFlame", false);
            this.maxTicks = section.getInt("maxTicks", 100);
            this.addedTicks = section.getInt("addedTicks", 8);
            this.maxUses = Math.max(-1, section.getInt("maxUses", 1));
            for (Type type : Type.values()) {
                int val = section.getInt("flameType." + type.getId(), 0);
                if (val > 0)
                    this.randomType.addItem(type, val);
            }
            if (randomType.getFullWeight() == 0)
                randomType.addItem(Type.FIRE, 1);
        }

        @Override
        public TrapHandler createTrapHandler(@NotNull RoomInstance.RoomHandler roomHandler) {
            return new FlameChestHandler(roomHandler);
        }

        private Type craftType() {
            return randomType.getItem();
        }

        public class FlameChestHandler extends TrapHandler implements InteractListener {

            //private final Type type
            private final HashMap<Block, Integer> usesHandler = new HashMap<>();
            private final HashMap<Block, Type> typeHandler = new HashMap<>();
            private final HashMap<Block, Long> cooldownHandler = new HashMap<>();
            //private int uses = maxUses;

            public FlameChestHandler(@NotNull RoomInstance.RoomHandler roomHandler) {
                super(roomHandler);
                //type = FlameChestInstance.this.craftType();
            }

            @Override
            public void setupOffset() {
                where.forEach(loc -> {
                    Type type = FlameChestInstance.this.craftType();
                    if (type == Type.NONE)
                        return;
                    typeHandler.put(getWorld().getBlockAt(getRoom().getLocation().add(loc)), type);
                    usesHandler.put(getWorld().getBlockAt(getRoom().getLocation().add(loc)), maxUses);
                });
                //whereHandler = getWorld().getBlockAt(getRoom().getLocation().add(where));
            }

            @Override
            public void onFirstPlayerEnter(@NotNull Player player) {

            }

            @Override
            public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
                Block block = event.getClickedBlock();
                int uses = usesHandler.getOrDefault(block, 0);
                if (uses == 0)
                    return;
                if (!(block.getState() instanceof Chest chest)) {
                    usesHandler.remove(block);
                    typeHandler.remove(block);
                    return;
                }
                Long val = cooldownHandler.getOrDefault(block, null);
                long now = System.currentTimeMillis();
                if (val != null && val > now)
                    return;
                Location center = block.getLocation().add(0.5, 10 / 16F, 0.5);
                Vector direction;
                if (directioned)
                    direction = event.getPlayer().getBoundingBox().getCenter().subtract(center.toVector()).normalize();
                else
                    direction = ((Directional) block.getBlockData()).getFacing().getDirection();

                getWorld().playSound(center, Sound.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1F, 1F);
                Type type = typeHandler.get(block);
                if (uses > 0) { //-1 = unlimited
                    uses--;
                    if (uses == 0) {
                        usesHandler.remove(block);
                        typeHandler.remove(block);
                        cooldownHandler.remove(block);
                    } else {
                        usesHandler.put(block, uses);
                        cooldownHandler.put(block, now + (maxTicks + 5 + 2) * 50L);
                    }
                } else
                    cooldownHandler.put(block, now + (maxTicks + 5 + 2) * 50L);

                chest.open();
                new BukkitRunnable() {
                    private int counter = 0;

                    @Override
                    public void run() {
                        counter++;
                        if (!(block.getState() instanceof Chest chest)) {
                            this.cancel();
                            return;
                        }
                        if (counter >= maxTicks + 5) {
                            this.cancel();
                            chest.close();
                            return;
                        }
                        List<Vector> spreaded = new ArrayList<>();//TODO 2 per tick, configurable?
                        for (int i = 0; i < 2; i++) {
                            Vector dir = direction.clone().add(new Vector(
                                    (Math.random() - 0.5) / 3, (Math.random() - 0.5) / 3, (Math.random() - 0.5) / 3));//TODO spread configurabile 1/3 atm
                            spreaded.add(dir);
                            getWorld().spawnParticle(type.getParticle(), center, 0, dir.getX(), dir.getY(), dir.getZ(), type.getSpeed());
                        }
                        if (counter < maxTicks - 5 && counter % 5 == 0)
                            getWorld().playSound(center, type.getSound(), SoundCategory.BLOCKS, 1F, type.getSoundPitch());
                        if (counter > 5) { //give particle time to get to target
                            HashSet<Entity> target = new HashSet<>();
                            spreaded.forEach(dir -> {
                                RayTraceResult ray = getWorld().rayTraceEntities(center, dir, 5.5, 0.1);
                                if (ray == null || ray.getHitEntity() == null)
                                    return;
                                RayTraceResult back = getWorld().rayTraceBlocks(ray.getHitPosition().toLocation(getWorld()),
                                        dir.multiply(-1), 5.5, FluidCollisionMode.ALWAYS, true);
                                if (back == null || !block.equals(back.getHitBlock()))
                                    return;
                                target.add(ray.getHitEntity());
                            });
                            type.apply(target, addedTicks);
                        }

                    }
                }.runTaskTimer(DeepDungeons.get(), 2L, 1L);
            }
        }
    }
}
