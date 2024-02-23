package emanondev.deepdungeons.reward;

import emanondev.deepdungeons.parameter.Parameters;
import emanondev.deepdungeons.roomold.handler.RoomHandler;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pro.dracarys.DracarysGUI.api.DracarysGUIAPI;
import pro.dracarys.DracarysGUI.utils.Util;

import java.util.List;
import java.util.Map;

public class DracarysGuiRewardProvider extends ARewardProvider {

    public DracarysGuiRewardProvider() {
        super("dracarysgui");
    }

    @Override
    public void populate(List<String> info, Inventory inventory, RoomHandler handler) {
        try {
            int chance = Parameters.CHANCE.readValue(info);
            if (chance < 100 && Math.random() * 100 > chance) {
                if (inventory.getHolder() instanceof BlockInventoryHolder) {
                    //TODO add chance to control behavior
                    ((BlockInventoryHolder) inventory.getHolder()).getBlock().setType(Material.AIR);
                }
                return;
            }
            int amount = Parameters.AMOUNT.readValue(info);
            String group = Parameters.DRACARYSGUILOOTGROUP.readValue(info);
            Map<ItemStack, Double> map = DracarysGUIAPI.getLootGroupItems(group);
            for (int i = 0; i < amount; i++)
                inventory.addItem(Util.getFromWeightedMap(map));
        } catch (Exception e) {

        }
    }

    @Override
    public DracarysGuiStandGui setupGui(Player user, ArmorStand stand) {
        return new DracarysGuiStandGui(user, stand);
    }

    public class DracarysGuiStandGui extends RewardStandGui {

        public DracarysGuiStandGui(Player player, ArmorStand stand) {
            super(player, stand, DracarysGuiRewardProvider.this);
        }

        public void registerParams() {
            super.registerParams();
            this.registerParam(Parameters.AMOUNT, 9);
            this.registerParam(Parameters.DRACARYSGUILOOTGROUP, 0);
        }

    }

}
