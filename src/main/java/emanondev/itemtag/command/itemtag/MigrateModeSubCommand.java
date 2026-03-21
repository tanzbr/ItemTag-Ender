package emanondev.itemtag.command.itemtag;

import emanondev.itemedit.command.AbstractCommand;
import emanondev.itemtag.command.ListenerSubCmd;
import emanondev.itemtag.actions.MigrationHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

public class MigrateModeSubCommand extends ListenerSubCmd {

    private static final Set<UUID> activeMigrators = new HashSet<>();

    public MigrateModeSubCommand(AbstractCommand cmd) {
        // name: migratemode, requiresPlayer: false, requiresItem: false
        super("migratemode", cmd, false, false);
    }

    public static Set<UUID> getActiveMigrators() {
        return activeMigrators;
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
            sendColoredMessage(sender, "&cUsage: /it migratemode <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendColoredMessage(sender, "&cPlayer not found or is offline.");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        if (activeMigrators.contains(targetUUID)) {
            activeMigrators.remove(targetUUID);
            sendColoredMessage(sender, "&aMigration mode &cDISABLED &afor player &7" + target.getName() + "&a.");
            sendColoredMessage(target, "&cMigration mode disabled.");
        } else {
            activeMigrators.add(targetUUID);
            sendColoredMessage(sender, "&aMigration mode &eENABLED &afor player &7" + target.getName() + "&a.");
            sendColoredMessage(target, "&eMigration mode enabled! Right-click on a chest to migrate its contents.");
        }
    }

    @Override
    public List<String> onComplete(@NotNull CommandSender sender, String[] args) {
        if (args.length == 2) {
            return null; // suggest player names
        }
        return Collections.emptyList();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!activeMigrators.contains(player.getUniqueId())) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) {
            sendColoredMessage(player, "&cYou must click on a container (chest, barrel, etc.) blocks.");
            return;
        }

        event.setCancelled(true); // Prevent opening GUI

        Container container = (Container) block.getState();
        Inventory inventory = container.getInventory();

        List<String> consolidatedReport = new ArrayList<>();
        int migratedItemsCount = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().name().contains("AIR")) {
                continue;
            }

            MigrationHelper.MigrationResult result = MigrationHelper.migrateItem(item, null);
            if (result.isUpdated()) {
                inventory.setItem(i, result.getItemStack());
                consolidatedReport.addAll(result.getReport());
                migratedItemsCount++;
            }
        }

        // Disable mode
        activeMigrators.remove(player.getUniqueId());
        CommandSender console = Bukkit.getConsoleSender();

        if (migratedItemsCount > 0) {
            sendColoredMessage(console, "&a--- Bulk Migration Report for " + player.getName() + " ---");
            for (String reportLine : consolidatedReport) {
                sendColoredMessage(console, reportLine);
            }
            sendColoredMessage(console, "&a--- End of Report ---");
            sendColoredMessage(player,
                    "&aSuccessfully migrated &e" + migratedItemsCount + " &aitems in the container.");
            sendColoredMessage(console, "&aPlayer &e" + player.getName() + " &amigrated &e"
                    + migratedItemsCount + " &aitems inside a container.");
        } else {
            sendColoredMessage(player, "&eNo items needed migration in this container.");
        }
        sendColoredMessage(player, "&cMigration mode has been disabled automatically.");
    }
}
