package net.evmodder.EnchantBook;

import java.util.ArrayDeque;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ListenerLapisEnchant implements Listener{
	final ItemStack LAPIS_3 = new ItemStack(Material.LAPIS_LAZULI, 3);

	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantPrepare(InventoryOpenEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING && !evt.isCancelled()){
			evt.getInventory().setItem(1, LAPIS_3);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantPrepare(InventoryClickEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING
				&& evt.getSlotType() == SlotType.CRAFTING && evt.getSlot() == 1) evt.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantClose(InventoryCloseEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING){
			ItemStack item = evt.getInventory().getItem(1);
			if(item != null && item.getAmount() >= 3) item.setAmount(item.getAmount()-3);
			evt.getInventory().setItem(1, item);
		}
	}
	
	private ArrayDeque<EnchantItemEvent> enchantEvents = new ArrayDeque<EnchantItemEvent>();
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemEnchant(EnchantItemEvent evt){
		if(evt.isCancelled()) return;
		evt.getInventory().setItem(1, evt.getInventory().getItem(1));

		enchantEvents.addFirst(evt);
		new BukkitRunnable(){
			@Override public void run(){
				EnchantItemEvent evt = enchantEvents.removeLast();
				if(evt.getInventory().getItem(1) == null) evt.getInventory().setItem(1, LAPIS_3);
				int cost = evt.getExpLevelCost() - (evt.whichButton() + 1);
				evt.getEnchanter().setLevel(evt.getEnchanter().getLevel() - cost);
			}
			
		}.runTaskLater(EnchantBook.getPlugin(), 1);
	}
}
