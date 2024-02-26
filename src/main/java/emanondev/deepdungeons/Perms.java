package emanondev.deepdungeons;

import emanondev.core.PermissionBuilder;
import org.bukkit.permissions.Permission;

public class Perms {

    public static final Permission DUNGEONTREASURE_COMMAND = PermissionBuilder.ofCommand(DeepDungeons.get(),
            "dungeontreasure").buildAndRegister(DeepDungeons.get());


    public static final Permission DUNGEONTREASURE_ADMIN = PermissionBuilder.ofPlugin(DeepDungeons.get(), "admin")
            .addChild(DUNGEONTREASURE_COMMAND, true).buildAndRegister(DeepDungeons.get());

}
