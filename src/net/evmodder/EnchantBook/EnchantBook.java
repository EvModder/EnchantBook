package net.evmodder.EnchantBook;

import java.io.InputStream;
import java.util.HashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.permissions.Permissible;
import net.evmodder.EvLib.EvPlugin;
import net.evmodder.EvLib.FileIO;

public final class EnchantBook extends EvPlugin{
	//TODO: On anvil: Enchantbook with multiple enchants + empty book = remove 1st enchant and put on empty book
	private static EnchantBook plugin; public static EnchantBook getPlugin(){return plugin;}
	static HashMap<String, Enchantment> enchantLookupMap;
	static HashMap<Enchantment, Integer> maxLevelConfig;
	static final int MAX_ENCHANT_LEVEL = 32767;

	@Override public void onEvEnable(){
		plugin = this;
		enchantLookupMap = new HashMap<String, Enchantment>();
		maxLevelConfig = new HashMap<Enchantment, Integer>();

		InputStream defaultNames = getClass().getResourceAsStream("/enchant-names.yml");
		YamlConfiguration enchAliases = FileIO.loadConfig(this, "enchant-names.yml", defaultNames);
		for(String key : enchAliases.getKeys(false)){
			String[] aliases = enchAliases.getString(key).toLowerCase().replaceAll("_", "").split(",");
			Enchantment ench = parseEnchant(key);
			if(ench == null) getLogger().severe("Unknown enchantment: " + key);
			else{
				enchantLookupMap.put(key.toLowerCase().replaceAll("_", ""), ench);
				for(String s : aliases) enchantLookupMap.put(s, ench);
			}
		}
		if(config.contains("max-levels")){
			ConfigurationSection maxLevels = config.getConfigurationSection("max-levels");
			for(String enchName : maxLevels.getKeys(false)){
				Enchantment ench = enchantLookupMap.get(enchName.replaceAll("_", "").toLowerCase());
				if(ench == null) getLogger().severe("Unknown enchantment: " + enchName);
				else{
					maxLevelConfig.put(ench, maxLevels.getInt(enchName));
					maxLevelMins.put(ench, Math.min(ench.getMaxLevel(), maxLevels.getInt(enchName)));
				}
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
		Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(str.toLowerCase()));
		if(ench == null) ench = Enchantment.getByName(str.toUpperCase());
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
				return maxLevelConfig.get(ench);
			case VANILLA:
			default:
				return ench.getMaxLevel();
		}
	}
	static HashMap<Enchantment, Integer> maxLevelSupported = new HashMap<Enchantment, Integer>();
	static HashMap<Enchantment, Integer> maxLevelVanilla = new HashMap<Enchantment, Integer>();
	static HashMap<Enchantment, Integer> maxLevelMins = new HashMap<Enchantment, Integer>();
	static{
		for(Enchantment enchant : Enchantment.values()){
			maxLevelSupported.put(enchant, MAX_ENCHANT_LEVEL);
			maxLevelVanilla.put(enchant, enchant.getMaxLevel());
		}
	}
	static HashMap<Enchantment, Integer> getMaxlevels(LimitType limit){
		switch(limit){
			case GAME:
				return maxLevelSupported;
			case CONFIG:
				return maxLevelConfig;
			case VANILLA:
			default:
				return maxLevelVanilla;
		}
	}
	static HashMap<Enchantment, Integer> getMaxLevels(Permissible p){
		boolean aboveConfig = p.hasPermission("enchantbook.aboveconfig");
		boolean aboveNatural = p.hasPermission("enchantbook.abovenatural");
		if(aboveConfig && aboveNatural){
			return maxLevelSupported;
		}
		else if(aboveNatural){
			return maxLevelConfig;
		}
		else if(aboveConfig){
			return maxLevelVanilla;
		}
		else{
			return maxLevelMins;
		}
	}
}