package com.mcpvp.common.item;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
	
	public static List<String> getLore(ItemStack stack) {
		if (stack == null)
			return null;
		if (stack.getItemMeta() == null)
			return null;
		return stack.getItemMeta().getLore();
	}
	
	public static ItemStack setDescription(ItemStack stack, List<String> description) {
		if (stack == null)
			return stack;
		if (description == null)
			return stack;
		ItemMeta meta = stack.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		for (String line : description)
			if (line != null) // && line.length() != 0)
				lore.add(ChatColor.RESET + line);
		if (lore.size() != 0)
			meta.setLore(lore);
		stack.setItemMeta(meta);
		return stack;
	}
	
	/**
	 * Wraps the given String, but avoids cutting color characters off.
	 * @param string The String to wrap.
	 * @param lineLength The length of each line.
	 * @return A list of wrapped text.
	 */
	public static List<String> wrapWithColor(String string, int lineLength) {
		int length = translateLength(string, lineLength);
		List<String> lines;
		if (length == string.length()) {
			lines = new ArrayList<>();
			lines.add(string);
		} else {
			int lastSpace = string.lastIndexOf(' ', length);
			length = lastSpace == -1 ? length : lastSpace + 1;
			String line = string.substring(0, length).trim();
			lines = wrapWithColor(ChatColor.getLastColors(line) + string.substring(length).trim(), lineLength);
			lines.add(0, line);
		}
		return lines;
	}
	
	/**
	 * @deprecated Unknown what the purpose of this is. It would be just as easy
	 * to strip color codes and find the length (?)
	 */
	@Deprecated
	public static int translateLength(String string, int length) {
		int nonColorCharCount = 0;
		boolean previousWasColorChar = false;
		for (int i = 0; i < string.length(); i++)
			if (previousWasColorChar)
				previousWasColorChar = false;
			else if (string.charAt(i) == ChatColor.COLOR_CHAR)
				previousWasColorChar = true;
			else {
				nonColorCharCount++;
				if (nonColorCharCount == length)
					return i + 1;
			}
		return string.length();
	}

	
}
