# Minecraft Plugin Server Hibernate / Sleep
I mostly use PaperMC, so there might be traces of that in the code.



## Server Naptime / Server Hibernate
A plugin for small servers (eg. with friends) that lets the server sleep / hibernate when no players are connected. It saves on computer resources.  

 * CPU load is greatly reduced, from ~20% it will be about ~1% (x20 reduction) when there are zero players online. RAM is also reduced somewhat.  
 * There is a default config, but almost every single feature can be changed in the config. Use the config to configure the plugins sleep/nap time, startup time, delays, etc... feel free to experiment on your server.
 * Feel free to check out the code as well for explanations/comments.


For small servers I use `java -Xms256m -Xmx3G -jar <jar file> nogui` to reduce inital RAM usage.  
However, for performace reasons you might want to use the RAM as much as possible, then use google and look up "aikars flags java".


Also check out: https://github.com/gekigek99/minecraft-server-hibernation
