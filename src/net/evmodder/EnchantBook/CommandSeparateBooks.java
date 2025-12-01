package net.evmodder.EnchantBook;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import net.evmodder.EvLib.bukkit.EvCommand;

public class CommandSeparateBooks extends EvCommand{
	EnchantBook pl;
	public CommandSeparateBooks(EnchantBook plugin){
		super(plugin);
		pl = plugin;
	}

	@Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a){return null;}

	@Override public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if((sender instanceof Player) == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return false;
		}
		Player p = (Player) sender;

		if(args.length > 0 && p.hasPermission("enchantbook.separate.all")
				&& ((args[0]=args[0].toLowerCase()).startsWith("@a") || args[0].equals("all"))){
			//split whole inventory
			int enchCount=0, bookCount=0, invSpaces=0;

			for(ItemStack item : p.getInventory().getContents()){
				if(item == null || item.getType() == Material.AIR) ++invSpaces;
				else{
					if(item.getType() == Material.ENCHANTED_BOOK){
						bookCount+=item.getAmount(); ++invSpaces;
						enchCount += ((EnchantmentStorageMeta)item.getItemMeta()).getStoredEnchants().size();
					}
					else if(item.getType() == Material.BOOK){
						bookCount+=item.getAmount(); ++invSpaces;
					}
				}
			}
			if(!p.hasPermission("enchantbook.separate.free") && p.getGameMode() != GameMode.CREATIVE){
				if(bookCount < enchCount){
					p.sendMessage(ChatColor.RED+"You need "+ChatColor.GOLD+(enchCount-bookCount)+
								ChatColor.RED+" more blank books in your inventory to do this");
					return true;
				}
				p.getInventory().remove(Material.BOOK);
				ItemStack bookReturn = new ItemStack(Material.BOOK);
				bookReturn.setAmount(bookCount-enchCount);
				if(bookReturn.getAmount() > 0){
					p.getInventory().addItem(bookReturn);
					invSpaces -= (bookReturn.getAmount() + 63) / 64;
				}
			}
			if(enchCount > invSpaces){
				p.sendMessage(ChatColor.RED+"You do not have enough space in your inventory to do this!");
				return true;
			}
			else if(!EnchantAPI.splitBooks(p.getInventory())){
				p.sendMessage(ChatColor.RED+"Inventory space check failed! (error in plugin?)");
			}
		}
		else{
			if(p.getInventory().getItemInMainHand().getType() != Material.ENCHANTED_BOOK){
				p.sendMessage(ChatColor.RED+"You must be holding an enchanted book to run this command");
				return true;
			}
			ItemStack[] resultBooks = EnchantAPI.splitBook(p.getInventory().getItemInMainHand());
			if(resultBooks.length == 1){
				p.sendMessage(ChatColor.RED+"That book appears to already be separated");
				return true;
			}
			int numBooks = EnchantAPI.countItem(p.getInventory(), Material.BOOK) + 1;
			if(!p.hasPermission("enchantbook.separate.free") && p.getGameMode() != GameMode.CREATIVE){
				if(numBooks < resultBooks.length){
					int need = resultBooks.length - numBooks;
					p.sendMessage(ChatColor.RED+"You need "+ChatColor.GOLD+need+ChatColor.RED
							+" more regular book"+(need>1?"s":"")+" in your inventory to do this");
					return true;
				}
				p.getInventory().remove(Material.BOOK);
				if(numBooks > resultBooks.length){
					ItemStack bookReturn = new ItemStack(Material.BOOK);
					bookReturn.setAmount(numBooks - resultBooks.length);
					p.getInventory().addItem(bookReturn);
				}
			}
			p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
			for(ItemStack overflow : p.getInventory().addItem(resultBooks).values())
				p.getWorld().dropItem(p.getLocation(), overflow);
		}
		p.sendMessage(ChatColor.AQUA+"Successfully separated book enchantements!");
		return true;
	}
}