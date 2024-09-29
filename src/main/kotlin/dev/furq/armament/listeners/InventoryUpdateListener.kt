package dev.furq.armament.listeners

import dev.furq.armament.Armament
import dev.furq.armament.utils.InventoryUpdater
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent

class InventoryUpdateListener(private val plugin: Armament) : Listener {

    @EventHandler
    fun PlayerJoinEvent.onPlayerJoin() {
        InventoryUpdater(plugin).updatePlayerInventory(player.inventory)
    }

    @EventHandler
    fun PlayerItemHeldEvent.onItemHeld() {
        val item = player.inventory.getItem(newSlot)
        InventoryUpdater(plugin).updateItem(item, player.inventory, newSlot)
    }
}