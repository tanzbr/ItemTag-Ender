package emanondev.itemtag.command.itemtag;

import emanondev.itemedit.Util;
import emanondev.itemedit.command.AbstractCommand;
import emanondev.itemedit.command.SubCmd;
import emanondev.itemtag.ItemTag;
import emanondev.itemtag.TagItem;
import emanondev.itemtag.actions.ActionHandler;
import emanondev.itemtag.actions.ActionsUtility;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MigrateItemSubCommand extends SubCmd {

    public MigrateItemSubCommand(AbstractCommand cmd) {
        super("migrateitem", cmd, false, false);
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player) {
            Util.sendMessage(sender,
                    "&cThis command can only be executed via the server console for security reasons.");
            return;
        }

        if (args.length < 2) {
            Util.sendMessage(sender, "&cUsage: /it migrateitem <player> [template_name]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Util.sendMessage(sender, "&cPlayer not found.");
            return;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().name().contains("AIR")) {
            Util.sendMessage(sender, "&cTarget player has no item in main hand.");
            return;
        }

        TagItem tagItem = ItemTag.getTagItem(item);
        if (!ActionsUtility.hasActions(tagItem)) {
            Util.sendMessage(sender, "&cThis item has no actions to migrate.");
            return;
        }

        String baseTemplateName = args.length > 2 ? args[2]
                : "migrated_" + UUID.randomUUID().toString().substring(0, 8);
        List<String> actions = ActionsUtility.getActions(tagItem);
        boolean updated = false;
        int count = 1;

        for (int i = 0; i < actions.size(); i++) {
            String action = actions.get(i);
            String[] split = action.split(ActionsUtility.TYPE_SEPARATOR);
            if (split.length < 2)
                continue;

            String type = split[0];
            String rawAction = split[1];

            if (type.equals("servercommand") || type.equals("commandasop")) {
                // If it's already a template, skip
                if (rawAction.startsWith("template:") || rawAction.contains(" template:")) {
                    continue;
                }

                // Strip pin if present
                if (rawAction.startsWith("-pin")) {
                    int index = rawAction.split(" ")[0].length() + 1;
                    if (rawAction.length() > index) {
                        rawAction = rawAction.substring(index);
                    }
                }

                String newTemplateName = baseTemplateName + (count > 1 ? "_" + count : "");

                // Save into Config
                ItemTag.get().getConfig("templates.yml").set("command_templates." + newTemplateName, rawAction);
                ItemTag.get().getConfig("templates.yml").save();

                // Rewrite the action in the item
                String newActionInfo = "template:" + newTemplateName;
                newActionInfo = ActionHandler.fixActionInfo(type, newActionInfo);

                actions.set(i, type + ActionsUtility.TYPE_SEPARATOR + newActionInfo);
                updated = true;
                count++;

                Util.sendMessage(sender, "&aMigrated action to template: " + newTemplateName);
            }
        }

        if (updated) {
            ActionsUtility.setActions(tagItem, actions);
            // Replace item in hand
            target.getInventory().setItemInMainHand(tagItem.getItem());
            Util.sendMessage(sender, "&aItem successfully migrated and saved to player's hand!");
            Util.sendMessage(target, "&aYour item was securely migrated by an administrator.");
        } else {
            Util.sendMessage(sender, "&eNo actions needed migration on this item.");
        }
    }

    @Override
    public List<String> onComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return null; // Player names
        } else if (args.length == 3) {
            return Collections.singletonList("<template_name>");
        }
        return Collections.emptyList();
    }
}
