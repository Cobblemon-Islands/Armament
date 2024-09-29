package dev.furq.armament.utils

import dev.furq.armament.Armament
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.persistence.PersistentDataType
import org.bukkit.attribute.Attribute
import java.io.File

class InventoryUpdater(private val plugin: Armament) {

    private val armorsConfigFile = File(plugin.dataFolder, "armors.yml")
    private val armorsConfig = YamlConfiguration.loadConfiguration(armorsConfigFile)
    private val armorCreator = ArmorCreator(plugin)

    private val runningShoesMapping = mapOf(
        0.2 to "maletrainer",
        0.4 to "maletrainer",
        0.7 to "maletrainer"
    )

    fun updatePlayerInventory(inventory: PlayerInventory) {
        inventory.contents.filterNotNull().forEachIndexed { index, item ->
            if (isCustomArmor(item)) {
                updateItemIfNeeded(item, inventory, index)
            }
        }
    }

    private fun replaceOraxenArmor(item: ItemStack?, inventory: PlayerInventory, index: Int) {
        val pdc = item?.itemMeta?.persistentDataContainer ?: return
        val oraxenId = NamespacedKey.fromString("oraxen:id")?.let { pdc.get(it, PersistentDataType.STRING) } ?: return
        
        val parts = oraxenId.split("_")
        val oraxenPiece = parts.last()
        val armorName = parts.dropLast(1).joinToString("")
        
        val newItem = armorCreator.createArmorPiece(armorName, oraxenPiece)
        if (newItem != null) {
            newItem.addUnsafeEnchantments(item.enchantments)
            inventory.setItem(index, newItem)
        }
    }

    fun updateItemIfNeeded(item: ItemStack?, inventory: PlayerInventory, index: Int) {
        if (isOraxenArmor(item)) {
            replaceOraxenArmor(item, inventory, index)
            return
        }
        if (isRunningShoes(item) && !isCustomArmor(item!!)) {
            replaceRunningShoes(item, inventory, index)
            return
        }
        val itemMeta = item?.itemMeta ?: return
        val pdc = itemMeta.persistentDataContainer
        val armorID = pdc.get(NamespacedKey(plugin, "armor"), PersistentDataType.STRING) ?: return
        val armorConfig = armorsConfig.getConfigurationSection("armors.$armorID") ?: return
        val piece = when {
            item.type.name.endsWith("_HELMET") -> "helmet"
            item.type.name.endsWith("_CHESTPLATE") -> "chestplate"
            item.type.name.endsWith("_LEGGINGS") -> "leggings"
            item.type.name.endsWith("_BOOTS") -> "boots"
            else -> return
        }
        val customModelData = armorConfig.getInt("custom_model_data")
        val displayName = armorConfig.getString("$piece.name")?.let { ChatColor.translateAlternateColorCodes('&', it) }
        val lore = armorConfig.getStringList("$piece.lore").map { ChatColor.translateAlternateColorCodes('&', it) }

        var updateNeeded = false

        val materialGetter = MaterialGetter(plugin)
        if (item.type !in materialGetter.getArmorMaterial()) {
            val material =
                materialGetter.getArmorMaterial().firstOrNull { it.name.contains(item.type.name.split("_").last()) }
            if (material != null) {
                item.type = material
                updateNeeded = true
            }
        }

        if (displayName != itemMeta.displayName) {
            itemMeta.setDisplayName(displayName)
            updateNeeded = true
        }

        if (customModelData != itemMeta.customModelData) {
            itemMeta.setCustomModelData(customModelData)
            updateNeeded = true
        }

        if (lore != itemMeta.lore) {
            itemMeta.lore = lore
            updateNeeded = true
        }

        if (updateNeeded) {
            item.itemMeta = itemMeta
            inventory.setItem(index, item)
        }
    }

    private fun isCustomArmor(item: ItemStack): Boolean {
        val pdc = item.itemMeta?.persistentDataContainer
        return pdc?.has(NamespacedKey(plugin, "armor"), PersistentDataType.STRING) == true
    }

    private fun isOraxenArmor(item: ItemStack?): Boolean {
        val pdc = item?.itemMeta?.persistentDataContainer
        val oraxenId = NamespacedKey.fromString("oraxen:id")?.let { pdc?.get(it, PersistentDataType.STRING) }
        return oraxenId != null && oraxenId.contains("_")
    }

    private fun isRunningShoes(item: ItemStack?): Boolean {
        val itemMeta = item?.itemMeta ?: return false
        if (item.type != Material.LEATHER_BOOTS) return false

        val attributeModifiers = itemMeta.attributeModifiers ?: return false
        return attributeModifiers[Attribute.GENERIC_MOVEMENT_SPEED].any { modifier ->
            runningShoesMapping.containsKey(modifier.amount)
        }
    }

    private fun replaceRunningShoes(item: ItemStack?, inventory: PlayerInventory, index: Int) {
        val itemMeta = item?.itemMeta ?: return
        val speedModifier = itemMeta.attributeModifiers?.get(Attribute.GENERIC_MOVEMENT_SPEED)
            ?.firstOrNull { modifier ->
                runningShoesMapping.containsKey(modifier.amount)
            } ?: return

        val newArmorName = runningShoesMapping[speedModifier.amount] ?: return
        val newItem = armorCreator.createArmorPiece(newArmorName, "boots") ?: return
        val newItemMeta = newItem.itemMeta ?: return

        newItemMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, speedModifier)
        newItemMeta.setDisplayName("&d${ChatColor.stripColor(itemMeta.displayName)}")
        newItem.addUnsafeEnchantments(item.enchantments)
        newItem.itemMeta = newItemMeta
        inventory.setItem(index, newItem)
    }
}