package x.naxdy.lms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LastManStanding extends JavaPlugin implements Listener
{
	private static Logger log;
	
	private List<LMSMatch> matches;
	private int instanceCount;
	
	@Override
	public void onEnable()
	{
		log = this.getLogger();
		
		log.info("Registering events...");
		getServer().getPluginManager().registerEvents(this, this);
		
		matches = new ArrayList<LMSMatch>();
		instanceCount = 0;
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run()
			{
				update();
			}
		}
		, 0L, 20L);
	}
	
	@Override
	public void onDisable()
	{
		log.info("Clearing instances...");
		for(int i = 0; i < instanceCount; i++)
		{
			log.info("Unloading & removing instance"+i);
			Bukkit.unloadWorld("instance"+i, false);
			removeDir("instance"+i);
		}
	}
	
	// called once per second
	private void update()
	{
		for(int i = 0; i < matches.size(); i++)
		{
			matches.get(i).update();
			
			if(matches.get(i).removeMatch())
				matches.remove(i);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(command.getName().equalsIgnoreCase("match") && sender instanceof Player)
		{
			for(int i = 0; i < matches.size(); i++)
			{
				if(matches.get(i).isInMatch((Player)sender))
				{
					((Player)sender).sendMessage(formatStr(ChatColor.RED, "You are already in a match!"));
					return true;
				}
			}
			
			allocatePlayer((Player)sender);
			return true;
		}
		else if(command.getName().equalsIgnoreCase("who") && sender instanceof Player)
		{
			for(int i = 0; i < matches.size(); i++)
			{
				if(matches.get(i).printPlayers((Player)sender))
				{
					return true;
				}
			}
			
			((Player)sender).sendMessage(formatStr(ChatColor.RED, "You are not in a match."));
			return true;
		}
		else if(command.getName().equalsIgnoreCase("who") && !(sender instanceof Player))
		{
			String plrs = "";
			for(Player p : Bukkit.getOnlinePlayers())
			{
				plrs += p.getDisplayName() + ", ";
			}
			plrs = plrs.substring(0, plrs.length()-2);
			sender.sendMessage("Online Players [" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers() + "]:\n" + plrs);
			return true;
		}
		else if(command.getName().equalsIgnoreCase("lms"))
		{
			sender.sendMessage(ChatColor.GOLD + "This server is running " + ChatColor.BOLD + "LastManStanding" + ChatColor.RESET + " " + ChatColor.GOLD + "v" + this.getDescription().getVersion() +
					"\n" + ChatColor.GRAY + "Contribute to this plugin at " + ChatColor.YELLOW + "https://github.com/xNaXDy/LastManStanding");
			return true;
		}
		return false;
	}
	
	@EventHandler
	private void onPlayerJoin(PlayerJoinEvent event)
	{
		event.setJoinMessage("");
		
		event.getPlayer().sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "--- Last Man Standing -- \n" + ChatColor.RESET + "" + ChatColor.GRAY + "Everyone starts on the same playing field. Gather supplies in a small world, and defeat all your opponents. Last man standing wins!");
		
		if(event.getPlayer().isOp())
			return;
		
		allocatePlayer(event.getPlayer());
	}
	
	@EventHandler
	private void onPlayerQuit(PlayerQuitEvent event)
	{
		event.setQuitMessage("");
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).onPlayerQuit(event))
			{
				return;
			}
		}
	}
	
	@EventHandler
	private void onEntityDamage(EntityDamageEvent event)
	{
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).onEntityDamage(event))
			{
				return;
			}
		}
	}
	
	@EventHandler
	private void onPlayerMove(PlayerMoveEvent event)
	{
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).onPlayerMove(event))
			{
				return;
			}
		}
	}
	
	@EventHandler
	private void onPlayerChat(AsyncPlayerChatEvent event)
	{
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).onPlayerChat(event))
			{
				return;
			}
		}
		
		event.setCancelled(true);
	}
	
	@EventHandler
	private void onBlockBreak(BlockBreakEvent event)
	{
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).onBlockBreak(event))
			{
				return;
			}
		}
	}
	
	@EventHandler
	private void onPlayerInteract(PlayerInteractEvent event)
	{
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).onPlayerInteract(event))
			{
				return;
			}
		}
	}
	
	@EventHandler
	private void onPlayerDeath(PlayerDeathEvent event)
	{
		event.getEntity().setHealth(20);
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).onPlayerDeath(event))
			{
				return;
			}
		}		
	}
	
	public void allocatePlayer(Player p)
	{
		for(int i = 0; i < matches.size(); i++)
		{
			if(matches.get(i).allocatePlayer(p))
			{
				return;
			}
		}
		
		// TODO: pull max players from config
		matches.add(new LMSMatch(this, 50, 2));
		matches.get(matches.size()-1).allocatePlayer(p);
	}
	
	public int getInstanceCountInc()
	{
		return instanceCount++;
	}
	
	public static Logger getLMSLogger()
	{
		return log;
	}
	
	public static void removeDir(String dirname)
	{
		File dir = new File(dirname);
		log.info("Deleting folder " + dir.getAbsolutePath());
		deleteDir(dir);
	}
	
	private static void deleteDir(File f)
	{
		if(f.exists())
		{
			if(f.isDirectory() && f.list().length == 0)
			{
				f.delete();
			}
			else if(!f.isDirectory())
			{
				f.delete();
			}
			else
			{
				String[]entries = f.list();
				for(String s: entries){
				    File currentFile = new File(f.getPath(),s);
				    if(currentFile.isDirectory())
				    	deleteDir(currentFile);
				    else
				    	currentFile.delete();
				}
				f.delete();
			}
		}
	}
	
	public static String formatStr(ChatColor col, String msg)
	{
		return col + "" + ChatColor.BOLD + "> " + ChatColor.RESET + "" + ChatColor.GRAY + msg;
	}
	
	public static String formatTime(int time)
	{
		return (String.format("%02d", (int)(time/60)) + ":" + String.format("%02d", (int)(time%60)));
	}
}
