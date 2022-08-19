# Server Naptime
Server Naptime is a Minecraft server plugin built on top of the Spigot API framework. Its' purpose is save a Minecraft server's resources by sleeping the server when there are no active players online. As soon as a player joins, the server is woken up and the player can start playing.

Server Naptime generally reduces CPU load "from ~20%" to "about ~1% (x20 reduction) when there are zero players online" while "RAM is also reduced somewhat" (_[gvk](https://github.com/gvk) the original developer_). On a technical level, this is done by sleeping server threads as specified by the administrator in this plugin's ```config.yml```.

Server Naptime was originally created by GitHub user __[gvk](https://github.com/gvk)__ and hosted publicly on the [Minecraft Plugin Server Hibernate](https://github.com/gvk/MinecraftPluginServerHibernate) repository under the [GNU General Public License v3.0](https://github.com/gvk/MinecraftPluginServerHibernate/blob/master/LICENSE) license. I ([RT5Phantom](https://github.com/RT5Phantom)) found the original project and used it on my server network, while making changes to enhance the plugin's functionality and efficiency. Eventually, I decided that I was going to fully inject my personal coding standards into the existing project and actively seek to maintain and develop a separate version for my personal use.

## Using Server Naptime
Server Naptime is a Spigot API Framework plugin and consequently requires a Spigot server to run.

### Installing Server Naptime
To install Server Naptime on your Minecraft Spigot server (or Spigot-based fork) follow these steps below:
1. Download or Build a version of Server Naptime using a version of the Spigot API Framework that supports your Minecraft Server's version.
2. Add the Server Naptime jar file to your server's plugin folder and start your server.
3. Server Naptime is now running on your server
      - If you want to modify Server Naptime's configuration, you can stop your server and make changes to the```config.yml``` in Server Naptime's plugin directory.

### Buiding Server Naptime
To build Server Naptime for yourself:
1. Using a Java IDE pull Server Naptime's Repository: [https://github.com/RT5Phantom/ServerNaptime](https://github.com/RT5Phantom/ServerNaptime)
2. Using Maven export the project as a .jar file
      - In the ```pom.xml``` you can change the dependencies and Java versions as required if you want to either update or backport Server Naptime.  _Backporting or Updating may cause code to break as dependency systems change on different versions._
      - Additional threads can be slept by Server Naptime if you build and run the plugin on a forked version of the Spigot server framework.
      
### License
**The Server Naptime project is licensed under the [**GNU General Public License v3.0**](/LICENSE) license, following the original project's example.**

_The following is prioritized after Server Naptime's license. These guidelines were created as part of [RT5Phantom's](https://github.com/RT5Phantom) fork of the [project](https://github.com/RT5Phantom/ServerNaptime). In any case of conflict, the original license takes precedence over the below guidelines._
- Forks of Server Naptime
  - Feel free to create public forks, modifications, and published releases of the Server Naptime plugin but please keep them open source for the community.
  - Link back to the repository of the project that you modified, as well as the [original project](https://github.com/gvk/MinecraftPluginServerHibernate).
  - Do not advertise your version as the original version of Server Naptime and make it clear that it is a fork.

## Version Control
```Iteration.Release.Patch or v2.1.0``` <br />
```Iteration.Release.Patch-Cycle-VersionOfCycle.jar or ServerNaptime-2.1.0-SNAPSHOT-1.jar``` <br />
- **Iteration** tracks the high level version of this project and any major code, system, or structure redevelopment, restructuring, or rewriting.
- **Release** tracks the major releases of this project, whether it is adding additional functionality, new classes/objects, or smaller code rewrites.
- **Patch** tracks the minor releases of this project and is generally reserved for updates with bug fixes, typos, or fixing of oversites.
- **Cycle** tracks the current development phase of the project and can have three options; *SNAPSHOT*, *BATA*, and *RELEASE*.
  - <ins>SNAPSHOT</ins> is an active development build of the project.
  - <ins>BATA</ins> is a prelease build of the project.
  - <ins>RELEASE</ins> is a final build of the project
- **Version of Cycle** tracks the build number of the cycle. For example, the second build of the project's snapshot development.
