package dev.furq.armament.listeners

import dev.furq.armament.Armament
import dev.furq.armament.utils.ArmorCreator
import dev.furq.armament.utils.ArmorGUI
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.persistence.PersistentDataType

class GUIListener(private val plugin: Armament) : Listener {
    private val armorGUI = ArmorGUI(plugin)
    private val armorCreator = ArmorCreator(plugin)

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (!event.view.title.startsWith(armorGUI.guiTitle)) return
        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return
        val currentPage = event.view.title.substringAfterLast("Page ").toIntOrNull()?.minus(1) ?: return

        when (event.rawSlot) {
            45 -> if (currentPage > 0) armorGUI.openGUI(player, currentPage - 1)
            53 -> if (currentPage + 1 < armorGUI.getMaxPages()) armorGUI.openGUI(player, currentPage + 1)
            in 0 until 45 -> {
                val clickedItem = event.currentItem ?: return
                val armorName = clickedItem.itemMeta?.persistentDataContainer
                    ?.get(NamespacedKey(plugin, "armor"), PersistentDataType.STRING) ?: return
                giveFullArmorSet(player, armorName)
            }
        }
    }

    private fun giveFullArmorSet(player: Player, armorName: String) {
        val armorItems = armorCreator.createFullArmorSet(armorName)

        armorItems.forEach {
            player.inventory.addItem(it)
        }

        player.sendMessage(
            "${plugin.getMessage("prefix")} ${
                plugin.getMessage("armorset-received").replace("{armorName}", armorName)
            }"
        )
    }
}