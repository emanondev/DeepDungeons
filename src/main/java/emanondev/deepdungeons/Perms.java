package emanondev.deepdungeons;

import emanondev.core.PermissionBuilder;
import org.bukkit.permissions.Permission;


/**
 * Container for Plugin permissions
 */
public class Perms {

    private Perms(){
        throw new AssertionError();
    }

    public static final Permission DUNGEONTREASURE_COMMAND = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeontreasure").buildAndRegister(DeepDungeons.get());

    public static final Permission DUNGEONMONSTERSPAWNER_COMMAND = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonmonsterspawner").buildAndRegister(DeepDungeons.get());
    public static final Permission DUNGEONDUNGEON_COMMAND = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeondungeon").buildAndRegister(DeepDungeons.get());
    public static final Permission DUNGEONROOM_COMMAND = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonroom").buildAndRegister(DeepDungeons.get());

    public static final Permission DUNGEONCREATOR_COMMAND = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeoncreator").buildAndRegister(DeepDungeons.get());

    public static final Permission PARTY_HELP = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "help").buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_JOIN = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "join").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_DISBAND = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "disband").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_CREATE = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "create").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_LEAVE = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "leave").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_TOGGLE_PUBLIC = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "toggle_public").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_INVITE = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "invite").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_KICK = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "kick").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_LEADER = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "leader").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_LIST = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "list").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());
    public static final Permission PARTY_INFO = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeonparty", "info").addChild(PARTY_HELP, true).buildAndRegister(DeepDungeons.get());


    public static final Permission PARTY_ALL = PermissionBuilder.ofCommand(DeepDungeons.get(),
                    "dungeonparty")
            .addChild(PARTY_HELP, true)
            .addChild(PARTY_JOIN, true)
            .addChild(PARTY_DISBAND, true)
            .addChild(PARTY_CREATE, true)
            .addChild(PARTY_LEAVE, true)
            .addChild(PARTY_TOGGLE_PUBLIC, true)
            .addChild(PARTY_INVITE, true)
            .addChild(PARTY_KICK, true)
            .addChild(PARTY_LEADER, true)
            .addChild(PARTY_LIST, true)
            .addChild(PARTY_INFO, true)
            .buildAndRegister(DeepDungeons.get());
    ;

    public static final Permission DUNGEONTREASURE_ADMIN = PermissionBuilder.ofPlugin(DeepDungeons.get(), "admin")
            .addChild(PARTY_ALL, true)
            .addChild(DUNGEONROOM_COMMAND, true)
            .addChild(DUNGEONDUNGEON_COMMAND, true)
            .addChild(DUNGEONTREASURE_COMMAND, true)
            .addChild(DUNGEONCREATOR_COMMAND, true)
            .addChild(DUNGEONMONSTERSPAWNER_COMMAND, true).buildAndRegister(DeepDungeons.get());
}
