package emanondev.itemtag.actions;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public abstract class Action {

    private final String id;

    public Action(String id) {
        this.id = id.toLowerCase(Locale.ENGLISH);
    }

    public String getID() {
        return id;
    }

    /**
     * @throws IllegalArgumentException if text is not valid
     */
    public abstract void validateInfo(String text);

    /**
     * @throws IllegalArgumentException if text is not valid
     */
    public abstract void execute(Player player, String text);

    /**
     * @return allowed completitions
     */
    public abstract List<String> tabComplete(CommandSender sender, List<String> params);

    public abstract List<String> getInfo();// TODO move on configuration

    public String fixActionInfo(String actionInfo) {
        return actionInfo;
    }

    /**
     * Pre-check whether this action can be executed.
     * 
     * @return true if execution is allowed, false if it should be blocked.
     */
    public boolean canExecute(Player player, String text) {
        return true;
    }
}
