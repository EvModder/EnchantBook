package net.evmodder.EnchantBook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class EnchantAPI{
	public static Collection<Enchantment> parseEnchantList(String str){
		str = str.toUpperCase();
		if(str.equals("ALL") || str.startsWith("@A")) return Arrays.asList(Enchantment.values());

		ArrayList<Enchantment> enchants = new ArrayList<Enchantment>();
		for(String s : str.split(",")){
			Enchantment ench = EnchantBook.parseEnchant(s);
			if(ench != null) enchants.add(ench);
		}
		return enchants;
	}

	public static int countItem(Inventory inv, Material type){
		int numItems = 0;
		for(ItemStack item : inv.getContents()){
			if(item != null && item.getType() == type) numItems += item.getAmount();
		}
		return numItems;
	}

	public static ItemStack[] splitBook(ItemStack book){
		if(book == null || book.getType() != Material.ENCHANTED_BOOK) return new ItemStack[]{book};
		EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta)book.getItemMeta();
		if(bookMeta.getStoredEnchants().size() <= 1) return new ItemStack[]{book};

		ItemStack[] resultBooks = new ItemStack[bookMeta.getStoredEnchants().size()];
		int i = 0;
		for(Entry<Enchantment, Integer> enchant : bookMeta.getStoredEnchants().entrySet()){
			ItemStack newBook = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta newMeta = (EnchantmentStorageMeta)newBook.getItemMeta();
			newMeta.addStoredEnchant(enchant.getKey(), enchant.getValue(), true);
			newBook.setItemMeta(newMeta);
			resultBooks[i++] = newBook;
		}
		return resultBooks;
	}

	public static boolean splitBooks(Inventory inv){
		ArrayList<ItemStack> resultBooks = new ArrayList<ItemStack>();

		int openSpaces = 0;
		for(int i = 0; i < inv.getSize(); ++i){
			if(inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) ++openSpaces;
			else if(inv.getItem(i).getType() == Material.ENCHANTED_BOOK){
				resultBooks.addAll(Arrays.asList(splitBook(inv.getItem(i))));
				inv.setItem(i, null);
				++openSpaces;
			}
		}
		if(resultBooks.size() > openSpaces) return false;
		inv.addItem(resultBooks.toArray(new ItemStack[]{}));
		return true;
	}

	public static ItemStack[] combineBooks(ItemStack[] books, Map<Enchantment, Integer> maxLevels){
		// Only combine books with the same enchantment set
		HashSet<Map<Enchantment, Integer>> mergeBooks = new HashSet<Map<Enchantment, Integer>>();
		ArrayList<Map<Enchantment, Integer>> maxBooks = new ArrayList<Map<Enchantment, Integer>>();

		for(ItemStack item : books){
			if(item != null && item.getType() == Material.ENCHANTED_BOOK){
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)item.getItemMeta();
				boolean maxLevel = false;
				for(Entry<Enchantment, Integer> enchant : bookmeta.getStoredEnchants().entrySet()){
					if(enchant.getValue() >= maxLevels.get(enchant.getKey())){
						maxLevel = true;
						break;
					}
				}
				while(mergeBooks.remove(bookmeta.getStoredEnchants())){
					for(Entry<Enchantment, Integer> enchant : bookmeta.getStoredEnchants().entrySet()){
						enchant.setValue(enchant.getValue() + 1);
						if(enchant.getValue() >= maxLevels.get(enchant.getKey())) maxLevel = true;
					}
				}
				if(maxLevel) maxBooks.add(bookmeta.getStoredEnchants());
				else mergeBooks.add(bookmeta.getStoredEnchants());
			}
		}
		ItemStack[] resultBooks = new ItemStack[mergeBooks.size() + maxBooks.size()];
		int i = 0;
		for(Map<Enchantment, Integer> enchants : mergeBooks){
			ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)item.getItemMeta();
			bookmeta.getStoredEnchants().putAll(enchants);
			item.setItemMeta(bookmeta);
			resultBooks[i++] = item;
		}
		for(Map<Enchantment, Integer> enchants : maxBooks){
			ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)item.getItemMeta();
			bookmeta.getStoredEnchants().putAll(enchants);
			item.setItemMeta(bookmeta);
			resultBooks[i++] = item;
		}
		return resultBooks;
	}
}