/**
 * Under GPL3 License, see https://github.com/gvk/MinecraftSpigotPlugins
 */

package gvk;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import gvk.util.ConfigUtil;
import gvk.util.gvkJavaPlugin;

/**
 * Plugin class for hibernating or sleep the server when no player are connected. While new players can still join and "wake up" the server. 
 * Saves resources for small servers (with usually friends).
 */
public final class ServerNaptimeMain extends gvkJavaPlugin {
	
	// Plugin code constants
	private static final String PLUGIN_NAME = "ServerNaptime";
	private static final String PLUGIN_NAME_SHORT = "Naptime";
	private static final String CONFIG_sleepTime = "naptime.sleepTime";
	private static final String CONFIG_startSleepDelay = "naptime.startSleepDelay";
	private static final String CONFIG_ticksAwakeBetweenSleep = "naptime.ticksAwakeBetweenSleep";
	private static final String CONFIG_sleepOtherThreads = "naptime.alsoSleepSomeInternalProcesses";
	private static final String CONFIG_sleepAllThreads = "naptime.SleepAllInternalProcesses";
	private static final String CONFIG_threadsToSleep = "naptime.internalProcessesToSleep";
	private static final String CONFIG_unloadChunks = "naptime.unloadChunks";
	private static final String CONFIG_callGC = "naptime.callGarbageCollect";
	private static final String CONFIG_saveOnUnload = "naptime.saveOnUnload";
	
	// Plugin settings
	private boolean isSleepProcessActive = true;
	private final int sleepTime;
	private final int taskDelay;
	private final int taskWaitPeriod;
	private final boolean sleepOtherThreads;
	private final boolean sleepAllThreads;
	private final List<String> threadsToSleep;
	private final boolean unloadChunks;
	private final boolean callGC;
	private final boolean saveOnUnload;
    
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
    	
    	ConfigUtil.saveConfig(this);
    	
    	// read config, with explanations
    	sleepTime = config.getInt(CONFIG_sleepTime); // Time in ms for each nap.
    	taskDelay = config.getInt(CONFIG_startSleepDelay); // Delay before start in ticks.
    	taskWaitPeriod = config.getInt(CONFIG_ticksAwakeBetweenSleep); // How many ticks between each nap/sleep.
    	sleepOtherThreads = config.getBoolean(CONFIG_sleepOtherThreads); // Whether to sleep other threads as well.
    	sleepAllThreads = config.getBoolean(CONFIG_sleepAllThreads); // Whether to sleep all threads.
    	threadsToSleep = config.getStringList(CONFIG_threadsToSleep); // Start names of other threads to sleep.
    	unloadChunks = config.getBoolean(CONFIG_unloadChunks); // Whether to unload chunks before the first nap.
    	callGC = config.getBoolean(CONFIG_callGC); // Whether to do call java's GC at plugin action start.
    	saveOnUnload = config.getBoolean(CONFIG_saveOnUnload); // Whether to save when the last player leaves the game.
    	
    	
    	consoleInfoMessage("While no players online: Server will sleep every %d ticks, and sleep will be %d milliseconds long.", taskWaitPeriod, sleepTime);
    }
    
    @Override
    public final void onDisable() {
    	isSleepProcessActive = false;
        try {
            Bukkit.getScheduler().cancelTasks(this);
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
     * Set up the sleep and unload world tasks to be executed when there are no players connected.
     */
    private final void startSleepTask() {
    	// set up the two different tasks: [unload + sleep] and just [sleep]
    	final Runnable constinuousSleepTask = this::trySleepThreads;
    	final Runnable firstSleepTask = () -> {
    		unloadAllChunks();
    		trySleepThreads();
    		currentTask = constinuousSleepTask;
    		consoleInfoMessage("No, players connected. Sleep cycle started...");
    	};
    	
    	// set the first current task
    	currentTask = firstSleepTask;
    	
    	// run the tasks
    	final Runnable scheduledTask = () -> {
			if (shouldServerSleep())
    			currentTask.run(); // run the task
            else
            	currentTask = firstSleepTask; // reset
    	};
    	
    	// repeatedly run the task with delay in between
    	Bukkit.getScheduler().scheduleSyncRepeatingTask(this, scheduledTask, taskDelay, taskWaitPeriod);
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
    	if(sleepAllThreads)
			return getAllAliveThreads().filter(t -> !t.equals(Thread.currentThread())).collect(Collectors.toList());
		else
			return getAllAliveThreads().filter(t -> threadsToSleep.stream().anyMatch(s -> t.getName().startsWith(s))).collect(Collectors.toList());
    }
    
    /**
     * Gets all alive threads as a Stream.
     * @return a Stream of all current threads filtered by alive.
     */
    private final Stream<Thread> getAllAliveThreads() {
    	return Thread.getAllStackTraces().keySet().stream().filter(Thread::isAlive);
    }
    
    /**
     * Unloads all chunks in all worlds, also by settings optionally saves the chunks.
     */
    private final void unloadAllChunks() {
    	if(unloadChunks) {
    		boolean save = saveOnUnload;
    		for (final World w : Bukkit.getWorlds()) {
                for (final Chunk c : w.getLoadedChunks()) {
                    c.unload(save);
                }
                // Bukkit.unloadWorld(w, true); // nah, too little to gain, while also having to reload when player joins.
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
    	return Bukkit.getServer().getOnlinePlayers().size() == 0 && this.isSleepProcessActive;
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
