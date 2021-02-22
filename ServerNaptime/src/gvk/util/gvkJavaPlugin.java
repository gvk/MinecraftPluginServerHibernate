/**
 * Under GPL3 License, see https://github.com/gvk/MinecraftSpigotPlugins
 */

package gvk.util;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base class plugin for plugins.
 */
public abstract class gvkJavaPlugin extends JavaPlugin {
	
	/**
     * Sends message to the console prefixed with the name of the plugin.
     */
    public void consoleInfoMessage(String message) {
    	this.getLogger().info(message);
    }
    /**
     * Sends formated message to the console prefixed with the name of the plugin.
     */
    public void consoleInfoMessage(String message, Object... args) {
    	consoleInfoMessage(String.format(message, args));
    }
	
}
