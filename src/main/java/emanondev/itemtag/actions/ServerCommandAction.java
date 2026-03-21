package emanondev.itemtag.actions;

import emanondev.itemedit.UtilsString;
import emanondev.itemedit.utility.CompleteUtility;
import emanondev.itemtag.ItemTag;
import emanondev.itemtag.command.itemtag.SecurityUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerCommandAction extends Action {

    public ServerCommandAction() {
        super("servercommand");
    }

    @Override
    public void validateInfo(String text) {
        if (text.isEmpty()) {
            throw new IllegalStateException();
        }
        boolean devMode = ItemTag.get().getConfig("templates.yml").getBoolean("dev_mode", false);
        if (!devMode && !text.startsWith("template:")) {
            throw new IllegalArgumentException("You cannot add free commands in Production mode. Use template:<name>");
        }
        if (!devMode && text.startsWith("template:")) {
            String templateName = text.substring("template:".length());
            if (ItemTag.get().getConfig("templates.yml").getString("command_templates." + templateName) == null) {
                throw new IllegalArgumentException("Template '" + templateName + "' does not exist in templates.yml");
            }
        }
    }

    @Override
    public boolean canExecute(Player player, String text) {
        if (text.startsWith("template:") || text.contains(" template:")) {
            return true;
        }

        if (!text.startsWith("-pin")) {
            // old unsafe item
            if (!ItemTag.get().getConfig().getBoolean("actions.unsafe_mode", false)) {
                ItemTag.get().log("&cWARNING");
                ItemTag.get()
                        .log("Hello! You see this message because &e" + player.getName() + "&f is using an item with");
                ItemTag.get().log("a &eservercommand&f action and this item was created a few versions ago, this item");
                ItemTag.get().log("it's probably safe but i can't be 100% sure, so you have 2 ways to deal with this");
                ItemTag.get().log("");
                ItemTag.get().log("A: If you are 100% certain that only trusted players can use creative mode you");
                ItemTag.get().log(
                        "   can turn unsafe mode on by going on &econfig.yml &fand set &eactions: unsafe_mode: &ctrue");
                ItemTag.get().log("B: You can manually update old items with /itemtagupdateolditem while");
                ItemTag.get().log("   having those items in hand, or you can just delete them and refund them");
                ItemTag.get().log("");
                ItemTag.get().log("&aAll items inside /serveritem (/si) have already been updated");
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&cPor favor, abra um ticket para que possamos converter este item e ele voltar a funcionar."));
                return false;
            }
        } else {
            int index = text.split(" ")[0].length() + 1;
            String code = text.substring("-pin".length(), index - 1);
            String innerText = text.substring(index);

            // Allow legacy execution if it's already an approved template! (even if pin is
            // invalid)
            boolean devMode = ItemTag.get().getConfig("templates.yml").getBoolean("dev_mode", false);
            boolean templateExists = false;
            if (!devMode) {
                org.bukkit.configuration.ConfigurationSection section = ItemTag.get().getConfig("templates.yml")
                        .getConfigurationSection("command_templates");
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        if (innerText.equals(section.getString(key))) {
                            templateExists = true;
                            break;
                        }
                    }
                }
            }
            if (templateExists) {
                return true;
            }

            if (!SecurityUtil.verifyControlKey(innerText, code)) {
                ItemTag.get().log("&cWARNING");
                ItemTag.get().log("&e" + player.getName() + "&f is using an item that contains a &eservercommand");
                ItemTag.get().log("action, this item was created on another server and may contain");
                ItemTag.get().log("malicious actions, therefor this action was blocked");
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&cPor favor, abra um ticket para que possamos converter este item e ele voltar a funcionar."));
                return false;
            }

            if (!devMode) {
                ItemTag.get().log("&cWARNING: Blocked raw servercommand execution in Production mode from "
                        + player.getName());
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&cPor favor, abra um ticket para que possamos converter este item e ele voltar a funcionar."));
                return false;
            }
        }
        return true;
    }

    @Override
    public void execute(Player player, String text) {
        if (!canExecute(player, text)) {
            return;
        }

        if (!text.startsWith("-pin")) {
            // old unsafe -- already validated by canExecute, unsafe_mode must be true
        } else {
            int index = text.split(" ")[0].length() + 1;
            text = text.substring(index);
        }

        boolean devMode = ItemTag.get().getConfig("templates.yml").getBoolean("dev_mode", false);
        if (text.startsWith("template:")) {
            String templateName = text.substring("template:".length());
            String templateCommand = ItemTag.get().getConfig("templates.yml")
                    .getString("command_templates." + templateName);
            if (templateCommand == null) {
                ItemTag.get().log(
                        "&cWARNING: User " + player.getName() + " executed non-existent template: " + templateName);
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "&cThis item relies on a template that no longer exists."));
                return;
            }
            text = templateCommand;
        } else {
            if (!devMode) {
                String foundTemplateName = null;
                org.bukkit.configuration.ConfigurationSection section = ItemTag.get().getConfig("templates.yml")
                        .getConfigurationSection("command_templates");
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        if (text.equals(section.getString(key))) {
                            foundTemplateName = key;
                            break;
                        }
                    }
                }

                if (foundTemplateName != null) {
                    text = ItemTag.get().getConfig("templates.yml").getString("command_templates." + foundTemplateName);
                } else {
                    // Should not reach here since canExecute already blocks this
                    return;
                }
            }
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                UtilsString.fix(text, player, true, "%player%", player.getName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> params) {
        if (params.get(params.size() - 1).startsWith("%")) {
            return CompleteUtility.complete(params.get(params.size() - 1), Collections.singletonList("%player%"));
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getInfo() {
        ArrayList<String> list = new ArrayList<>();
        list.add("&b" + getID() + " &e<command>");
        list.add("&e<command> &bcommand executed by server");
        list.add("&b%player% may be used as placeholder for player name");
        list.add("&bN.B. no &e/&b is required, example: '&eheal %player%&b'");
        return list;
    }

    @Override
    public String fixActionInfo(String actionInfo) {
        return "-pin" + SecurityUtil.generateControlKey(actionInfo) + " " + actionInfo;
    }

}