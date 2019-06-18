package Evil_Code_EnchantBook;

import java.util.ArrayList;
import java.util.List;

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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class EnchantLapisListener implements Listener{
	@SuppressWarnings("deprecation")
	final ItemStack lapis = new ItemStack(Material.INK_SACK,3,(short)0,(byte)4);
	private JavaPlugin plugin;
	
	public EnchantLapisListener(JavaPlugin pl){
		plugin = pl;
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantPrepare(InventoryOpenEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING){
			evt.getInventory().setItem(1, lapis);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantPrepare(InventoryClickEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING && evt.getSlotType() == SlotType.CRAFTING
				&& evt.getSlot() == 1) evt.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantClose(InventoryCloseEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING){
			ItemStack item = evt.getInventory().getItem(1);
			if(item != null && item.getAmount() >= 3) item.setAmount(item.getAmount()-3);
			evt.getInventory().setItem(1, item);
		}
	}
	
	private List<EnchantItemEvent> enchantEvents = new ArrayList<EnchantItemEvent>();
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemEnchant(EnchantItemEvent evt){
		if(evt.isCancelled()) return;
		/*
		if(evt.getItem().getType() == Material.BOOK){
			evt.getItem().setType(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta meta = (EnchantmentStorageMeta) evt.getItem().getItemMeta();
			for(Enchantment enchant : evt.getEnchantsToAdd().keySet()){
				meta.addStoredEnchant(enchant, evt.getEnchantsToAdd().get(enchant), true);
			}
			evt.getItem().setItemMeta(meta);
		}
		else evt.getItem().addEnchantments(evt.getEnchantsToAdd());
		*/
		
		//preserve lapis ----------
		evt.getInventory().setItem(1, evt.getInventory().getItem(1));
	
		enchantEvents.add(evt);
		new BukkitRunnable(){
			@Override public void run(){
				EnchantItemEvent evt = enchantEvents.remove(0);
				if(evt.getInventory().getItem(1) == null) evt.getInventory().setItem(1, lapis);
				
				evt.getEnchanter().setLevel((evt.getEnchanter().getLevel()-evt.getExpLevelCost())+evt.whichButton()+1);
			}
			
		}.runTaskLater(plugin, 1);
		//-------------------------
		/*
		ItemStack result = evt.getInventory().getItem(0);
		evt.getInventory().setItem(0, new ItemStack(Material.AIR));
		if(evt.getEnchanter().getInventory().addItem(result).isEmpty() == false){
			evt.getEnchanter().setItemOnCursor(result);//TODO: test this for bugs!!
		}
		*/
	}
	
/*	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantPrepare(InventoryOpenEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING){
			evt.getInventory().setItem(1, lapis);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantPrepare(InventoryClickEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING && evt.getSlotType() == SlotType.CRAFTING
				&& evt.getSlot() == 1) evt.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEnchantPrepare(InventoryCloseEvent evt){
		if(evt.getInventory().getType() == InventoryType.ENCHANTING){
			ItemStack item = evt.getInventory().getItem(1);
			item.setAmount(item.getAmount()-3);
			evt.getInventory().setItem(1, item);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemEnchant(EnchantItemEvent evt){
		ItemStack item = evt.getInventory().getItem(1);
		if(item.getAmount() == 3);// item.setAmount(item.getAmount()+3);
		evt.getInventory().setItem(1, item);
		
		
		int cost = evt.getExpLevelCost();
		if(cost == 30) cost -=3;
		else if(cost < 8) cost -=1;
		else cost -=2;
		
		evt.getEnchanter().setLevel(evt.getEnchanter().getLevel()-cost);
	}*/
}
