package emanondev.itemtag.command.itemtag;

import emanondev.itemedit.Util;
import emanondev.itemedit.command.AbstractCommand;
import emanondev.itemedit.command.SubCmd;
import emanondev.itemtag.ItemTag;
import emanondev.itemtag.TagItem;
import emanondev.itemtag.actions.ActionsUtility;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractTemplateSubCommand extends SubCmd {

    public ExtractTemplateSubCommand(AbstractCommand cmd) {
        super("extracttemplate", cmd, true, true);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        Player p = (Player) sender;
        if (args.length != 2) {
            Util.sendMessage(p, "&cUsage: /it extracttemplate <base_id>");
            return;
        }

        String baseId = args[1];
        ItemStack item = this.getItemInHand(p);
        TagItem tagItem = ItemTag.getTagItem(item);

        if (!ActionsUtility.hasActions(tagItem)) {
            Util.sendMessage(p, "&cThis item has no actions.");
            return;
        }

        List<String> actions = ActionsUtility.getActions(tagItem);
        int count = 1;
        boolean found = false;

        ItemTag.get().log("&a--- Template Extraction (" + baseId + ") ---");
        for (String action : actions) {
            String[] split = action.split(ActionsUtility.TYPE_SEPARATOR);
            if (split.length < 2)
                continue;

            String type = split[0];
            String rawAction = split[1];

            if (type.equals("servercommand") || type.equals("commandasop")) {
                found = true;
                // Remove the -pin block if present
                if (rawAction.startsWith("-pin")) {
                    int index = rawAction.split(" ")[0].length() + 1;
                    if (rawAction.length() > index) {
                        rawAction = rawAction.substring(index);
                    }
                }

                String templateKey = baseId + (count > 1 ? "_" + count : "");
                ItemTag.get().log("&f  " + templateKey + ": \"" + rawAction.replace("\"", "\\\"") + "\"");
                Util.sendMessage(p, "&aExtracted action to console: " + templateKey);
                count++;
            }
        }
        ItemTag.get().log("&a----------------------------------");

        if (!found) {
            Util.sendMessage(p, "&cNo servercommand or commandasop actions found to extract.");
        } else {
            Util.sendMessage(p, "&aCheck your console/logs for the YAML format to copy into Production config!");
        }
    }

    @Override
    public List<String> onComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Collections.singletonList("<base_id>");
        }
        return Collections.emptyList();
    }
}
