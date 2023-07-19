/**
 * Under GPL3 License, see https://github.com/gvk/MinecraftSpigotPlugins
 */

package gvk;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;


import gvk.util.ConfigUtil;
import gvk.util.gvkJavaPlugin;

/* About this plugin:
 * Causes the server to tick very slow (or "sleep") when there are no players online. While new players can still join and "wake up" the server.
 * Saves resources for small servers (usually used by friends). Also good when you don't want the spawn chunks time to continue when nobody is playing.
 * (made for smaller vanilla-like servers - at the time 1.16)
 * 
 * TODO: Enable freeze using day-light-cycle gamerule.
 * Also check if all configs works
 */

/**
 * Plugin class for hibernating or sleep the server when no player are connected. Joining causes "wake up".
 * Saves resources for small servers.
 */
public final class ServerNaptimeMain extends gvkJavaPlugin {
	
	// Plugin code constants
	private static final String PLUGIN_NAME = "ServerNaptime";
	private static final String PLUGIN_NAME_SHORT = "Naptime";
	private static final String CONFIG_sleepTime = "naptime.sleepTime";
	private static final String CONFIG_startSleepDelay = "naptime.startSleepDelay";
	private static final String CONFIG_ticksAwakeBetweenSleep = "naptime.ticksAwakeBetweenSleep";
	private static final String CONFIG_sleepOtherThreads = "naptime.alsoSleepSomeInternalProcesses";
	private static final String CONFIG_sleepAllThreads = "naptime.sleepAllInternalProcesses";
	private static final String CONFIG_threadsToSleep = "naptime.internalProcessesToSleep";
	private static final String CONFIG_unloadChunks = "naptime.unloadChunks";
	private static final String CONFIG_callGC = "naptime.callGarbageCollect";
	private static final String CONFIG_saveOnUnload = "naptime.saveOnUnload";
	private static final String CONFIG_commandsToExecuteOnSleep = "naptime.commandsToExecuteOnSleep";
	private static final String CONFIG_commandsToExecuteOnWake = "naptime.commandsToExecuteOnWake";
	
	// Plugin settings
	private boolean isSleepProcessActive = true;
	private final int sleepTime;
	private final int firstTaskDelay;
	private final int taskWaitPeriod;
	private final boolean sleepOtherThreads;
	private final boolean sleepAllThreads;
	private final List<String> threadsToSleep;
	private final boolean unloadChunks;
	private final boolean callGC;
	private final boolean saveOnUnload;
	private final List<String> commandsOnSleep;
	private final List<String> commandsOnWake;
    
	/**
	 * Constructs the plugin instance, and initializes the config.
	 */
    public ServerNaptimeMain() {
    	FileConfiguration config = ConfigUtil.startUsingConfig(this);
    	
    	// default config, explanation further down.
    	config.addDefault(CONFIG_sleepTime, 1000L);
    	config.addDefault(CONFIG_startSleepDelay, 20L*30L); // never more than 2 minutes. usually below 1:20 on weak computers
    	config.addDefault(CONFIG_ticksAwakeBetweenSleep, 1L);
    	config.addDefault(CONFIG_sleepOtherThreads, true);
    	config.addDefault(CONFIG_sleepAllThreads, false);
    	config.addDefault(CONFIG_threadsToSleep, new String[] {"Server-Worker", "Paper Async Chunk" , "Craft Async Scheduler Management Thread", "Snooper Timer", "Log4j"});
    	config.addDefault(CONFIG_unloadChunks, true);
    	config.addDefault(CONFIG_callGC, true);
    	config.addDefault(CONFIG_saveOnUnload, true);
    	
    	config.addDefault(CONFIG_commandsToExecuteOnSleep, new String[0]);
    	config.addDefault(CONFIG_commandsToExecuteOnWake, new String[0]);
    	
    	ConfigUtil.saveConfig(this);
    	
    	// read config, with explanations
    	sleepTime = config.getInt(CONFIG_sleepTime); // Time in ms for each nap.
    	firstTaskDelay = config.getInt(CONFIG_startSleepDelay); // Delay before sleeps start in ticks.
    	taskWaitPeriod = config.getInt(CONFIG_ticksAwakeBetweenSleep); // How many ticks between each nap/sleep.
    	sleepOtherThreads = config.getBoolean(CONFIG_sleepOtherThreads); // Whether to sleep other threads as well.
    	sleepAllThreads = config.getBoolean(CONFIG_sleepAllThreads); // Whether to sleep all threads.
    	threadsToSleep = config.getStringList(CONFIG_threadsToSleep); // Start names of other threads to sleep.
    	unloadChunks = config.getBoolean(CONFIG_unloadChunks); // Whether to unload chunks before the first nap.
    	callGC = config.getBoolean(CONFIG_callGC); // Whether to do call java's GC at plugin action start.
    	saveOnUnload = config.getBoolean(CONFIG_saveOnUnload); // Whether to save when the last player leaves the game.
    	commandsOnSleep = config.getStringList(CONFIG_commandsToExecuteOnSleep); // Commands when the last player leaves / server starts sleep
    	commandsOnWake = config.getStringList(CONFIG_commandsToExecuteOnWake); // Commands when a player is first to join / server wakes up
    	
    	consoleInfoMessage("While no players online: Server will sleep every %d ticks, and sleep will be %d milliseconds long.", taskWaitPeriod, sleepTime);
    }
    
    @Override
    public final void onDisable() {
    	isSleepProcessActive = false;
        try {
        	getServer().getScheduler().cancelTasks(this);
        }
        catch (final Throwable t) {
        	consoleInfoMessage("There was an error while disabling the plugin.");
        }
    }
    
    @Override
    public final void onEnable() {
    	isSleepProcessActive = true;
    	startSleepTask();
    }
    
    /**
     * Set up the sleep and unload world tasks to be executed when there are no players connected. Executes when plugin is enabled.
     */
    private final void startSleepTask() {
    	// set up the three different tasks: [unload] and [sleep], and [wake]
    	final Runnable constinuousSleepTask = this::trySleepThreads;
    	final Runnable firstSleepTask = () -> {
    		runOnSleepCommands();
    		unloadAllChunks();
    		//trySleepThreads(); // let it tick for the command and unload to happen.
    		currentTask = constinuousSleepTask;
    		consoleInfoMessage("No, players connected. Sleep cycle started...");
    	};
    	final Runnable firstWakeTask = () -> {
    		if(currentTask != firstSleepTask) runOnWakeCommands();
    		currentTask = firstSleepTask; // reset
    	};
    	
    	// set the first current task
    	currentTask = firstSleepTask;
    	
    	// run the tasks
    	final Runnable scheduledTask = () -> {
			if (shouldServerSleep())
    			currentTask.run(); // run the task
            else
            	firstWakeTask.run(); // resets and calls commands.
    	};
    	
    	// repeatedly run the task with delay in between
    	getServer().getScheduler().scheduleSyncRepeatingTask(this, scheduledTask, firstTaskDelay, taskWaitPeriod);
	}
	private Runnable currentTask;
    
    /**
     * Sleeps the server thread(s). 
     */
    private final void trySleepThreads() {
    	try {
    		// get list of threads to suspend and suspend them
    		List<Thread> threads = null;
    		if(sleepOtherThreads) {
    			threads = filterThreadsBySettings();
    			threads.forEach(Thread::suspend);
    		}
    		
    		// sleep current thread. "Server Thread"
            Thread.sleep(sleepTime); 

    		// resume the list of suspended threads
            if(sleepOtherThreads) {
            	threads.forEach(Thread::resume);
            }
        }
        catch (Exception ex) {}
    }
    
    /**
     * Get the threads to sleep based on settings.
     * @return a List of all threads to sleep.
     */
    private final List<Thread> filterThreadsBySettings() {
    	if(sleepAllThreads) // different predicate/filter on threads depending on settings
			return getAllAliveThreadsFiltered(t -> !t.equals(Thread.currentThread()));
		else
			return getAllAliveThreadsFiltered(t -> threadsToSleep.stream().anyMatch(s -> t.getName().startsWith(s)));
    }
    
    /**
     * Gets all alive threads as a List based on filters/predicate.
     * @return a List of all current alive threads filtered by provided predicate.
     */
    private final List<Thread> getAllAliveThreadsFiltered(Predicate<Thread> filter) {
    	return getAllAliveThreads().filter(filter).collect(Collectors.toList());
    }
    
    /**
     * Gets all alive threads as a Stream.
     * @return a Stream of all current threads filtered by alive.
     */
    private final Stream<Thread> getAllAliveThreads() {
    	return Thread.getAllStackTraces().keySet().stream().filter(Thread::isAlive);
    }
    
    /**
     * Executes the commands specified in the config file, when the last player leaves.
     */
    private void runOnSleepCommands() {
    	runCommandsAsConsole(commandsOnSleep);
	}
    /**
     * Executes the commands specified in the config file, when the first player joins
     */
    private void runOnWakeCommands() {
    	runCommandsAsConsole(commandsOnWake);
	}
    /**
     * Executes the commands specified in the provided list as console/admin
     */
    private void runCommandsAsConsole(List<String> commands) {
    	commands.forEach(command -> getServer().dispatchCommand(getServer().getConsoleSender(), command));
    }
    
    /**
     * Unloads all chunks in all worlds, also by settings optionally saves the chunks.
     */
    private final void unloadAllChunks() {
    	if(unloadChunks) {
    		boolean save = saveOnUnload;
    		for (final World w : getServer().getWorlds()) {
    			for (final Chunk c : w.getLoadedChunks()) {
    				c.unload(save);
    			}
    			if(save) w.save();
    			// getServer().unloadWorld(w, true); // nah, too little to gain, while also having to reload when player joins.
    		}
    	}
    	if(callGC) { // some server owners know better than this, and are free to disable it.
    		System.gc();
    		System.runFinalization();
    	}
	}

    
    @Override
	public final boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
    	// when is this called?? I think it scans the plugin.yml,  but check that.
		final String commandName = PLUGIN_NAME_SHORT;
		final String permissionName = PLUGIN_NAME_SHORT+".toggle";
		if (command.getName().equalsIgnoreCase(commandName) && (sender.isOp() || sender.hasPermission(permissionName))) {
			sender.sendMessage(String.format("[%s] %s is now %s", PLUGIN_NAME, PLUGIN_NAME_SHORT, this.toggleSleepy() ? "enabled" : "disabled"));
			return true;
		}
		return false;
    }
	
    /**
     * Toggles whether the plugin should sleep the server when there are no players connected.
     * @return the state of isSleepProcessActive after toggle.
     */
    private final boolean toggleSleepy() {
        return isSleepProcessActive = !isSleepProcessActive;
    }
    
    /**
     * Determines whether the plugin should sleep the server. That is if the plugin is active and there are no players.
     * @return true if the server should sleep (no players, and active plugin), false otherwise.
     */
    private final boolean shouldServerSleep() {
    	return getServer().getOnlinePlayers().size() == 0 && this.isSleepProcessActive;
    }
   
}

/* === THREAD NAMES === 

AWT-EventQueue-0, false
AWT-Shutdown, false
AWT-Windows, true
Attach Listener, true
Craft Async Scheduler Management Thread, false
DestroyJavaVM, false
Finalizer, true
Java2D Disposer, true
Log4j2-TF-1-AsyncLogger[AsyncContext@5c647e05]-1, true
Netty Server IO #0, true
ObjectCleanerThread, true
Paper Async Chunk Task Thread #0, false
Paper Async Chunk Task Thread #1, false
Paper Async Chunk Task Thread #2, false
Paper Async Chunk Task Thread #3, false
Paper Async Chunk Task Thread #4, false
Paper Async Chunk Task Thread #5, false
Paper Async Chunk Task Thread #6, false
Paper Async Chunk Urgent Task Thread, false
Paper Watchdog Thread, false
Reference Handler, true
Server console handler, true
Server thread, false
Server-Worker-1, true
Server-Worker-10, true
Server-Worker-11, true
Server-Worker-12, true
Server-Worker-2, true
Server-Worker-3, true
Server-Worker-4, true
Server-Worker-5, true
Server-Worker-6, true
Server-Worker-7, true
Server-Worker-8, true
Server-Worker-9, true
Signal Dispatcher, true
Snooper Timer, true
Thread-4, true
Timer hack thread, true
Timer-0, true
TimerQueue, true
WindowsStreamPump, true 
*/
