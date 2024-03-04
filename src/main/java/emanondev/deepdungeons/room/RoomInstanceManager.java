package emanondev.deepdungeons.room;

import emanondev.core.YMLConfig;
import emanondev.core.util.DRegistry;
import emanondev.deepdungeons.DeepDungeons;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.regex.Pattern;

public class RoomInstanceManager extends DRegistry<RoomType.RoomInstance> {
    private static final RoomInstanceManager instance = new RoomInstanceManager();

    private RoomInstanceManager() {
        super(DeepDungeons.get(), "RoomInstanceManager", true);
        Bukkit.broadcastMessage("created");
    }

    @Override
    public void load() {
        Bukkit.broadcastMessage("load");
        File roomsFolder = new File(DeepDungeons.get().getDataFolder(), "rooms");
        if (!roomsFolder.isDirectory()) {
            //TODO
            return;
        }
        Bukkit.broadcastMessage("Path "+roomsFolder.getAbsolutePath());
        File[] files2 = roomsFolder.listFiles();
        Bukkit.broadcastMessage("files2 "+(files2==null?0:files2.length));

        File[] files = roomsFolder.listFiles(File::isFile);
        Bukkit.broadcastMessage("files "+(files==null?0:files.length));

        if (files!=null) for (File file:files){
            String fileName = file.getName();
            if(!fileName.endsWith(".yml")) {
                logIssue("Can't read room file &erooms" +File.separator+ file.getName() + "&f because it's not ending with &e.yml (was it manually edited? is that file supposed to be in that folder?)");
                continue;
            }
            fileName = fileName.substring(0,fileName.length()-4);
            if (!Pattern.compile("[a-z][_a-z0-9]+").matcher(fileName).matches()){
                logIssue("Can't read room file &erooms"+File.separator+file.getName()+"&f because name it's not valid (Regex: &e[a-z][_a-z0-9]+&f) (was it manually edited?)");
                continue;
            }
            YMLConfig config = new YMLConfig(DeepDungeons.get(), file);
            String type = config.getString("type");
            if (type==null ){
                logIssue("Can't read room file &erooms"+File.separator+file.getName()+"&f because path &etype:&f leads to nothing (corrupted file?)");
                continue;
            }
            RoomType roomType = RoomTypeManager.getInstance().get(type);
            if (roomType==null){
                logIssue("Can't read room file &erooms"+File.separator+file.getName()+"&f because room type &etype &fdoesn't match any existing type (maybe a 3rd party plugin didn't load or was removed?)");
                continue;
            }
            RoomType.RoomInstance room;
            try{
                room = roomType.read(fileName,config);
                RoomInstanceManager.getInstance().register(room);
            } catch (Throwable t){
                t.printStackTrace();
                logIssue("Can't read room file &erooms"+File.separator+file.getName()+"&f please report the above stack trace to the developer ( https://discord.com/invite/w5HVCDPtRp )");
                continue;
            }
        }

    }

    public static @NotNull RoomInstanceManager getInstance() {
        return instance;
    }
}
