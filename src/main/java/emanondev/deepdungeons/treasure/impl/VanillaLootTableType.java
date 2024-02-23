package emanondev.deepdungeons.treasure.impl;

import emanondev.deepdungeons.treasure.TreasureInstance;
import emanondev.deepdungeons.treasure.TreasureType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;

public class VanillaLootTableType extends TreasureType {

    public VanillaLootTableType() {
        super("vanillaloottable");
    }


    public class VanillaLootTableInstance extends TreasureInstance {

        private LootTable table;

        public VanillaLootTableInstance() {
            super(VanillaLootTableType.this);
        }

        @Override
        public @NotNull Collection<ItemStack> getTreasure(@NotNull Random random, @NotNull Location location, @Nullable Player who) {
            return table.populateLoot(random, new LootContext.Builder(location).killer(who).lootingModifier(0).build());
        }
    }
}
