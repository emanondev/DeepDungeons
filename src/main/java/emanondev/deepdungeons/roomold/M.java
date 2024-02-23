package emanondev.deepdungeons.roomold;

import emanondev.core.MessageBuilder;
import emanondev.deepdungeons.DeepDungeons;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.entity.Player;

import java.util.List;

public class M {

    public static void BUILDER_START(Player p){
        new MessageBuilder(DeepDungeons.get(), p)
                .addTextTranslation("command.DungeonRoom.start.message",
                        List.of("&9[&bDungeonRoom&9] &bStai creando una stanza, Segui le istruzioni passo passo"
                                , "&eRicordati di passare il mouse sopra i mesaggi per eventuali dettagli"))
                .addText("\n")
                .addFullComponentTranslation("command.DungeonRoom.area_selection",
                        "&9[&bDungeonRoom&9] &bSeleziona l'area della stanza con WorldEdit",
                        List.of("&6Clicca per eseguire &e/dungeonroom next",
                                "&9Seleziona l'area della stanza con worldedit",
                                "",
                                "&9Includi le pareti e ricorda che le porte",
                                "&9 per procedere nelle stanze successive",
                                "&9 si troveranno solo sui bordi della stanza",
                                "&9 e non al suo interno"),
                        "/dungeonroom next", ClickEvent.Action.RUN_COMMAND)
                .send();
    }
}
