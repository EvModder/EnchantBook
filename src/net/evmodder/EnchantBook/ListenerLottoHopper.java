package net.evmodder.EnchantBook;

import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.scheduler.BukkitRunnable;

public class ListenerLottoHopper implements Listener{
	EnchantBook pl;
	public ListenerLottoHopper(EnchantBook plugin){pl = plugin;}
	Inventory lotteryChest;
	ItemStack lotteryEntry;

	@EventHandler
	public void onHopperDeposit(InventoryMoveItemEvent evt){
		if(evt.getDestination().getType() == InventoryType.CHEST && !evt.isCancelled()
	//				&& evt.getDestination(.getName().toLowerCase().contains("lottery")
		){
			if(lotteryEntry != null) evt.setCancelled(true);

			lotteryChest = evt.getDestination();
			lotteryEntry = evt.getItem();

			if(lotteryEntry.getType() == Material.ENCHANTED_BOOK){
				pl.getLogger().fine("Lottery Entry Accepted");

				new BukkitRunnable(){public void run(){
					//wait 1 tick for the book to move to the chest
					int level1Enchants = 1;
					for(int enchLvl : ((EnchantmentStorageMeta)lotteryEntry.getItemMeta()).getStoredEnchants().values()){
						level1Enchants += (2<<(enchLvl-1));
					}
					//just go ahead and mold it with what's in there
					PermissibleBase perms = new PermissibleBase(null);
					perms.addAttachment(pl, "evp.evchant.combine.abovenatural", true);
					lotteryChest.setContents(pl.combine(lotteryChest.getContents(), perms));

					boolean win = false;
					if(lotteryChest.firstEmpty() == -1)win = true;
					else if(new Random().nextInt(10 + (int)(90/level1Enchants)) == 0) win = true;

					if(win){
						DoubleChest chest = ((DoubleChest)lotteryChest.getHolder());
						org.bukkit.Location dumpLocation = chest.getLocation().add(0,1,0);
						
						boolean noWinner = true;
						for(Player p : chest.getWorld().getPlayers()){
							if(p.getLocation().distanceSquared(chest.getLocation()) < 9){
								p.sendMessage(
										ChatColor.GREEN+"ASFBASFU!"+
										ChatColor.RED+"DFOKCANFDF!! "+
										ChatColor.GOLD+"You won the Lottery!!!");
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
				}}.runTaskLater(pl, 1);
			}//if they entered an enchanted book
			else{
				pl.getLogger().info("Lottery Entry Invalid");
				DoubleChest chest = ((DoubleChest)lotteryChest.getHolder());
				chest.getWorld().dropItemNaturally(chest.getLocation().add(0,2,0), lotteryEntry);

				new BukkitRunnable(){public void run(){
					lotteryChest.remove(lotteryEntry);
					lotteryEntry = null;
				}}.runTaskLater(pl, 1);
			}
		}//if the conditions are met
	}
}