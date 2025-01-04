package com.mcpvp.common.item;

import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NBTUtil {
	
	public static boolean hasString(ItemStack item, String key, String value) {
		if (item == null)
			return false;
		
		net.minecraft.server.v1_8_R3.ItemStack nmsCopy = CraftItemStack.asNMSCopy(item);
		NBTTagCompound nbt = getCompound(nmsCopy);
		return nbt.hasKey(key) && nbt.getString(key).equals(value);
	}
	
	public static org.bukkit.inventory.ItemStack saveString(org.bukkit.inventory.ItemStack itemStack, String key, String value) {
		if (itemStack == null || CraftItemStack.asNMSCopy(itemStack) == null)
			return itemStack;
		
		net.minecraft.server.v1_8_R3.ItemStack nmsItem = saveStringNMS(CraftItemStack.asNMSCopy(itemStack), key, value);
		return CraftItemStack.asCraftMirror(nmsItem);
	}
	
	public static net.minecraft.server.v1_8_R3.ItemStack saveStringNMS(net.minecraft.server.v1_8_R3.ItemStack itemStack, String key, String value) {
		NBTTagCompound nbt = getCompound(itemStack);
		if (!nbt.hasKey(key))
			nbt.setString(key, value);
		else {
			String prevValue = nbt.getString(key);
			if (prevValue.contains(value))
				return itemStack;
			
			nbt.setString(key, value);
		}
		itemStack.setTag(nbt);
		return itemStack;
	}
	
	private static NBTTagCompound getCompound(net.minecraft.server.v1_8_R3.ItemStack itemStack) {
		NBTTagCompound nbt = itemStack.getTag();
		if (nbt == null) {
			itemStack.setTag(new NBTTagCompound());
			nbt = itemStack.getTag();
		}
		return nbt;
	}
	
}
