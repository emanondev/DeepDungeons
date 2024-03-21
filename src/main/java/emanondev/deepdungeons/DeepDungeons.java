package emanondev.deepdungeons;

import emanondev.core.CorePlugin;
import emanondev.deepdungeons.area.AreaManager;
import emanondev.deepdungeons.command.*;
import emanondev.deepdungeons.door.DoorTypeManager;
import emanondev.deepdungeons.dungeon.DungeonInstanceManager;
import emanondev.deepdungeons.dungeon.DungeonTypeManager;
import emanondev.deepdungeons.paperpopulator.PopulatorTypeManager;
import emanondev.deepdungeons.party.PartyManager;
import emanondev.deepdungeons.room.RoomInstanceManager;
import emanondev.deepdungeons.room.RoomTypeManager;
import emanondev.deepdungeons.spawner.MonsterSpawnerTypeManager;
import emanondev.deepdungeons.trap.TrapTypeManager;
import emanondev.deepdungeons.treasure.TreasureTypeManager;

/**
 * Plugin main class
 * <p>
 * For managers use getInstance() static method on respective classes
 *
 * @see DungeonTypeManager
 * @see DungeonInstanceManager
 * @see RoomTypeManager
 * @see RoomInstanceManager
 * @see DoorTypeManager
 * @see TrapTypeManager
 * @see TreasureTypeManager
 * @see MonsterSpawnerTypeManager
 * @see AreaManager
 */
public class DeepDungeons extends CorePlugin {

    private static DeepDungeons instance;

    /**
     * @return instance of Plugin
     */
    public static DeepDungeons get() {
        return instance;
    }

    @Override
    protected boolean registerReloadCommand() {
        return false;
    }

    @Override
    public void disable() {
        BuilderMode.getInstance().disable();
    }

    @Override
    public void reload() {

    }

    @Override
    public void enable() {

        //TreasureTypeManager.getInstance().load();
        TrapTypeManager.getInstance().load();
        PopulatorTypeManager.getInstance().load();
        //MonsterSpawnerTypeManager.getInstance().load();
        DoorTypeManager.getInstance().load();
        RoomTypeManager.getInstance().load();
        RoomInstanceManager.getInstance().load();
        DungeonTypeManager.getInstance().load();
        DungeonInstanceManager.getInstance().load();
        PartyManager.getInstance().load();
        BuilderMode.getInstance();
        AreaManager.getInstance();
        //this.registerCommand(new DungeonTreasureCommand());
        //this.registerCommand(new DungeonMonsterSpawnerCommand());
        this.registerCommand(new DungeonPopulatorCommand());
        this.registerCommand(new DungeonRoomBuilderCommand());
        this.registerCommand(new DungeonDungeonBuilderCommand());
        this.registerCommand(new DungeonCreatorCommand());
        this.registerCommand(new DungeonPartyCommand());
    }

    @Override
    public void load() {
        instance = this;
    }

}
