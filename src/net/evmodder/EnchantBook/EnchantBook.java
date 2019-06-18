package net.evmodder.EnchantBook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.permissions.Permissible;
import net.evmodder.EvLib.EvPlugin;

public final class EnchantBook extends EvPlugin{
	YamlConfiguration easyNames;
	HashMap<String, Enchantment> enchants = new HashMap<String, Enchantment>();
	final int ENCH_MAX_LVL = 32767;

	@Override public void onEvEnable(){
		InputStream defaultNames = getClass().getResourceAsStream("/default names.yml");
		easyNames = FileIO.loadConfig(this, "enchants.yml", defaultNames);
		for(String key : easyNames.getKeys(false)){
			//int id = Integer.parseInt(key);
			String[] names = easyNames.getString(key).split(",");
			@SuppressWarnings("deprecation")
			Enchantment ench = Enchantment.getByName(names[0]);
			for(String s : names) enchants.put(s.toUpperCase(), ench);
		}

		if(config.getBoolean("old-enchanting-behavior", false))
			getServer().getPluginManager().registerEvents(new ListenerLapisEnchant(this), this);
		//if(config.getBoolean("lottery-hopper", true))//TODO: fix
		//	getServer().getPluginManager().registerEvents(new ListenerLottoHopper(this), this);
		if(config.getBoolean("modified-anvil", true))
			getServer().getPluginManager().registerEvents(new AnvilListener(this), this);

		for(Enchantment ench : Enchantment.values()){
			if(!config.contains("max-levels."+ench.getName().toLowerCase())){
				// Write new enchantment to config (without changing or clearing existing content)
				InputStream defaultConfig = getClass().getResourceAsStream("/config.yml");
				String currentConfig = FileIO.loadFile("config-"+getName()+".yml", defaultConfig);
				String newEnch = ench.getName().toLowerCase()+": "+ench.getMaxLevel();
				String updatedConfig = currentConfig.replace("max-levels:", "max-levels:\n  "+newEnch);
				FileIO.saveFile("config-"+getName()+".yml", updatedConfig);
			}
		}

		new CommandEnchant(this);
		new CommandCombineBooks(this);
		new CommandSeparateBooks(this);
	}
	@Override public void onEvDisable(){/*  */}

	enum LimitType{VANILLA, CONFIG, MAX}
	int maxEnchValue(Enchantment ench, LimitType limit){
		return limit == LimitType.VANILLA ? ench.getMaxLevel()
			: limit == LimitType.CONFIG ? config.getInt("max-levels."+ench.getName().toLowerCase(), ench.getMaxLevel())
			: ENCH_MAX_LVL;
	}

	@SuppressWarnings("deprecation")
	public Collection<Enchantment> getEnchantments(String str){
		str = str.toUpperCase();
		if(str.equals("ALL") || str.startsWith("@A")) return Arrays.asList(Enchantment.values());

		ArrayList<Enchantment> enchs = new ArrayList<Enchantment>();
		for(String s : str.split(",")){
			if(Enchantment.getByName(s) != null) enchs.add(Enchantment.getByName(s));
			else if(enchants.containsKey(s)) enchs.add(enchants.get(s));
			/*else if(StringUtils.isNumeric(s)){
				try{enchs.add(Enchantment.getById(Integer.parseInt(s)));}
				catch(Exception e){e.printStackTrace();}
			}
			else{
				// error: invalid enchant
			}*/
		}
		return enchs;
	}

	ItemStack[] split(ItemStack[] inv){
		Vector<ItemStack> stuff = new Vector<ItemStack>();
		for(ItemStack item : inv) stuff.add(item);

		StringBuilder names = new StringBuilder();
		StringBuilder values = new StringBuilder();

		for(int i=0; i<stuff.size(); ++i){
			if(stuff.get(i) != null && stuff.get(i).getType() == Material.ENCHANTED_BOOK){
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)stuff.get(i).getItemMeta();

				for(Enchantment enchant : bookmeta.getStoredEnchants().keySet()){
					names.append(enchant.getName()); names.append(',');
					values.append(Integer.toString(bookmeta.getStoredEnchantLevel(enchant))); values.append(',');

					stuff.set(i, null);
				}
			}
		}
		while(stuff.contains(null))stuff.remove(null);

		if(names.length() == 0) return inv;

		String[] nameString = names.substring(0, names.length()-1).split(",");
		String[] valueString = values.substring(0, values.length()-1).split(",");

		for(int i=0; i<nameString.length; ++i){
			ItemStack newbook = new ItemStack(Material.ENCHANTED_BOOK);
			EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)newbook.getItemMeta();
			bookmeta.addStoredEnchant(
					Enchantment.getByName(nameString[i]),
					Integer.parseInt(valueString[i]),
					true);
			newbook.setItemMeta(bookmeta);
			stuff.add(newbook);
		}

		return stuff.toArray(new ItemStack[]{});
	}

	ItemStack[] combine(ItemStack[] inv, Permissible perms){
		Vector<ItemStack> stuff = new Vector<ItemStack>();
		for(ItemStack item : inv) stuff.add(item);

		StringBuilder enchNames = new StringBuilder(), enchValues = new StringBuilder();

		for(int i=0; i<stuff.size(); ++i){
			if(stuff.get(i) != null && stuff.get(i).getType() == Material.ENCHANTED_BOOK){
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)stuff.get(i).getItemMeta();

				for(Enchantment enchant : bookmeta.getStoredEnchants().keySet()){
					enchNames.append(enchant.getName()).append(',');
					enchValues.append(bookmeta.getStoredEnchantLevel(enchant)).append(',');
					stuff.set(i, null);
				}
			}
		}
		while(stuff.contains(null))stuff.remove(null);

		if(enchNames.length() == 0) return inv;

		String[] nameString = enchNames.substring(0, enchNames.length()-1).split(",");
		String[] valueString = enchValues.substring(0, enchValues.length()-1).split(",");
		Enchantment[] enchants = new Enchantment[nameString.length];
		int[] values = new int[enchants.length];

		for(int i=0; i<enchants.length; ++i){
			enchants[i] = Enchantment.getByName(nameString[i]);
			values[i] = Integer.parseInt(valueString[i]);
		}
		boolean matches = true;
		while(matches){
			matches = false;
			//-1 is treated as 'null'
			for(int i=0; i<enchants.length; ++i){

				if(enchants[i] != null)
				for(int x=0; x<enchants.length; ++x){
					if(enchants[x] != null)
						if(x != i && enchants[i].getName().equals(enchants[x].getName()) && values[i] == values[x]){

						if(!perms.hasPermission("evp.evchant.combine.unrestricted") && (
							(!perms.hasPermission("evp.evchant.combine.abovenatural")
									&& values[i]+1 > enchants[i].getMaxLevel()) ||
							(!perms.hasPermission("evp.evchant.combine.aboveconfig")
									&& values[i]+1 > maxEnchValue(enchants[i], LimitType.CONFIG))));
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
			if(enchants[i] != null){
				ItemStack newbook = new ItemStack(Material.ENCHANTED_BOOK);
				EnchantmentStorageMeta bookmeta = (EnchantmentStorageMeta)newbook.getItemMeta();
				bookmeta.addStoredEnchant(enchants[i], values[i], true);
				newbook.setItemMeta(bookmeta);
				stuff.add(newbook);
			}
		}
		return stuff.toArray(new ItemStack[]{});
	}
}