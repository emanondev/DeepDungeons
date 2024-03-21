package emanondev.deepdungeons;

import emanondev.core.ItemBuilder;
import emanondev.core.message.DMessage;
import emanondev.core.util.ParticleUtility;
import emanondev.core.util.WorldEditUtility;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Compact utils
 */
@ApiStatus.Internal
public class CUtils {

    @NotNull
    @ApiStatus.Internal
    public static Color craftColorRedSpectrum(int index) {
        return switch (index % 8) {
            case 0 -> Color.fromRGB(255, 85, 85);
            case 1 -> Color.fromRGB(255, 149, 128);
            case 2 -> Color.fromRGB(254, 213, 171);
            case 3 -> Color.fromRGB(254, 242, 174);
            case 4 -> Color.fromRGB(255, 204, 100);
            case 5 -> Color.fromRGB(255, 166, 25);
            case 6 -> Color.fromRGB(255, 127, 0);
            default -> Color.fromRGB(255, 87, 0);
        };
    }

    @NotNull
    @ApiStatus.Internal
    public static Color craftColorRainbow(int index) {
        return switch (index % 15) {
            case 0 -> Color.fromRGB(85, 255, 85);
            case 1 -> Color.fromRGB(85, 255, 182);
            case 2 -> Color.fromRGB(85, 231, 255);
            case 3 -> Color.fromRGB(85, 134, 255);
            case 4 -> Color.fromRGB(117, 106, 250);
            case 5 -> Color.fromRGB(182, 149, 241);
            case 6 -> Color.fromRGB(222, 128, 246);
            case 7 -> Color.fromRGB(255, 85, 255);
            case 8 -> Color.fromRGB(255, 85, 158);
            case 9 -> Color.fromRGB(255, 97, 73);
            case 10 -> Color.fromRGB(255, 146, 24);
            case 11 -> Color.fromRGB(255, 194, 24);
            case 12 -> Color.fromRGB(255, 243, 73);
            case 13 -> Color.fromRGB(229, 255, 110);
            default -> Color.fromRGB(195, 255, 143);
        };
    }

    @ApiStatus.Internal
    public static void setSlot(Player player, int slot, Inventory inv, Material material, String langPath, String... holders) {
        inv.setItem(slot, createItem(player, material, 1, false, langPath, holders));
    }

    @ApiStatus.Internal
    public static void setSlot(Player player, int slot, Inventory inv, Material material, int amount, boolean enchant, String langPath, String... holders) {
        inv.setItem(slot, createItem(player, material, amount, enchant, langPath, holders));
    }

    @NotNull
    @ApiStatus.Internal
    public static ItemBuilder createIBuilder(Player player, Material material, String langPath, String... holders) {
        return createIBuilder(player, material, 1, false, langPath, holders);
    }

    @NotNull
    @ApiStatus.Internal
    public static ItemBuilder createIBuilder(Player player, Material material, int amount, boolean enchant, String langPath, String... holders) {
        return new ItemBuilder(material).setGuiProperty().setAmount(Math.min(64, Math.max(1, amount))).addEnchantment(Enchantment.DURABILITY, enchant ? 1 : 0)
                .setDescription(new DMessage(DeepDungeons.get(), player).appendLang(langPath, holders));
    }

    @NotNull
    @ApiStatus.Internal
    public static ItemBuilder emptyIBuilder(Material material) {
        return new ItemBuilder(material).setGuiProperty();
    }

    @NotNull
    @ApiStatus.Internal
    public static ItemBuilder emptyIBuilder(Material material, int amount, boolean enchant) {
        return new ItemBuilder(material).setGuiProperty().setAmount(Math.min(64, Math.max(1, amount))).addEnchantment(Enchantment.DURABILITY, enchant ? 1 : 0);
    }

    @NotNull
    @ApiStatus.Internal
    public static ItemStack createItem(Player player, Material material, String langPath, String... holders) {
        return createIBuilder(player, material, 1, false, langPath, holders).build();
    }

    @NotNull
    @ApiStatus.Internal
    public static ItemStack createItem(Player player, Material material, int amount, boolean enchant, String langPath, String... holders) {
        return createIBuilder(player, material, amount, enchant, langPath, holders).build();
    }

    @ApiStatus.Internal
    public static void sendMsg(CommandSender player, String langPath, String... holders) {
        craftMsg(player, langPath, holders).send();
    }

    @ApiStatus.Internal
    public static DMessage craftMsg(CommandSender player, String langPath, String... holders) {
        return new DMessage(DeepDungeons.get(), player).appendLang(langPath, holders);
    }

    @ApiStatus.Internal
    public static DMessage emptyMsg(CommandSender player) {
        return new DMessage(DeepDungeons.get(), player);
    }

    @ApiStatus.Internal
    public static void markBlock(Player player, Block block, Color color) {
        markBlock(player, block.getLocation().toVector().toBlockVector(), color);
    }

    @ApiStatus.Internal
    public static void markBlock(Player player, BlockVector block, Color color) {
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


    public static void showWEBound(@NotNull Player player, int tick) {
        try {
            if (WorldEditUtility.getSelectionRegion(player) != null)
                ParticleUtility.spawnParticleBoxFaces(player, tick / 6 + 6, 4, Particle.REDSTONE,
                        WorldEditUtility.getSelectionBoxExpanded(player), new Particle.DustOptions(Color.WHITE, 0.3F));
        } catch (Exception ignored) {
        }
    }

    public static void showArrow(@NotNull Player player, @NotNull Color color, BlockFace direction, Vector from) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.3F);
        Vector r = from.clone().add(direction.getDirection().multiply(0.7));
        Vector dir = direction.getOppositeFace().getDirection();
        ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir,
                0.7, 0.1, dust);
        if (direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
            dir.add(new Vector(0, 0.4, 0));
            for (int i = 0; i < 8; i++) {
                dir.rotateAroundZ(Math.PI / 4);
                ParticleUtility.spawnParticleLine(player, Particle.REDSTONE, r.getX(), r.getY(), r.getZ(), dir, 0.3, 0.1, dust);
            }
        } else if (direction == BlockFace.EAST || direction == BlockFace.WEST) {
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

    public static String toText(boolean value) {
        return value ? "<green>true</green>" : "<red>false</red>";
    }

    public static String toText(Vector value) {
        return value == null ? null : Util.toString(value).replace(";", " ");
    }
}
