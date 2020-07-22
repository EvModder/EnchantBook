package net.evmodder.EnchantBook;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.ChatColor;
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
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.scheduler.BukkitRunnable;
import net.evmodder.EvLib.extras.TextUtils;

public class AnvilListener implements Listener{
	final EnchantBook plugin;
	final boolean ALLOW_SEPARATING_ENCH_BOOKS;
	final HashMap<Permissible, AnvilInventory> openAnvils;

	public AnvilListener(){
		plugin = EnchantBook.getPlugin();
		ALLOW_SEPARATING_ENCH_BOOKS = plugin.getConfig().getBoolean("separate-books-on-anvil", true);
		openAnvils = new HashMap<Permissible, AnvilInventory>();
	}

	static boolean validItem(ItemStack item){
		return item != null && item.getType() != Material.AIR;
	}

	static boolean hasName(ItemStack item){
		return item.hasItemMeta() && item.getItemMeta().hasDisplayName();
	}

	//TODO: rewrite & move to TextUtils?
	static String translateByPermission(String str, Permissible p){
		if(p.hasPermission("enchantbook.anvil.color.*") && p.hasPermission("enchantbook.anvil.format.*"))
			return TextUtils.translateAlternateColorCodes('&', str);
		HashSet<Character> hasPerm = new HashSet<Character>();
		for(ChatColor color : ChatColor.values()){
			String colorName = color.name().toLowerCase();
			if(	p.hasPermission("enchantbook.anvil.color."+colorName) ||
				p.hasPermission("enchantbook.anvil.color."+color.getChar()) ||
				p.hasPermission("enchantbook.anvil.format."+colorName) ||
				p.hasPermission("enchantbook.anvil.format."+color.getChar())
			) hasPerm.add(color.getChar());
		}
		StringBuilder builder = new StringBuilder();
		boolean amp = false;
		for(char ch : str.toCharArray()){
			if(amp){
				if(hasPerm.contains(ch)) builder.append(ChatColor.getByChar(ch));
				else builder.append('&').append(ch);
				amp = false;
			}
			else if(ch == '&') amp = true;
			else builder.append(ch);
		}
		if(amp) builder.append('&');
		return builder.toString();
	}

	static boolean canApplyTo(ItemStack baseItem, ItemStack applied){
		if(applied.getType() == baseItem.getType() || applied.getType() == Material.ENCHANTED_BOOK) return true;
		switch(baseItem.getType()){
			case DIAMOND_SWORD:
			case DIAMOND_AXE:
			case DIAMOND_PICKAXE:
			case DIAMOND_SHOVEL:
			case DIAMOND_HOE:
			case DIAMOND_HELMET:
			case DIAMOND_CHESTPLATE:
			case DIAMOND_LEGGINGS:
			case DIAMOND_BOOTS:
				return applied.getType() == Material.DIAMOND;
			case IRON_SWORD:
			case IRON_AXE:
			case IRON_PICKAXE:
			case IRON_SHOVEL:
			case IRON_HOE:
			case IRON_HELMET:
			case IRON_CHESTPLATE:
			case IRON_LEGGINGS:
			case IRON_BOOTS:
			case CHAINMAIL_HELMET:
			case CHAINMAIL_CHESTPLATE:
			case CHAINMAIL_LEGGINGS:
			case CHAINMAIL_BOOTS:
				return applied.getType() == Material.IRON_INGOT;
			case GOLDEN_SWORD:
			case GOLDEN_AXE:
			case GOLDEN_PICKAXE:
			case GOLDEN_SHOVEL:
			case GOLDEN_HOE:
			case GOLDEN_HELMET:
			case GOLDEN_CHESTPLATE:
			case GOLDEN_LEGGINGS:
			case GOLDEN_BOOTS:
				return applied.getType() == Material.GOLD_INGOT;
			case LEATHER_HELMET:
			case LEATHER_CHESTPLATE:
			case LEATHER_LEGGINGS:
			case LEATHER_BOOTS:
				return applied.getType() == Material.LEATHER;
			case STONE_SWORD:
			case STONE_AXE:
			case STONE_PICKAXE:
			case STONE_SHOVEL:
			case STONE_HOE:
				return applied.getType() == Material.COBBLESTONE;
			case WOODEN_SWORD:
			case WOODEN_AXE:
			case WOODEN_PICKAXE:
			case WOODEN_SHOVEL:
			case WOODEN_HOE:
				switch(applied.getType()){
					case ACACIA_PLANKS:
					case BIRCH_PLANKS:
					case DARK_OAK_PLANKS:
					case JUNGLE_PLANKS:
					case OAK_PLANKS:
					case SPRUCE_PLANKS:
						return true;
					default:
						return false;
				}
			case ELYTRA:
				return applied.getType() == Material.PHANTOM_MEMBRANE;
			case TURTLE_HELMET:
				return applied.getType() == Material.SCUTE;
			default:
				return false;
		}
	}

	private ItemStack anvilOutputItem(ItemStack item0, ItemStack item1, Permissible p){
		if(ALLOW_SEPARATING_ENCH_BOOKS && item0.getType() == Material.ENCHANTED_BOOK && item1.getType() == Material.BOOK){
			Map<Enchantment, Integer> storedEnchants = ((EnchantmentStorageMeta)item0.getItemMeta()).getStoredEnchants();
			ItemStack resultBook = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta meta = (EnchantmentStorageMeta)resultBook.getItemMeta();
			Entry<Enchantment, Integer> enchantToRemove = storedEnchants.entrySet().iterator().next();
			meta.addStoredEnchant(enchantToRemove.getKey(), enchantToRemove.getValue(), /*ignoreLevelRestriction=*/true);
			return resultBook;
		}
		if(item0.getType() != item1.getType() && item1.getType() != Material.ENCHANTED_BOOK) return null;
		item0 = item0.clone();

		// Apply durability improvements
		if(item0.getItemMeta() instanceof Damageable && ((Damageable)item0.getItemMeta()).hasDamage()){
			short maxDurability = item0.getType().getMaxDurability();
			short durability = (short) Math.floor(maxDurability*.12);
			if(item1.getType() == item0.getType()) durability +=
					maxDurability - ((Damageable)item1.getItemMeta()).getDamage();

			((Damageable)item0.getItemMeta()).setDamage(
					durability > maxDurability? 0 : maxDurability - durability);
		}

		Map<Enchantment, Integer> maxLevels = EnchantBook.getMaxLevels(p);
		boolean conflicting = p.hasPermission("enchantbook.anvil.conflicting");
		boolean anyItem = p.hasPermission("enchantbook.anvil.anyitem");

		Map<Enchantment, Integer> appliedEnchants = item0.getEnchantments();
		Map<Enchantment, Integer> storedEnchants = item0.getItemMeta() instanceof EnchantmentStorageMeta ?
				((EnchantmentStorageMeta)item0.getItemMeta()).getStoredEnchants() : null;

		// Merge applied enchants from item 1 -> 0
		for(Entry<Enchantment, Integer> enchant : item1.getItemMeta().getEnchants().entrySet()){
			if(!conflicting && item0.getItemMeta().hasConflictingEnchant(enchant.getKey())) continue;
			if(!anyItem && enchant.getKey().canEnchantItem(item0) == false) continue;

			int currentLevel = appliedEnchants.getOrDefault(enchant.getKey(), 0);
			if(currentLevel == enchant.getValue() && currentLevel < maxLevels.get(enchant.getKey())){
				appliedEnchants.put(enchant.getKey(), currentLevel + 1);
			}
			else if(enchant.getValue() > currentLevel){
				appliedEnchants.put(enchant.getKey(), enchant.getValue());
			}
		}
		if(item1.getItemMeta() instanceof EnchantmentStorageMeta){
			Map<Enchantment, Integer> storedEnchantsToAdd = ((EnchantmentStorageMeta)item1.getItemMeta()).getStoredEnchants();
			if(item0.getItemMeta() instanceof EnchantmentStorageMeta){ // Merge stored enchants
				for(Entry<Enchantment, Integer> enchant : storedEnchantsToAdd.entrySet()){
					int currentLevel = storedEnchants.getOrDefault(enchant.getKey(), 0);
					if(currentLevel == enchant.getValue() && currentLevel < maxLevels.get(enchant.getKey())){
						storedEnchants.put(enchant.getKey(), currentLevel + 1);
					}
					else if(enchant.getValue() > currentLevel){
						storedEnchants.put(enchant.getKey(), enchant.getValue());
					}
				}
			}
			else{ // Apply stored enchants
				for(Entry<Enchantment, Integer> enchant : storedEnchantsToAdd.entrySet()){
					if(!conflicting && item0.getItemMeta().hasConflictingEnchant(enchant.getKey())) continue;
					if(!anyItem && enchant.getKey().canEnchantItem(item0) == false) continue;
	
					int currentLevel = appliedEnchants.getOrDefault(enchant.getKey(), 0);
					if(currentLevel == enchant.getValue() && currentLevel < maxLevels.get(enchant.getKey())){
						appliedEnchants.put(enchant.getKey(), currentLevel + 1);
					}
						else if(enchant.getValue() > currentLevel){
						appliedEnchants.put(enchant.getKey(), enchant.getValue());
					}
				}
			}
		}
		return item0;
	}

	private void runItemNameUpdateLoop(){
		new BukkitRunnable(){@Override public void run(){
			for(Entry<Permissible, AnvilInventory> anvilView : openAnvils.entrySet()){
				ItemStack result = anvilView.getValue().getItem(2);
				if(validItem(result) && hasName(result)){
					ItemMeta meta = result.getItemMeta();
					meta.setDisplayName(translateByPermission(meta.getDisplayName(), anvilView.getKey()));
					result.setItemMeta(meta);
					anvilView.getValue().setItem(2, result);
				}
			}
			if(openAnvils.isEmpty()) cancel();
		}}.runTaskTimer(plugin, 2, 2);
	}

	@EventHandler
	public void onAnvilClose(InventoryCloseEvent evt){
		if(evt.getInventory().getType() == InventoryType.ANVIL) openAnvils.remove(evt.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAnvilClick(final InventoryClickEvent evt){
		if(evt.isCancelled() || evt.getInventory().getType() != InventoryType.ANVIL) return;
		AnvilInventory anvil = (AnvilInventory)evt.getInventory();
		openAnvils.put(evt.getWhoClicked(), anvil);
		if(openAnvils.size() == 1) runItemNameUpdateLoop();

		int slot = evt.getRawSlot();
		ItemStack result = anvil.getItem(2);

		if(slot > 2) return; // Didn't click an anvil slot
		if(slot == 2 && !validItem(result)) return; // Clicked result but it is empty
		if(slot != 2 &&  !validItem(anvil.getItem(slot))) return; // Removed an input item

		ItemStack item0 = anvil.getItem(0), item1 = anvil.getItem(1);

		// This is (unnecessarily?) run every time the anvil is clicked ---------------------------------
		if(!validItem(item0) && evt.getRawSlot() == 0 && validItem(evt.getCursor())) item0 = evt.getCursor();
		if(!validItem(item1) && evt.getRawSlot() == 1 && validItem(evt.getCursor())) item1 = evt.getCursor();
		if(!validItem(item0)) return; // Must have a primary input
		if(validItem(item1)){
			result = anvilOutputItem(item0, item1, evt.getWhoClicked());
			if(result != null) anvil.setItem(2, result);
		}
		else if(!validItem(result)) return;
		//-----------------------------------------------------------------------------------------------

		// They are removing the result item
		if(evt.getRawSlot() == 2 && anvil.getItem(2) != null){
			// Ensure the item name is set correctly
			if(anvil.getItem(2).hasItemMeta() && anvil.getItem(2).getItemMeta().hasDisplayName()){
				String itemName = anvil.getItem(2).getItemMeta().getDisplayName();
				openAnvils.remove(evt.getWhoClicked());

				itemName = translateByPermission(itemName, evt.getWhoClicked());
				ItemMeta meta = result.getItemMeta();
				meta.setDisplayName(itemName);
				result.setItemMeta(meta);
				evt.setCurrentItem(result);
			}
			// Don't delete the input book (item0) when splitting enchantment books
			if(ALLOW_SEPARATING_ENCH_BOOKS && item1.getType() == Material.BOOK){
				Enchantment removedEnchant = ((EnchantmentStorageMeta)result.getItemMeta()).getStoredEnchants().keySet().iterator().next();
				EnchantmentStorageMeta meta0 = (EnchantmentStorageMeta)item0.getItemMeta();
				meta0.getStoredEnchants().remove(removedEnchant);
				item0.setItemMeta(meta0);

				evt.setCancelled(true);
				anvil.setItem(0, item0);
				anvil.setItem(1, null);
				evt.setCurrentItem(result);//TODO: test if this actually gives them the item, if not, try setting itemOnCursor()
			}
		}
	}
}