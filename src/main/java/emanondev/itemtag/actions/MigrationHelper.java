package emanondev.itemtag.actions;

import emanondev.itemtag.ItemTag;
import emanondev.itemtag.TagItem;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MigrationHelper {

    public static class MigrationResult {
        private final boolean updated;
        private final ItemStack itemStack;
        private final List<String> report;

        public MigrationResult(boolean updated, ItemStack itemStack, List<String> report) {
            this.updated = updated;
            this.itemStack = itemStack;
            this.report = report;
        }

        public boolean isUpdated() {
            return updated;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public List<String> getReport() {
            return report;
        }
    }

    /**
     * Inspects an item, migrates legacy servercommand/commandasop actions to
     * templates,
     * deduplicates them with existing templates, and returns the result.
     * 
     * @param item             The item to migrate.
     * @param baseTemplateName An optional base name used to prefix new generated
     *                         templates.
     * @return MigrationResult containing migration details.
     */
    public static MigrationResult migrateItem(ItemStack item, String baseTemplateName) {
        if (item == null || item.getType().name().contains("AIR")) {
            return new MigrationResult(false, item, new ArrayList<>());
        }

        TagItem tagItem = ItemTag.getTagItem(item);
        if (!ActionsUtility.hasActions(tagItem)) {
            return new MigrationResult(false, item, new ArrayList<>());
        }

        if (baseTemplateName == null || baseTemplateName.isEmpty()) {
            baseTemplateName = "migrated_" + UUID.randomUUID().toString().substring(0, 8);
        }

        List<String> actions = ActionsUtility.getActions(tagItem);
        boolean updated = false;
        int[] count = { 1 };
        List<String> report = new ArrayList<>();

        for (int i = 0; i < actions.size(); i++) {
            String action = actions.get(i);
            String[] split = action.split(ActionsUtility.TYPE_SEPARATOR, 2);
            if (split.length < 2)
                continue;

            String type = split[0];
            String rawAction = split[1];

            if (type.equals("servercommand") || type.equals("commandasop")) {
                // Direct commandasop/servercommand action
                String result = migrateCommandAction(type, rawAction, baseTemplateName, count, report, i);
                if (result != null) {
                    actions.set(i, type + ActionsUtility.TYPE_SEPARATOR + result);
                    updated = true;
                }
            } else if (type.equals("delay")) {
                // Delay-wrapped action: format is "<ticks> <subType> <subActionInfo>"
                String[] delayParts = rawAction.split(" ", 3);
                if (delayParts.length < 3)
                    continue;

                String ticks = delayParts[0];
                String subType = delayParts[1];
                String subActionInfo = delayParts[2];

                if (subType.equals("servercommand") || subType.equals("commandasop")) {
                    String result = migrateCommandAction(subType, subActionInfo, baseTemplateName, count, report, i);
                    if (result != null) {
                        // Rebuild: delay%%:%%<ticks> <subType> <newSubActionInfo>
                        String newDelayAction = ticks + " " + subType + " " + result;
                        actions.set(i, type + ActionsUtility.TYPE_SEPARATOR + newDelayAction);
                        updated = true;
                    }
                }
            }
        }

        if (updated) {
            ActionsUtility.setActions(tagItem, actions);
        }

        return new MigrationResult(updated, tagItem.getItem(), report);
    }

    /**
     * Attempts to migrate a single commandasop/servercommand action to a template.
     *
     * @param type             The action type ("commandasop" or "servercommand").
     * @param rawAction        The raw action info (may include -pin prefix).
     * @param baseTemplateName Base name for new templates.
     * @param count            Mutable counter for template naming (array of 1
     *                         element).
     * @param report           Report list to append migration info to.
     * @param actionIndex      The index of the action in the actions list (for
     *                         report).
     * @return The new action info string (with pin via fixActionInfo), or null if
     *         no migration needed.
     */
    private static String migrateCommandAction(String type, String rawAction, String baseTemplateName,
            int[] count, List<String> report, int actionIndex) {
        // If it's already a template, skip
        if (rawAction.startsWith("template:") || rawAction.contains(" template:")) {
            return null;
        }

        // Strip pin if present
        if (rawAction.startsWith("-pin")) {
            int index = rawAction.split(" ")[0].length() + 1;
            if (rawAction.length() > index) {
                rawAction = rawAction.substring(index);
            }
        }

        // Deduplication: check if a template with this exact command already exists
        String existingTemplateName = null;
        org.bukkit.configuration.ConfigurationSection section = ItemTag.get().getConfig("templates.yml")
                .getConfigurationSection("command_templates");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (rawAction.equals(section.getString(key))) {
                    existingTemplateName = key;
                    break;
                }
            }
        }

        String templateNameToUse;
        if (existingTemplateName != null) {
            templateNameToUse = existingTemplateName;
            report.add("§7[" + (actionIndex + 1) + "] §e" + type + " §7-> §areused template: §f" + templateNameToUse
                    + " §7(cmd: " + rawAction + ")");
        } else {
            templateNameToUse = baseTemplateName + (count[0] > 1 ? "_" + count[0] : "");

            // Save into Config
            ItemTag.get().getConfig("templates.yml").set("command_templates." + templateNameToUse, rawAction);
            ItemTag.get().getConfig("templates.yml").save();
            count[0]++;

            report.add("§7[" + (actionIndex + 1) + "] §e" + type + " §7-> §anew template: §f" + templateNameToUse
                    + " §7(cmd: " + rawAction + ")");
        }

        // Build new action info with fixActionInfo (adds pin)
        String newActionInfo = "template:" + templateNameToUse;
        newActionInfo = ActionHandler.fixActionInfo(type, newActionInfo);
        return newActionInfo;
    }
}
