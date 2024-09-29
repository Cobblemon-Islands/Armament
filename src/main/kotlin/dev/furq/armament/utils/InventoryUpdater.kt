package dev.furq.armament.utils

import dev.furq.armament.Armament
import org.bukkit.ChatColor
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
    private val runningShoesMapping = listOf(0.2, 0.4, 0.7)
    private val materialGetter = MaterialGetter(plugin)
    private val armorMaterials = materialGetter.getArmorMaterial()

    fun updatePlayerInventory(inventory: PlayerInventory) {
        inventory.contents.filterNotNull().forEachIndexed { index, item ->
            updateItem(item, inventory, index)
        }
    }

    fun updateItem(item: ItemStack?, inventory: PlayerInventory, index: Int) {
        when {
            isCustomArmor(item) -> updateItemIfNeeded(item, inventory, index)
            isOraxenArmor(item) -> replaceOraxenArmor(item, inventory, index)
            isRunningShoes(item) -> replaceRunningShoes(item, inventory, index)
        }
    }

    private fun replaceOraxenArmor(item: ItemStack?, inventory: PlayerInventory, index: Int) {
        val oraxenId = item?.itemMeta?.persistentDataContainer
            ?.get(NamespacedKey.fromString("oraxen:id")!!, PersistentDataType.STRING) ?: return

        val (armorName, oraxenPiece) = oraxenId.split("_").let { it.dropLast(1).joinToString("") to it.last() }

        armorCreator.createArmorPiece(armorName, oraxenPiece)?.let { newItem ->
            newItem.addUnsafeEnchantments(item.enchantments)
            inventory.setItem(index, newItem)
        }
    }

    private fun updateItemIfNeeded(item: ItemStack?, inventory: PlayerInventory, index: Int) {
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

        if (item.type !in armorMaterials) {
            val material = armorMaterials.firstOrNull { it.name.contains(item.type.name.split("_").last()) }
            if (material != null) {
                item.type = material
                updateNeeded = true
            }
        }

        if (displayName != itemMeta.displayName && !isRunningShoes(item)) {
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

    private fun isCustomArmor(item: ItemStack?): Boolean {
        val pdc = item?.itemMeta?.persistentDataContainer
        return pdc?.has(NamespacedKey(plugin, "armor"), PersistentDataType.STRING) == true
    }

    private fun isOraxenArmor(item: ItemStack?): Boolean {
        val pdc = item?.itemMeta?.persistentDataContainer
        val oraxenId = NamespacedKey.fromString("oraxen:id")?.let { pdc?.get(it, PersistentDataType.STRING) }
        return oraxenId != null && oraxenId.contains("_")
    }

    private fun isRunningShoes(item: ItemStack?): Boolean {
        val itemMeta = item?.itemMeta ?: return false
        val attributeModifiers = itemMeta.attributeModifiers ?: return false
        return attributeModifiers[Attribute.GENERIC_MOVEMENT_SPEED].any { modifier ->
            runningShoesMapping.contains(modifier.amount)
        }
    }

    private fun replaceRunningShoes(item: ItemStack?, inventory: PlayerInventory, index: Int) {
        val itemMeta = item?.itemMeta ?: return
        val speedModifier = itemMeta.attributeModifiers?.get(Attribute.GENERIC_MOVEMENT_SPEED)
            ?.firstOrNull { modifier ->
                runningShoesMapping.contains(modifier.amount)
            } ?: return

        val newItem = armorCreator.createArmorPiece("maletrainer", "boots") ?: return
        val newItemMeta = newItem.itemMeta ?: return

        newItemMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, speedModifier)
        newItemMeta.setDisplayName(itemMeta.displayName)
        newItem.addUnsafeEnchantments(item.enchantments)
        newItem.itemMeta = newItemMeta
        inventory.setItem(index, newItem)
    }
}