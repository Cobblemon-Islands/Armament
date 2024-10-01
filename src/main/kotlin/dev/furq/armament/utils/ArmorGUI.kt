package dev.furq.armament.utils

import dev.furq.armament.Armament
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ArmorGUI(private val plugin: Armament) {
    private val armorsConfig = plugin.getArmorsConfig()
    private val armors = armorsConfig.getConfigurationSection("armors")?.getKeys(false)?.toList() ?: emptyList()
    private val itemsPerPage = 45
    val guiTitle = "§9Armament Armors"

    fun openGUI(player: Player, page: Int) {
        val inventory = Bukkit.createInventory(null, 54, "$guiTitle §8| §7Page ${page + 1}")
        val startIndex = page * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, armors.size)

        for (i in startIndex until endIndex) {
            val armorName = armors[i]
            val armorItem = ArmorCreator(plugin).createArmorPiece(armorName, "chestplate")
            if (armorItem != null) {
                inventory.addItem(armorItem)
            }
        }

        if (page > 0) {
            inventory.setItem(45, createNavigationButton("Previous Page"))
        }
        if (endIndex < armors.size) {
            inventory.setItem(53, createNavigationButton("Next Page"))
        }

        player.openInventory(inventory)
    }

    private fun createNavigationButton(name: String): ItemStack {
        return ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName(ChatColor.YELLOW.toString() + name)
            }
        }
    }

    fun getMaxPages(): Int {
        return (armors.size - 1) / itemsPerPage + 1
    }
}