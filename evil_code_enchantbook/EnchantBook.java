package Evil_Code_EnchantBook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnchantBook extends JavaPlugin implements Listener{
	Map<Enchantment, Integer> configMax = new HashMap<Enchantment, Integer>();
	private int defaultConfigMax = 10;
	private boolean useOldEnchanting=true;
	
	final int projectID = 0;//Unavailable until plugin is uploaded to bukkit.com
	Map<Integer, List<String>> enchants = new HashMap<Integer, List<String>>();
	
	@Override public void onEnable(){
		//Uncomment this once posted on bukkit.org
//		new Updater(this, projectID, this.getFile(), Updater.UpdateType.DEFAULT, false);
		loadFiles();
		getServer().getPluginManager().registerEvents(this, this);
		if(useOldEnchanting) getServer().getPluginManager().registerEvents(new EnchantLapisListener(this), this);
		getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
	}
	@Override public void onDisable(){/*  */}
	
	private void loadFiles(){
		String maxLvlsFile = FileIO.loadFile("max enchant levels.txt", "");
		for(String line : maxLvlsFile.split("\n")){
			if(line.contains(":")){
				String[] part = line.split(":");
				if(part[0].equals("defaultmax")) defaultConfigMax = Integer.parseInt(part[1]);
				else if(line.startsWith("useoldenchanting(pre-1.8):")) useOldEnchanting = Boolean.parseBoolean(part[1]);
				else{
					try{configMax.put(Enchantment.getByName(part[0].toUpperCase()), Integer.parseInt(part[1]));}
					catch(NumberFormatException ex){}
				}
			}
		}
		if(configMax.size() != Enchantment.values().length){
		
			StringBuilder maxLvlsData = new StringBuilder();
			for(Enchantment enchant : Enchantment.values()){
				maxLvlsData.append(enchant.getName().toLowerCase()).append(": ").append(enchant.getMaxLevel()).append('\n');
			}
			maxLvlsData.append("\n\n#The Default maximum for any enchant, 0 for no absolute cap.\nDefault Max: "+defaultConfigMax
					+ "\nUse old enchanting (pre-1.8): "+useOldEnchanting);
			
			FileIO.saveFile("max enchant levels.txt", maxLvlsData.toString());
		}
		
		String enchantNamesFile = FileIO.loadFile("enchants.txt", getClass().getResourceAsStream("/default names.txt"));
		for(String line : enchantNamesFile.split("\n")){
			String[] data = line.replace(" ", "").replace("\t", "").toLowerCase().split(":");
			if(data.length > 1){
				try{enchants.put(Integer.parseInt(data[0]), Arrays.asList(data[1].split(",")));}
				catch(NumberFormatException ex2){getLogger().info(ex2.getMessage());}
			}
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]){
		if((sender instanceof Player) == false){
			sender.sendMessage("�cThis command can only be run by in-game players!");
			return false;
		}
		Player p = (Player) sender;
		
		if(cmd.getName().equals("enchant")){
			Enchantment[] enchantsToApply;
			boolean onlyApplicable = false;
			int level = 0;
			//--- Get info -----------------------------------------------------------------
			ItemStack heldItem = p.getInventory().getItemInMainHand();
			if(heldItem == null || heldItem.getType() == Material.AIR){
				p.sendMessage("�cYou must be holding an item to use this command.");
				return true;
			}
			else if(args.length == 0){
				p.sendMessage("�cPlease specify an enchantment");
				return false;
			}
			enchantsToApply = getEnchantments(args[0].replace("-", "_"));
			
			if(enchantsToApply == null){
				p.sendMessage(" \n\n\n\n\n\n\n�7Unknown/Un-added enchantment.");
				if(!Bukkit.getBukkitVersion().equals(Bukkit.getVersion())){
					p.sendMessage("�cLooks like the plugin and server are running on different versions\n"
								+ "�cTo get new enchantments, use enchantment IDs�7(/enchant [id] [lvl])");
				}
				else{
					StringBuilder list = new StringBuilder("�e  ");
					for(int enchantID : enchants.keySet()){
						list.append(enchants.get(enchantID).get(0));
						list.append("�7(�e");
						list.append(enchantID);
						list.append("�7)�e, ");
					}
					p.sendMessage("�bPlease reference the list below:\n"+ list.substring(0, list.length()-4));
				}
				return true;
			}
			
			if(args.length == 1){
				level = 1;
			}
			else{
				if(args.length > 2){for(int i=2; i<args.length; ++i) args[1] += ' '+args[i];}
				args[1] = args[1].toLowerCase();
				
				if(args[1].contains("app") || args[1].contains("true")) onlyApplicable = true;
				
				if(args[1].contains("max")){
					if(args[1].contains("nat")) level = -1;//natural max
					else if(args[1].contains("conf")) level = -2;//configuration-set max
					else if(args[1].contains("minec") || args[1].contains("mc") ||//max supported by the game
							args[1].contains("int") || args[1].contains("abs")) level = 32767;
					else level = -1;
				}
				else{
					args[1] = args[1].split(" ")[0].replaceAll("[^0-9]", "");;
					if(args[1].isEmpty()){
						p.sendMessage("�cPlease specify the level of the enchantment");
						return false;
					}
					level = Integer.parseInt(args[1]);
				}
			}
			boolean aboveNatural = false, aboveConfig = false;
			if(level > 0) for(Enchantment ench : enchantsToApply){
				if(level > ench.getMaxLevel()) aboveNatural = true;
				if(level > maxValue(ench)) aboveConfig = true;
			}
			else if(level == -1) for(Enchantment ench : enchantsToApply){
				if(ench.getMaxLevel() > maxValue(ench)) aboveConfig = true;
			}
			else if(level == -2) for(Enchantment ench : enchantsToApply){
				if(maxValue(ench) > ench.getMaxLevel()) aboveNatural = true;
			}
			
			if(!p.hasPermission("evp.evchant.command.unrestriced")){
				if(!p.hasPermission("evp.evchant.command.abovenatural") && aboveNatural){
					p.sendMessage("�4You do not have permission to enchant items above the maximum natural levels");
					return true;
				}
				else if(!p.hasPermission("evp.evchant.command.aboveconfig") && aboveConfig){
					p.sendMessage("�4You do not have permission to enchant items above the max level defined in the config");
					return true;
				}
			}
			
			if(heldItem.getType() != Material.ENCHANTED_BOOK){
				ItemMeta meta = heldItem.getItemMeta();
				
				for(Enchantment ench : enchantsToApply){
					if(onlyApplicable && ench.canEnchantItem(heldItem) == false) continue;
					
					if(meta.hasConflictingEnchant(ench) && p.hasPermission("evp.evchant.command.conflicting") == false){
						p.sendMessage("�cYou do not have permission to add conflicting enchantments to this item.");
						return true;
					}
					if(ench.canEnchantItem(heldItem) == false && p.hasPermission("evp.evchant.command.anyitem") == false){
						p.sendMessage("�cYou do not have permission to add this enchantment to this item.");
						return true;
					}
					if(level != 0) meta.addEnchant(ench,
							((level == -1) ? ench.getMaxLevel() : (level == -2) ? maxValue(ench) : level),
									true);
					else meta.removeEnchant(ench);
				}
				heldItem.setItemMeta(meta);
				
				if(level == 0) p.sendMessage("�bSuccessfully removed enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" from this �e"+heldItem.getType().toString());
				
				else if(level == -1) p.sendMessage("�bSuccessfully added enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" at max natural level to this �e"+heldItem.getType().toString());
				
				else if(level == -2) p.sendMessage("�bSuccessfully added enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" at max config level to this �e"+heldItem.getType().toString());
				
				else p.sendMessage("�bSuccessfully added enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" at level �e"+level+"�b to this �e"+heldItem.getType().toString());
			}
			else{
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)heldItem.getItemMeta();
				
				for(Enchantment ench : enchantsToApply){
					if(bookmeta.hasConflictingStoredEnchant(ench) && p.hasPermission("evp.evchant.command.conflicting") == false){
						p.sendMessage("�cYou do not have permission to store conflicting enchantments in this enchanted_book.");
						return true;
					}
					if(level != 0) bookmeta.addStoredEnchant(ench,
							((level == -1) ? ench.getMaxLevel() : (level == -2) ? maxValue(ench) : level),
									true);
					else bookmeta.removeStoredEnchant(ench);
				}
				heldItem.setItemMeta(bookmeta);
				
				if(level == 0) p.sendMessage("�bSuccessfully removed stored enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" from this book");

				else if(level == -1) p.sendMessage("�bSuccessfully stored enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" at max natural level in this book");

				else if(level == -2) p.sendMessage("�bSuccessfully stored enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" at max config level in this book");

				else p.sendMessage("�bSuccessfully stored enchantment"
						+ ((enchantsToApply.length>1)?'s':"")+" at level �e"+level+"�b to this book");
			}
			return true;
		}
		else if(cmd.getName().equals("combineall")){
			p.getInventory().setContents(combineAllBooks(p.getInventory().getContents(), (Permissible)p));
			p.sendMessage("�bAll books combined!");
			return true;
		}
		else if(cmd.getName().equals("separate")){
			int enchCount=0, bookCount=0, enchBookCount=0, invSpaces=0;

			for(ItemStack item : p.getInventory().getContents()){
				if(item != null && item.getType() != Material.AIR){
					if(item.getType() == Material.ENCHANTED_BOOK){
						bookCount+=item.getAmount(); enchBookCount+=item.getAmount();
						enchCount += ((EnchantmentStorageMeta)item.getItemMeta()).getStoredEnchants().size();
					}
					else if(item.getType() == Material.BOOK) bookCount+=item.getAmount();
				}
				else invSpaces++;
			}
			if(p.hasPermission("evp.evchant.separatebooks.free") == false && p.getGameMode() != GameMode.CREATIVE){
				if(bookCount < enchCount){
					p.sendMessage("�cYou need �6"+(enchCount-bookCount)+"�c more blank books in your inventory to do this");
					return true;
				}
				p.getInventory().remove(Material.BOOK);
				ItemStack bookReturn = new ItemStack(Material.BOOK);
				bookReturn.setAmount(bookCount-enchCount);
				if(bookReturn.getAmount() > 0) p.getInventory().addItem(bookReturn);
			}
			if((invSpaces + enchBookCount) - enchCount < 0){
				p.sendMessage("�cYou do not have enough space in your inventory to do this!");
			}
			else{
				p.getInventory().setContents(splitAllEnchantments(p.getInventory().getContents()));
				p.sendMessage("�bSuccessfully separated enchantements onto books!");
			}
			
			
			return true;
		}
		else return false;
	}
	
	Inventory lotteryChest;
	ItemStack lotteryEntry;
	@EventHandler
	public void onHopperDeposit(InventoryMoveItemEvent evt){
		if(evt.getDestination().getType() == InventoryType.CHEST && !evt.isCancelled()
					&& evt.getDestination().getName().toLowerCase().contains("lottery"))
		{
			if(lotteryEntry != null) evt.setCancelled(true);
			
			lotteryChest = evt.getDestination();
			lotteryEntry = evt.getItem();
			
			if(lotteryEntry.getType() == Material.ENCHANTED_BOOK){
				getLogger().fine("Lottery Entry Accepted");
				
				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){public void run(){
					//wait 1 tick for the book to move to the chest
					int level1Enchants = 1;
					for(int enchLvl : ((EnchantmentStorageMeta)lotteryEntry.getItemMeta()).getStoredEnchants().values()){
						level1Enchants += (2<<(enchLvl-1));
					}
					//just go ahead and mold it with what's in there
					PermissibleBase perms = new PermissibleBase(null);
					perms.addAttachment(EnchantBook.this, "evp.evchant.combine.abovenatural", true);
					lotteryChest.setContents(combineAllBooks(lotteryChest.getContents(), perms));
	
					boolean win = false;
					if(lotteryChest.firstEmpty() == -1)win = true;
					else if(new Random().nextInt(10 + (int)(90/level1Enchants)) == 0)win = true;
	
					if(win){
						DoubleChest chest = ((DoubleChest)lotteryChest.getHolder());
						org.bukkit.Location dumpLocation = chest.getLocation().add(0,1,0);
						
						boolean noWinner = true;
						for(Player p : chest.getWorld().getPlayers()){
							if(p.getLocation().distanceSquared(chest.getLocation()) < 9){
								p.sendMessage("�aASFBASFU!�4DFOKCANFDF!!�6 You won the Lottery!!!");
								for(ItemStack leftover : p.getInventory().addItem(lotteryChest.getContents()).values()){
									if(leftover != null) chest.getWorld().dropItem(dumpLocation, leftover);
								}
								noWinner = false;
							}
						}
						if(noWinner){
							for(ItemStack prize : lotteryChest.getContents()){
								if(prize != null) chest.getWorld().dropItemNaturally(dumpLocation, prize);
							}
						}
						lotteryChest.clear();
					}
					lotteryEntry = null;
				}}, 1);
			}//if they entered an enchanted book
			else{
				getLogger().info("Lottery Entry Invalid");
				DoubleChest chest = ((DoubleChest)lotteryChest.getHolder());
				chest.getWorld().dropItemNaturally(chest.getLocation().add(0,2,0), lotteryEntry);
				
				getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){public void run(){
					lotteryChest.remove(lotteryEntry);
					lotteryEntry = null;
				}}, 1);
			}
		}//if the conditions are met
	}
	
	//Non event-triggered methods.
	public int maxValue(Enchantment ench){
		return (configMax.containsKey(ench)) ? configMax.get(ench) : defaultConfigMax;
	}
	
	@SuppressWarnings("deprecation")
	public Enchantment[] getEnchantments(String str){
		if(str.equalsIgnoreCase("all")) return Enchantment.values();
		else if(Enchantment.getByName(str) != null) return new Enchantment[]{Enchantment.getByName(str)};
		
		else for(int id : enchants.keySet()){
			if(enchants.get(id).contains(str)) return new Enchantment[]{Enchantment.getById(id)};
		}
		try{
			int id = Integer.parseInt(str);
			//TODO: enable adding of new enchantments (via ID) without requiring a plugin update
			return new Enchantment[]{Enchantment.getById(id)};
		}
		catch(NumberFormatException ex){}
		return null;//new Enchantment[]{Enchantment.DAMAGE_ALL};//basically an 'else'
	}
	
	private ItemStack[] splitAllEnchantments(ItemStack[] inv){
		getLogger().info("method got called");
		List<ItemStack> stuff = new ArrayList<ItemStack>();for(ItemStack item : inv)stuff.add(item);
		
		StringBuilder names = new StringBuilder();
		StringBuilder values = new StringBuilder();
		
		for(int i = 0; i < stuff.size(); ++i){
			if(stuff.get(i) != null && stuff.get(i).getType() == Material.ENCHANTED_BOOK){
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)stuff.get(i).getItemMeta();
				//
				for(Enchantment enchant : bookmeta.getStoredEnchants().keySet()){
					names.append(enchant.getName()); names.append(',');
					values.append(Integer.toString(bookmeta.getStoredEnchantLevel(enchant))); values.append(',');
					
					stuff.set(i, null);
				}
			}
		}
		while(stuff.contains(null))stuff.remove(null);
		
		if(names.length() == 0) return inv;
		
		String[] nameString = names.substring(0, names.length()-1).split(",");
		String[] valueString = values.substring(0, values.length()-1).split(",");
		
		for(int i = 0; i < nameString.length; ++i){
			ItemStack newbook = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)newbook.getItemMeta();
			bookmeta.addStoredEnchant(
					Enchantment.getByName(nameString[i]),
					Integer.parseInt(valueString[i]),
					true);
			newbook.setItemMeta(bookmeta);
			stuff.add(newbook);
		}
		
		return stuff.toArray(new ItemStack[]{});
	}
	
	private ItemStack[] combineAllBooks(ItemStack[] inv, Permissible perms){
		List<ItemStack> stuff = new ArrayList<ItemStack>();for(ItemStack item : inv)stuff.add(item);
		//
		StringBuilder enchNames = new StringBuilder(), enchValues = new StringBuilder();
		
		for(int i = 0; i < stuff.size(); ++i){
			if(stuff.get(i) != null && stuff.get(i).getType() == Material.ENCHANTED_BOOK){
				//
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)stuff.get(i).getItemMeta();
				//
				for(Enchantment enchant : bookmeta.getStoredEnchants().keySet()){
					enchNames.append(enchant.getName()).append(',');
					enchValues.append(bookmeta.getStoredEnchantLevel(enchant)).append(',');
					stuff.set(i, null);
				}
			}
		}
		while(stuff.contains(null))stuff.remove(null);
		
		if(enchNames.length() == 0) return inv;
		
		String[] nameString = enchNames.substring(0, enchNames.length()-1).split(",");
		String[] valueString = enchValues.substring(0, enchValues.length()-1).split(",");
		Enchantment[] enchants = new Enchantment[nameString.length];
		int[] values = new int[enchants.length];
		
		for(int i = 0; i < enchants.length; ++i){
			enchants[i] = Enchantment.getByName(nameString[i]);
			values[i] = Integer.parseInt(valueString[i]);
		}
		boolean matches = true;
		while(matches){
			matches = false;
			//-1 is treated as 'null'
			for(int i = 0; i < enchants.length; ++i){
				
				if(enchants[i] != null)
				for(int x = 0; x < enchants.length; ++x){
					if(enchants[x] != null)
						if(x != i && enchants[i].getName().equals(enchants[x].getName()) && values[i] == values[x]){
						//
						if(!perms.hasPermission("evp.evchant.combine.unrestricted") && (
							(!perms.hasPermission("evp.evchant.combine.abovenatural") && values[i]+1 > enchants[i].getMaxLevel()) ||
							(!perms.hasPermission("evp.evchant.combine.aboveconfig") && values[i]+1 > maxValue(enchants[i]))));
						else{
							++values[i];
							enchants[x] = null;
							matches = true;
						}
					}
				}
			}
		}
		for(int i = 0; i < enchants.length; ++i){
			if(enchants[i] != null){
				ItemStack newbook = new ItemStack(Material.ENCHANTED_BOOK);
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)newbook.getItemMeta();
				bookmeta.addStoredEnchant(enchants[i], values[i], true);
				newbook.setItemMeta(bookmeta);
				stuff.add(newbook);
			}
		}
		return stuff.toArray(new ItemStack[]{});
	}
}