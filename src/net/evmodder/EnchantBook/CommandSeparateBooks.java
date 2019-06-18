package net.evmodder.EnchantBook;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import EvLib.CommandBase2;

public class CommandSeparateBooks extends CommandBase2{
	EnchantBook pl;
	public CommandSeparateBooks(EnchantBook plugin){
		super(plugin);
		pl = plugin;
	}

	@Override public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if((sender instanceof Player) == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return false;
		}
		Player p = (Player) sender;

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
				p.sendMessage(ChatColor.RED+"You need "+ChatColor.GOLD+(enchCount-bookCount)+
							ChatColor.RED+" more blank books in your inventory to do this");
				return true;
			}
			p.getInventory().remove(Material.BOOK);
			ItemStack bookReturn = new ItemStack(Material.BOOK);
			bookReturn.setAmount(bookCount-enchCount);
			if(bookReturn.getAmount() > 0) p.getInventory().addItem(bookReturn);
		}
		if(invSpaces + enchBookCount - enchCount < 0){
			p.sendMessage(ChatColor.RED+"You do not have enough space in your inventory to do this!");
		}
		else{
			p.getInventory().setContents(pl.split(p.getInventory().getContents()));
			p.sendMessage(ChatColor.AQUA+"Successfully separated enchantements onto books!");
		}
		return true;
	}
}