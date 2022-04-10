/**
 * Under GPL3 License, see https://github.com/gvk/MinecraftSpigotPlugins
 */

package com.gvk.servernaptime.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utilities for dealing with {@link FileConfiguration}.
 * <p>
 * Example usage: 
 * Start with {@code ConfigUtil.startUsingConfig} to get a {@link FileConfiguration} instance.
 * Then use addDefault() in the instance.
 * Lastly use ConfigUtil.saveConfig.
 * After that the config is ready to be read using the config instance get methods.
 * </p>
 */
public class ConfigUtil {

    /**
     * {@code plugin.saveDefaultConfig();}
     * @see JavaPlugin#saveDefaultConfig
     */
    public static void defaultCreateConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
    }

    /**
     * {@code plugin.reloadConfig();}
     * @see JavaPlugin#reloadConfig
     */
    public static void reloadConfig(JavaPlugin plugin) {
        plugin.reloadConfig();
    }

    /**
     * If the "config" file hasn't been created, it creates one. <br>
     * It reads the settings in the config.
     * @param plugin to configure.
     * @return the config object
     */
    public static FileConfiguration startUsingConfig(JavaPlugin plugin) {
        defaultCreateConfig(plugin);
        reloadConfig(plugin);
        return plugin.getConfig();
    }

    /**
     * Save the config file. <br>
     * Use after all config.addDefault()'s
     * @param plugin to save the config for
     */
    public static void saveConfig(JavaPlugin plugin) {
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

}