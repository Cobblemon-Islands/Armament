package dev.furq.armament.utils

import dev.furq.armament.Armament
import org.bukkit.Material

class MaterialGetter(val plugin: Armament) {
    fun getArmorMaterial(): List<Material> {
        val armorMaterial = plugin.config.getString("armor_material")
        val validMaterials = listOf("CHAINMAIL", "IRON", "GOLD", "DIAMOND", "NETHERITE")

        return if (armorMaterial != null && validMaterials.contains(armorMaterial.uppercase())) {
            listOf(
                Material.valueOf("${armorMaterial.uppercase()}_HELMET"),
                Material.valueOf("${armorMaterial.uppercase()}_CHESTPLATE"),
                Material.valueOf("${armorMaterial.uppercase()}_LEGGINGS"),
                Material.valueOf("${armorMaterial.uppercase()}_BOOTS")
            )
        } else {
            plugin.logger.severe("Invalid armor material specified in config.yml. Please use CHAINMAIL, DIAMOND, GOLD, or NETHERITE.")
            throw IllegalArgumentException("Invalid armor material")
        }
    }

    fun getArmorString(): String {
        val armorMaterial = plugin.config.getString("armor_material")
        val validMaterials = listOf("CHAINMAIL", "DIAMOND", "GOLD", "NETHERITE")

        return if (armorMaterial != null && validMaterials.contains(armorMaterial.uppercase())) {
            armorMaterial.uppercase()
        } else {
            plugin.logger.severe("Invalid armor material specified in config.yml. Please use CHAINMAIL, DIAMOND, GOLD, or NETHERITE.")
            throw IllegalArgumentException("Invalid armor material")
        }
    }
}