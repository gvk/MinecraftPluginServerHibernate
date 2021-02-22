# MinecraftSpigotPlugins
I rather have one repository with many projects, than many different repositories each with just one single file...  although that is not very git-ish.
<br><br>
I mostly use PaperMC, so there might be traces of that in the code.



## Server Naptime
A plugin for small servers (eg. with friends) that lets the server sleep / hibernate when no players are connected. It saves on computer resources.  

 * CPU load is greatly reduced, from ~20% it will be about ~1% when there are zero players. RAM is also reduced somewhat.  
 * There is a default config, but almost every single feature can be changed in the config. Use the config to configure the plugins sleep/nap time, startup time, delays, etc.
 * Feel free to check out the code as well for explinations/comments.


For small servers I use `java -Xms256m -Xmx3G -jar <jar file> nogui` to reduce RAM usage.  
However, for performace reasons you might want to use the RAM as much as possible, then use google and look up "aikars flags java".
