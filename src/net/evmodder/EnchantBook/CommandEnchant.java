package net.evmodder.EnchantBook;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import net.evmodder.EnchantBook.EnchantBook.LimitType;
import net.evmodder.EvLib.bukkit.EvCommand;

public class CommandEnchant extends EvCommand{
	public CommandEnchant(EnchantBook plugin){
		super(plugin);
	}

	int getLevel(Enchantment ench, int lvl){
		switch(lvl){
			case -1:
				return EnchantBook.getMaxLevel(ench, LimitType.VANILLA);
			case -2:
				return EnchantBook.getMaxLevel(ench, LimitType.CONFIG);
			case -3:
				return EnchantBook.getMaxLevel(ench, LimitType.GAME);
			default:
				return lvl;
		}
	}

	@Override public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3){
		// TODO: stuff here
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
		if(sender instanceof Player == false){
			sender.sendMessage(ChatColor.RED+"This command can only be run by in-game players!");
			return false;
		}
		Player p = (Player) sender;
		ItemStack heldItem = p.getInventory().getItemInMainHand();
		if(heldItem == null || heldItem.getType() == Material.AIR){
			p.sendMessage(ChatColor.RED+"You must be holding an item to use this command.");
			return true;
		}
		else if(args.length == 0){
			p.sendMessage(ChatColor.RED+"Please specify at least one enchantment");
			return false;
		}
		//TODO: multiple args, 1 per enchant, of form: <name>:<level>
		Collection<Enchantment> enchants = EnchantAPI.parseEnchantList(args[0]);

		if(enchants.isEmpty()){
			p.sendMessage(ChatColor.RED+"Unknown/Un-added enchantment.");
			if(!Bukkit.getBukkitVersion().equals(Bukkit.getVersion())){
				p.sendMessage(ChatColor.GRAY+"The plugin and server appear to be on different versions!");
				p.sendMessage(ChatColor.GRAY+"Please update to the lastest release");
			}
			/*else{
				StringBuilder list = new StringBuilder(ChatColor.AQUA+"Please reference the list below:\n");
				for(String id : EnchantBook.getPlugin().enchantLookupMap.keySet()){
					String[] names = pl.easyNames.getString(id).split(",");
					list.append(ChatColor.YELLOW).append(names[0]).append(ChatColor.GRAY)
						.append('(').append(ChatColor.AQUA).append(id).append(ChatColor.GRAY)
						.append(')').append(ChatColor.DARK_GRAY).append(", ");
				}
				p.sendMessage(list.substring(0, list.length()-2)+'.');
			}*/
			return true;
		}

		int lvlType = 1;
		if(args.length > 1){
			String lvlArg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

			if(lvlArg.contains("app") || lvlArg.contains("true")){
				// Only apply if this enchantment is regularly applicable to the particular item
				// (Don't add enchants that don't normally go on this item)
				enchants.removeIf((Enchantment e) -> e.canEnchantItem(heldItem) == false);
			}

			if(lvlArg.contains("max")){
				if(lvlArg.contains("nat")) lvlType = -1;//natural max
				else if(lvlArg.contains("minec") || lvlArg.contains("mc") ||
						lvlArg.contains("abs")) lvlType = EnchantBook.MAX_ENCHANT_LEVEL;
//				else if(lvlArg.contains("conf")) level = -2;//configuration-set max
				else lvlType = -2;
			}
			else{
				lvlArg = args[args.length-1].replaceAll("[^0-9]", "");
				if(lvlArg.isEmpty()){
					p.sendMessage(ChatColor.RED+"Please specify the level of the enchantment");
					return false;
				}
				lvlType = Integer.parseInt(lvlArg);
			}
		}

		boolean aboveNatural = p.hasPermission("enchantbook.enchant.abovenatural");
		boolean aboveConfig = p.hasPermission("enchantbook.enchant.aboveconfig");
		boolean conflicting = p.hasPermission("enchantbook.enchant.conflicting");
		boolean anyItem = p.hasPermission("enchantbook.enchant.anyitem");
		if(heldItem.getType() != Material.ENCHANTED_BOOK){
			ItemMeta meta = heldItem.getItemMeta();
			for(Enchantment ench : enchants){
				int level = getLevel(ench, lvlType);
				int maxNatural = EnchantBook.getMaxLevel(ench, LimitType.VANILLA);
				int maxConfig = EnchantBook.getMaxLevel(ench, LimitType.CONFIG);
				if(level > maxNatural && !aboveNatural) level = maxNatural;
				if(level > maxConfig && !aboveConfig) level = maxConfig;

				if(meta.hasConflictingEnchant(ench) && !conflicting){
					p.sendMessage(ChatColor.RED+"Insufficient permission to add conflicting enchant "
							+ChatColor.GOLD+ench.getKey().getKey().toLowerCase());
					return true;
				}
				if(ench.canEnchantItem(heldItem) == false && !anyItem){
					p.sendMessage(ChatColor.RED+"Insufficient permission to enchant "
							+ChatColor.GOLD+heldItem.getType().name().toLowerCase()+ChatColor.RED+" with "
							+ChatColor.GOLD+ench.getKey().getKey().toLowerCase());
					return true;
				}
				if(lvlType == 0) meta.removeEnchant(ench);
				else meta.addEnchant(ench, getLevel(ench, lvlType), true);
			}
			heldItem.setItemMeta(meta);

			if(lvlType == 0)
				p.sendMessage(ChatColor.AQUA+"Successfully removed enchantment"+(enchants.size()==1 ? "":"s"));

			else if(lvlType == -1) p.sendMessage(ChatColor.AQUA+"Successfully added enchantment"
					+(enchants.size()==1 ? "":"s")+" at max natural level");

			else if(lvlType == -2) p.sendMessage(ChatColor.AQUA+"Successfully added enchantment"
					+(enchants.size()==1 ? "":"s")+" at max config level");

			else p.sendMessage(ChatColor.AQUA+"Successfully added enchantment"
					+(enchants.size()==1 ? "":"s")+" at level "+ChatColor.GOLD+lvlType);
		}
		else{
			EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)heldItem.getItemMeta();
			for(Enchantment ench : enchants){
				if(lvlType == 0) bookmeta.removeStoredEnchant(ench);
				else bookmeta.addStoredEnchant(ench, getLevel(ench, lvlType), true);
			}
			heldItem.setItemMeta(bookmeta);

			if(lvlType == 0)
				p.sendMessage(ChatColor.AQUA+"Successfully removed stored enchantment"+(enchants.size()==1 ? "":"s"));

			else if(lvlType == -1) p.sendMessage(ChatColor.AQUA+"Successfully stored enchantment"
					+(enchants.size()==1 ? "":"s")+" at maximum level");

			else p.sendMessage(ChatColor.AQUA+"Successfully stored enchantment"
					+(enchants.size()==1 ? "":"s")+" at level "+ChatColor.GOLD+lvlType);
		}
		return true;
	}
}