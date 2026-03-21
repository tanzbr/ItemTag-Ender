package emanondev.itemtag.command.itemtag;

import emanondev.itemedit.command.AbstractCommand;
import emanondev.itemedit.command.SubCmd;
import emanondev.itemtag.ItemTag;
import emanondev.itemtag.TagItem;
import emanondev.itemtag.actions.ActionsUtility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class MigrateItemSubCommand extends SubCmd {

    public MigrateItemSubCommand(AbstractCommand cmd) {
        super("migrateitem", cmd, false, false);
    }

    private void sendColoredMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player) {
            sendColoredMessage(sender,
                    "&cThis command can only be executed via the server console for security reasons.");
            return;
        }

        if (args.length < 2) {
            sendColoredMessage(sender, "&cUsage: /it migrateitem <player> [template_name]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendColoredMessage(sender, "&cPlayer not found.");
            return;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().name().contains("AIR")) {
            sendColoredMessage(sender, "&cTarget player has no item in main hand.");
            return;
        }

        TagItem tagItem = ItemTag.getTagItem(item);
        if (!ActionsUtility.hasActions(tagItem)) {
            sendColoredMessage(sender, "&cThis item has no actions to migrate.");
            return;
        }

        String baseTemplateName = args.length > 2 ? args[2] : null;

        emanondev.itemtag.actions.MigrationHelper.MigrationResult result = emanondev.itemtag.actions.MigrationHelper
                .migrateItem(item, baseTemplateName);

        if (result.isUpdated()) {
            // Replace item in hand
            target.getInventory().setItemInMainHand(result.getItemStack());
            sendColoredMessage(sender, "&a--- Migration Report ---");
            for (String line : result.getReport()) {
                sendColoredMessage(sender, line);
            }
            sendColoredMessage(sender, "&a--- End of Report ---");
            sendColoredMessage(sender, "&aItem successfully migrated and saved to player's hand!");
            sendColoredMessage(target, "&aYour item was securely migrated by an administrator.");
        } else {
            sendColoredMessage(sender, "&eNo actions needed migration on this item.");
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
