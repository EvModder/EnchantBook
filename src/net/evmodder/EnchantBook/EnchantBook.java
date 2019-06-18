package net.evmodder.EnchantBook;

import java.io.InputStream;
import java.util.HashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import net.evmodder.EvLib.EvPlugin;
import net.evmodder.EvLib.FileIO;

public final class EnchantBook extends EvPlugin{
	private static EnchantBook plugin; public static EnchantBook getPlugin(){return plugin;}
	static HashMap<String, Enchantment> enchantLookupMap;
	static HashMap<Enchantment, Integer> maxLevelLookupMap;
	static final int MAX_ENCHANT_LEVEL = 32767;

	@Override public void onEvEnable(){
		plugin = this;
		enchantLookupMap = new HashMap<String, Enchantment>();
		maxLevelLookupMap = new HashMap<Enchantment, Integer>();

		InputStream defaultNames = getClass().getResourceAsStream("/enchant-names.yml");
		YamlConfiguration enchAliases = FileIO.loadConfig(this, "enchant-names.yml", defaultNames);
		for(String key : enchAliases.getKeys(false)){
			String[] aliases = enchAliases.getString(key).toLowerCase().replaceAll("_", "").split(",");
			@SuppressWarnings("deprecation")
			Enchantment ench = Enchantment.getByName(key);
			enchantLookupMap.put(key.toLowerCase().replaceAll("_", ""), ench);
			for(String s : aliases) enchantLookupMap.put(s, ench);
		}
		if(config.contains("max-levels")){
			ConfigurationSection maxLevels = config.getConfigurationSection("max-levels");
			for(String enchName : maxLevels.getKeys(false)){
				Enchantment ench = enchantLookupMap.get(enchName);
				if(ench == null) getLogger().severe("Unknown enchantment: " + enchName);
				maxLevelLookupMap.put(ench, maxLevels.getInt(enchName));
			}
		}

		if(config.getBoolean("enable-anvil-features", true))
			getServer().getPluginManager().registerEvents(new AnvilListener(), this);
		if(config.getBoolean("old-enchanting-behavior", false))
			getServer().getPluginManager().registerEvents(new ListenerLapisEnchant(), this);

		new CommandEnchant(this);
		new CommandCombineBooks(this);
		new CommandSeparateBooks(this);
	}

	@SuppressWarnings("deprecation")
	public static Enchantment parseEnchant(String str){
		Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(str));
		if(ench == null) ench = Enchantment.getByName(str);
		if(ench == null && enchantLookupMap != null)
			ench = enchantLookupMap.get(str.toLowerCase().replaceAll("_", ""));
		return ench;
	}

	enum LimitType{ VANILLA, CONFIG, GAME }
	public static int getMaxLevel(Enchantment ench, LimitType limit){
		switch(limit){
			case GAME:
				return MAX_ENCHANT_LEVEL;
			case CONFIG:
				return maxLevelLookupMap.get(ench);
			case VANILLA:
			default:
				return ench.getMaxLevel();
		}
	}
	static HashMap<Enchantment, Integer> maxSupported = new HashMap<Enchantment, Integer>();
	static HashMap<Enchantment, Integer> maxVanilla = new HashMap<Enchantment, Integer>();
	static{
		for(Enchantment enchant : Enchantment.values()){
			maxSupported.put(enchant, MAX_ENCHANT_LEVEL);
			maxVanilla.put(enchant, enchant.getMaxLevel());
		}
	}
	static HashMap<Enchantment, Integer> getMaxlevels(LimitType limit){
		
		switch(limit){
			case GAME:
				return maxSupported;
			case CONFIG:
				return maxLevelLookupMap;
			case VANILLA:
			default:
				return maxVanilla;
		}
	}
}