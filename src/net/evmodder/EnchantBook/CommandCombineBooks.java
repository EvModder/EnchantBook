package net.evmodder.EnchantBook;

import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.evmodder.EnchantBook.EnchantBook.LimitType;
import net.evmodder.EvLib.bukkit.EvCommand;

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
		Map<Enchantment, Integer> maxLevels = EnchantBook.maxLevelMins;
		boolean aboveNatural = sender.hasPermission("enchantbook.combine.abovenatural");
		boolean aboveConfig = sender.hasPermission("enchantbook.combine.aboveconfig");
		if(args.length > 0){
			args[0] = args[0].toLowerCase();
			if(aboveNatural && aboveConfig &&
					(args[0].equals("max") || args[0].equals("game") || args[0].contains("abs")))
				maxLevels = EnchantBook.getMaxlevels(LimitType.GAME);
			else if(aboveConfig && (args[0].equals("vanilla") || args[0].contains("nat")))
				maxLevels = EnchantBook.getMaxlevels(LimitType.VANILLA);
			else if(aboveNatural && args[0].equals("config"))
				maxLevels = EnchantBook.getMaxlevels(LimitType.CONFIG);
		}

		Player p = (Player) sender;
		ItemStack[] books = EnchantAPI.combineBooks(p.getInventory().getContents(), maxLevels);
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