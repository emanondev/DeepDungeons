package emanondev.deepdungeons.room;

import emanondev.core.YMLConfig;
import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.regex.Pattern;

public class RoomInstanceManager extends DRegistry<RoomType.RoomInstance> {
    private static final RoomInstanceManager instance = new RoomInstanceManager();

    private RoomInstanceManager() {
        super(DeepDungeons.get(), "RoomInstanceManager", true);
    }

    public static @NotNull RoomInstanceManager getInstance() {
        return instance;
    }

    public RoomType.RoomInstance readInstance(@NotNull File file) {
        String fileName = file.getName();
        if (!fileName.endsWith(".yml")) {
            logIssue("Can't read room file &erooms" + File.separator + file.getName() + "&f because it's not ending with &e.yml (was it manually edited? is that file supposed to be in that folder?)");
            return null;
        }
        fileName = fileName.substring(0, fileName.length() - 4);
        if (!Pattern.compile("[a-z][_a-z0-9]+").matcher(fileName).matches()) {
            logIssue("Can't read room file &erooms" + File.separator + file.getName() + "&f because name it's not valid (Regex: &e[a-z][_a-z0-9]+&f) (was it manually edited?)");
            return null;
        }
        return readInstance(new YMLConfig(DeepDungeons.get(), file), fileName);
    }

    private RoomType.RoomInstance readInstance(@NotNull YMLConfig config, String fileName) {
        String type = config.getString("type");
        if (type == null) {
            logIssue("Can't read room file &erooms" + File.separator + config.getFile().getName() + "&f because path &etype:&f leads to nothing (corrupted file?)");
            return null;
        }
        RoomType roomType = RoomTypeManager.getInstance().get(type);
        if (roomType == null) {
            logIssue("Can't read room file &erooms" + File.separator + config.getFile().getName() + "&f because room type &etype &fdoesn't match any existing type (maybe a 3rd party plugin didn't load or was removed?)");
            return null;
        }
        RoomType.RoomInstance room = null;
        try {
            room = roomType.read(fileName, config);
            //RoomInstanceManager.getInstance().register(room);
        } catch (Throwable t) {
            t.printStackTrace();
            logIssue("Can't read room file &erooms" + File.separator + config.getFile().getName() + "&f please report the above stack trace to the developer ( https://discord.com/invite/w5HVCDPtRp )");
            return null;
        }
        return room;
    }

    @Override
    public void load() {
        File roomsFolder = getFolder();
        if (!roomsFolder.isDirectory()) {
            //TODO
            return;
        }
        File[] files = roomsFolder.listFiles(File::isFile);
        if (files != null) for (File file : files) {
            try {
                RoomType.RoomInstance room = readInstance(file);
                if (room != null)
                    RoomInstanceManager.getInstance().register(room);
            } catch (Throwable t) {
                t.printStackTrace();
                logIssue("Can't read room file &erooms" + File.separator + file.getName() + "&f please report the above stack trace to the developer ( https://discord.com/invite/w5HVCDPtRp )");
                continue;
            }
        }

    }

    public @NotNull File getFolder() {
        return new File(DeepDungeons.get().getDataFolder(), "rooms");
    }
}
