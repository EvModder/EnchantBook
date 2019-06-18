package Evil_Code_EnchantBook;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.scheduler.BukkitRunnable;

public class AnvilListener implements Listener{
	private EnchantBook plugin;
	Set<AnvilInventory> openAnvils;

	public AnvilListener(EnchantBook pl){
		plugin = pl;
		openAnvils = new HashSet<AnvilInventory>();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAnvilClick(final InventoryClickEvent evt){
		if(!evt.isCancelled() && evt.getInventory().getType() == InventoryType.ANVIL
				&& evt.getWhoClicked().hasPermission("evp.evchant.anvil"))
		{
			if(evt.getWhoClicked().hasPermission("evp.evchant.anvil.color")){
				openAnvils.add((AnvilInventory) evt.getInventory());
				if(openAnvils.size() == 1) runAnvilColorRenameWatcherLoop();
			}
			
			if(evt.getRawSlot() > 2 || (evt.getInventory().getItem(0) == null && evt.getInventory().getItem(1) == null) ||
					(evt.getRawSlot() != 2 && evt.getInventory().getItem(evt.getRawSlot()) != null) ||
					(evt.getRawSlot() == 2 && evt.getInventory().getItem(2) == null)) return;
			
			//--------- This is unnecessarilly run every time the anvil gets clicked -------------------------
			ItemStack[] slotsToCombine = null;
			if(evt.getInventory().getItem(0) != null && evt.getInventory().getItem(1) != null)
				slotsToCombine = new ItemStack[]{evt.getInventory().getItem(0),evt.getInventory().getItem(1)};

			else if(evt.getCursor() != null && evt.getCursor().getType() != Material.AIR){
				if(evt.getInventory().getItem(0) == null && evt.getRawSlot() == 0)
					slotsToCombine = new ItemStack[]{evt.getCursor().clone(),evt.getInventory().getItem(1)};
				
				else if(evt.getInventory().getItem(1) == null && evt.getRawSlot() == 1)
					slotsToCombine = new ItemStack[]{evt.getInventory().getItem(0),evt.getCursor().clone()};
			}
			final ItemStack item;
			if(slotsToCombine != null){
				if(slotsToCombine[0].getType() != slotsToCombine[1].getType() && slotsToCombine[1].getType() != Material.ENCHANTED_BOOK) return;
				item = forgeItemFromInventory(plugin.getServer().getPlayer(evt.getWhoClicked().getUniqueId()), slotsToCombine);
			}
			else item = evt.getInventory().getItem(0).clone();
			//-----------------------------------------------------------------------------------------------

			if(evt.getRawSlot() == 2 && evt.getInventory().getItem(2) != null){
				String itemName = evt.getInventory().getItem(2).getItemMeta().getDisplayName();
				
				if(evt.getWhoClicked().hasPermission("evp.evchant.anvil.color") &&
						evt.getInventory().getItem(2).hasItemMeta() && evt.getInventory().getItem(2).getItemMeta().hasDisplayName())
				{
					openAnvils.remove(evt.getInventory());

					if(evt.getInventory().getItem(0).hasItemMeta() && evt.getInventory().getItem(0).getItemMeta().hasDisplayName() &&
						evt.getInventory().getItem(0).getItemMeta().getDisplayName().replace("�", "").equals(itemName))
					{
						itemName = evt.getInventory().getItem(0).getItemMeta().getDisplayName();
					}
					else itemName = itemName.replace("&", "�").replace("� ", "& ").replace("\\�", "&");
				}
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(itemName);
				item.setItemMeta(meta);

				evt.setCurrentItem(item);
			}
			else{
				new BukkitRunnable(){@Override public void run(){evt.getInventory().setItem(2, item);}}.runTaskLater(plugin, 1);
			}
		}//if it's an anvil
	}

	@EventHandler
	public void onAnvilClose(InventoryCloseEvent evt){
		if(evt.getInventory().getType() == InventoryType.ANVIL && evt.getPlayer().hasPermission("evp.evchant.anvil.color")){
			openAnvils.remove(evt.getInventory());
		}
	}

	private void runAnvilColorRenameWatcherLoop(){
		new BukkitRunnable(){@Override public void run(){
			for(AnvilInventory anvil : openAnvils){
				ItemStack result = anvil.getItem(2);
				if(result != null && result.getType() != Material.AIR && result.hasItemMeta() && result.getItemMeta().hasDisplayName()){
					ItemMeta meta = result.getItemMeta();
					meta.setDisplayName(meta.getDisplayName().replace("&", "�").replace("� ", "& ").replace("\\�", "&"));
					result.setItemMeta(meta);
					anvil.setItem(2, result);
				}
			}
			if(!openAnvils.isEmpty()) runAnvilColorRenameWatcherLoop();
		}}.runTaskLater(plugin, 2);
	}

	private ItemStack forgeItemFromInventory(Permissible p, ItemStack[] inv){
		ItemStack result = inv[0].clone();
		if(result.getDurability() != 0){
			short maxDurability = result.getType().getMaxDurability();
			short durability = (short) Math.floor(maxDurability*.12);
			for(ItemStack item : inv) if(item.getType() == result.getType()) durability += maxDurability-item.getDurability();
			result.setDurability(durability > maxDurability ? 0 : (short)(maxDurability-durability));
		}

		StringBuilder enchNames = new StringBuilder();
		StringBuilder enchValues = new StringBuilder();
		boolean books = true;

		for(int i = 0; i < inv.length; ++i){
			for(Enchantment enchant : inv[i].getEnchantments().keySet()){
				enchNames.append(enchant.getName()).append(',');
				enchValues.append(inv[i].getEnchantmentLevel(enchant)).append(',');
			}
			if(inv[i].getType() == Material.ENCHANTED_BOOK){
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)inv[i].getItemMeta();

				for(Enchantment enchant : bookmeta.getStoredEnchants().keySet()){
					enchNames.append(enchant.getName()).append(',');
					enchValues.append(bookmeta.getStoredEnchantLevel(enchant)).append(',');
				}
			}
			else books = false;
		}

		if(enchNames.length() == 0) return result;

		String[] nameString = enchNames.substring(0, enchNames.length()-1).split(",");
		String[] valueString = enchValues.substring(0, enchValues.length()-1).split(",");
		Enchantment[] enchants = new Enchantment[nameString.length];
		int[] values = new int[enchants.length];

		for(int i = 0; i < nameString.length; ++i){
			try{
				enchants[i] = Enchantment.getByName(nameString[i]);
				values[i] = Integer.parseInt(valueString[i]);
			}catch(NumberFormatException e){return null;}
		}
		boolean matches = true;
		while(matches){
			matches = false;
			for(int i = 0; i < enchants.length; ++i){
				if(enchants[i] != null)
				for(int x = 0; x < enchants.length; x++){

					if(enchants[x] != null)
					if(x != i && enchants[i].getName().equals(enchants[x].getName()) && values[i] == values[x]){
						//
						if(!p.hasPermission("evp.evchant.anvil.unrestricted") && (
							(!p.hasPermission("evp.evchant.anvil.abovenatural") && values[i]+1 > enchants[i].getMaxLevel()) ||
							(!p.hasPermission("evp.evchant.anvil.aboveconfig") && values[i]+1 > plugin.maxValue(enchants[i])))){
						}
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
			if(enchants[i] != null)
			for(int x = 0; x < enchants.length; ++x){

				if(enchants[x] != null)
				if(x != i && enchants[i].getName().equals(enchants[x].getName())){
					if(values[i] > values[x]) enchants[x] = null;
					else enchants[i] = null;
				}
			}
		}
		//Add the enchantments/(or stored enchants) of both items combined to the resulting item

		if(books){//if it is just a bunch of books added together
			EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)result.getItemMeta();
			//first remove the old stored enchants
			for(Enchantment storedEnchant : bookmeta.getStoredEnchants().keySet()) bookmeta.removeStoredEnchant(storedEnchant);

			//then add the new stored enchants
			for(int i = 0; i < enchants.length; ++i)if(enchants[i] != null)
					bookmeta.addStoredEnchant(enchants[i], values[i], true);
			//and then register it all
			result.setItemMeta(bookmeta);
		}
		else{//if any of the items is NOT an enchanted book
			ItemMeta meta = result.getItemMeta();
			//first remove the enchants
			for(Enchantment enchant : result.getEnchantments().keySet())meta.removeEnchant(enchant);

			//then add the new enchants
			for(int i = 0; i < enchants.length; ++i)if(enchants[i] != null){
				//
				if((meta.hasConflictingEnchant(enchants[i]) && p.hasPermission("evp.evchant.anvil.conflicting") == false) ||
					(enchants[i].canEnchantItem(result) == false && p.hasPermission("evp.evchant.anvil.anyitem") == false)){
				}
				else meta.addEnchant(enchants[i], values[i], true);
			}
			//and then register it all
			result.setItemMeta(meta);
		}
		return result;
	}
}