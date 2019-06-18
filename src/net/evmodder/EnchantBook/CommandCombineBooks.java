package net.evmodder.EnchantBook;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import EvLib.CommandBase2;

public class CommandCombineBooks extends CommandBase2{
	EnchantBook pl;
	public CommandCombineBooks(EnchantBook plugin){
		super(plugin);
		pl = plugin;
	}

	@Override public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if((sender instanceof Player) == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return false;
		}
		Player p = (Player) sender;
		p.getInventory().setContents(pl.combine(p.getInventory().getContents(), p));
		p.sendMessage(ChatColor.AQUA+"All books combined!");
		return true;
	}
}