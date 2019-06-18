package net.evmodder.EnchantBook;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.evmodder.EnchantBook.EnchantBook.LimitType;
import net.evmodder.EvLib.EvCommand;

public class CommandCombineBooks extends EvCommand{
	public CommandCombineBooks(EnchantBook plugin){
		super(plugin);
	}

	@Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a){return null;}

	@Override public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if((sender instanceof Player) == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return false;
		}
		LimitType limit = LimitType.CONFIG;
		if(args.length == 1){
			try{limit = LimitType.valueOf(args[0].toUpperCase());}
			catch(IllegalArgumentException ex){}
		}

		Player p = (Player) sender;
		ItemStack[] books = EnchantAPI.combineBooks(p.getInventory().getContents(), EnchantBook.getMaxlevels(limit));
		for(int i = 0; i < p.getInventory().getSize(); ++i){
			if(p.getInventory().getItem(i) != null &&
					p.getInventory().getItem(i).getType() == Material.ENCHANTED_BOOK){
				p.getInventory().setItem(i, null);
			}
		}
		for(ItemStack overflow : p.getInventory().addItem(books).values())
			p.getWorld().dropItem(p.getLocation(), overflow);
		p.sendMessage(ChatColor.AQUA+"All books combined!");
		return true;
	}
}