package emanondev.deepdungeons.dungeon;

import emanondev.core.YMLConfig;
import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import emanondev.deepdungeons.dungeon.DungeonType.DungeonInstance;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.regex.Pattern;

public class DungeonInstanceManager extends DRegistry<DungeonInstance> {

    private static final DungeonInstanceManager instance = new DungeonInstanceManager();

    private DungeonInstanceManager() {
        super(DeepDungeons.get(), "DungeonInstanceManager", true);
    }

    @NotNull
    public static DungeonInstanceManager getInstance() {
        return instance;
    }

    @Override
    public void load() {
        File dungeonsFolder = getFolder();
        if (!dungeonsFolder.isDirectory()) {
            //TODO
            return;
        }
        File[] files = dungeonsFolder.listFiles(File::isFile);
        if (files != null) for (File file : files) {
            try {
                DungeonInstance dungeon = readInstance(file);
                if (dungeon != null)
                    DungeonInstanceManager.getInstance().register(dungeon);
            } catch (Throwable t) {
                t.printStackTrace();
                logIssue("Can't read dungeon file &edungeons" + File.separator + file.getName() + "&f please report the above stack trace to the developer ( https://discord.com/invite/w5HVCDPtRp )");
                continue;
            }
        }
    }

    @NotNull
    public File getFolder() {
        return new File(DeepDungeons.get().getDataFolder(), "dungeons");
    }

    public DungeonInstance readInstance(@NotNull File file) {
        String fileName = file.getName();
        if (!fileName.endsWith(".yml")) {
            logIssue("Can't read dungeon file &edungeons" + File.separator + file.getName() + "&f because it's not ending with &e.yml (was it manually edited? is that file supposed to be in that folder?)");
            return null;
        }
        fileName = fileName.substring(0, fileName.length() - 4);
        if (!Pattern.compile("[a-z][_a-z0-9]+").matcher(fileName).matches()) {
            logIssue("Can't read dungeon file &edungeons" + File.separator + file.getName() + "&f because name it's not valid (Regex: &e[a-z][_a-z0-9]+&f) (was it manually edited?)");
            return null;
        }
        return readInstance(new YMLConfig(DeepDungeons.get(), file), fileName);
    }

    private DungeonInstance readInstance(@NotNull YMLConfig config, String fileName) {
        String type = config.getString("type");
        if (type == null) {
            logIssue("Can't read dungeon file &edungeons" + File.separator + fileName + "&f because path &etype:&f leads to nothing (corrupted file?)");
            return null;
        }
        DungeonType dungeonType = DungeonTypeManager.getInstance().get(type);
        if (dungeonType == null) {
            logIssue("Can't read dungeon file &edungeons" + File.separator + fileName + "&f because dungeon type &etype &fdoesn't match any existing type (maybe a 3rd party plugin didn't load or was removed?)");
            return null;
        }
        DungeonInstance dungeon = null;
        try {
            dungeon = dungeonType.read(fileName, config);
            //DungeonInstanceManager.getInstance().register(dungeon);
        } catch (Throwable t) {
            t.printStackTrace();
            logIssue("Can't read dungeon file &edungeons" + File.separator + fileName + "&f please report the above stack trace to the developer ( https://discord.com/invite/w5HVCDPtRp )");
            return null;
        }
        return dungeon;
    }
}
