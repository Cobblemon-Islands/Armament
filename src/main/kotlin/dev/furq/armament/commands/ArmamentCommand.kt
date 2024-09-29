package dev.furq.armament.commands

import dev.furq.armament.Armament
import dev.furq.armament.utils.ArmorGUI
import dev.furq.armament.utils.ArmorCreator
import dev.furq.armament.utils.ResourcePackGenerator
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.*

class ArmamentCommand(private val plugin: Armament) : CommandExecutor {

    private val prefix = plugin.getMessage("prefix")
    private val armorCreator = ArmorCreator(plugin)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.label.equals("armament", ignoreCase = true) && args.isNotEmpty()) {
            when (args[0].lowercase()) {
                "reload" -> handleReload(sender)
                "give" -> handleGive(sender, args)
                "giveset" -> handleGiveSet(sender, args)
                "gui" -> handleGUI(sender)
                "giveshoes" -> handleGiveShoes(sender, args)
                else -> sender.sendMessage("$prefix ${plugin.getMessage("command-unknown")}")
            }
        }
        return true
    }

    private fun handleReload(sender: CommandSender) {
        plugin.reloadConfig()
        plugin.reloadArmorsConfig()
        val sourceFolder = File(plugin.dataFolder, "source_files")
        val targetFolder = File(plugin.dataFolder, "resource_pack")
        if (!sourceFolder.exists()) sourceFolder.mkdirs()
        if (!targetFolder.exists()) targetFolder.mkdirs()
        ResourcePackGenerator(plugin).generateResourcePack(sourceFolder, targetFolder)
        sender.sendMessage("$prefix §7Reloaded Armament successfully!")
    }

    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (args.size < 3) {
            sender.sendMessage("$prefix §7Usage: /armament give <armorName> <armorPiece> [player]")
            return
        }
        val armorName = args[1]
        val armorPiece = args[2]
        val targetPlayer = if (args.size >= 4) Bukkit.getPlayer(args[3]) else sender as? Player

        val armorsConfig = plugin.getArmorsConfig()
        if (armorName !in armorsConfig.getConfigurationSection("armors")?.getKeys(false).orEmpty()) {
            sender.sendMessage("$prefix ${plugin.getMessage("armor-not-found")}")
            return
        }

        val armorItem = armorCreator.createArmorPiece(armorName, armorPiece) ?: return

        if (targetPlayer != null) {
            if (targetPlayer.inventory.firstEmpty() == -1) {
                targetPlayer.world.dropItemNaturally(targetPlayer.location, armorItem)
            } else {
                targetPlayer.inventory.addItem(armorItem)
            }
            sender.sendMessage(
                "$prefix ${
                    plugin.getMessage("armor-given").replace("{player}", targetPlayer.name)
                        .replace("{armorName}", armorName)
                }"
            )
            if (sender != targetPlayer) {
                targetPlayer.sendMessage(
                    "$prefix ${
                        plugin.getMessage("armor-received").replace("{armorName}", armorName)
                    }"
                )
            }
        } else {
            sender.sendMessage("$prefix ${plugin.getMessage("player-not-found")}")
        }
    }

    private fun handleGiveSet(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("$prefix §7Usage: /armament giveset <armorName> [player]")
            return
        }
        val armorName = args[1]
        val armorsConfig = plugin.getArmorsConfig()
        val armors = armorsConfig.getConfigurationSection("armors")?.getKeys(false)
            ?: return sender.sendMessage("$prefix ${plugin.getMessage("armors-not-found")}")

        if (armorName !in armors) {
            sender.sendMessage("$prefix ${plugin.getMessage("armor-not-found")}")
            return
        }
        val armorItems = armorCreator.createFullArmorSet(armorName)

        val targetPlayer: Player? = when {
            args.size >= 3 -> Bukkit.getPlayer(args[2])
            sender is Player -> sender
            else -> null
        }

        if (targetPlayer != null) {
            armorItems.forEach {
                if (targetPlayer.inventory.firstEmpty() == -1) {
                    it?.let { piece -> targetPlayer.world.dropItemNaturally(targetPlayer.location, piece) }
                } else {
                    targetPlayer.inventory.addItem(it)
                }
            }
            sender.sendMessage(
                "$prefix ${
                    plugin.getMessage("armorset-given").replace("{player}", targetPlayer.name)
                        .replace("{armorName}", armorName)
                }"
            )
            targetPlayer.sendMessage(
                "$prefix ${
                    plugin.getMessage("armorset-received").replace("{armorName}", armorName)
                }"
            )
        } else {
            sender.sendMessage("$prefix ${plugin.getMessage("player-not-found")}")
        }
    }

    private fun handleGUI(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("$prefix ${plugin.getMessage("player-only-command")}")
            return
        }
        ArmorGUI(plugin).openGUI(sender, 0)
    }

    private fun handleGiveShoes(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) return
        sender.sendMessage("$prefix §7Usage: /armament giveshoes <slow|medium|fast> [player]")


        val speedType = args[1].lowercase()
        if (speedType !in listOf("slow", "medium", "fast")) return
        sender.sendMessage("$prefix §7Invalid speed type. Use slow, medium, or fast.")


        val targetPlayer = if (args.size >= 3) Bukkit.getPlayer(args[2]) else sender as? Player
        if (targetPlayer == null) return
        sender.sendMessage("$prefix ${plugin.getMessage("player-not-found")}")


        val shoes = createRunningShoes(speedType)
        targetPlayer.inventory.addItem(shoes)

        sender.sendMessage("$prefix §7Given $speedType running shoes to ${targetPlayer.name}")
        if (sender != targetPlayer) {
            targetPlayer.sendMessage("$prefix §7You received $speedType running shoes")
        }
    }

    private fun createRunningShoes(speedType: String): ItemStack {
        val shoes = armorCreator.createArmorPiece("maletrainer", "boots") ?: return ItemStack(Material.AIR)
        val itemMeta = shoes.itemMeta ?: return shoes

        val speedAmount = when (speedType) {
            "slow" -> 0.2
            "medium" -> 0.4
            "fast" -> 0.7
            else -> 0.2
        }

        val speedModifier = AttributeModifier(
            UUID.randomUUID(),
            "minecraft:generic.movement_speed",
            speedAmount,
            AttributeModifier.Operation.MULTIPLY_SCALAR_1,
            EquipmentSlot.FEET
        )
        itemMeta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, speedModifier)

        val displayName = when (speedType) {
            "slow" -> "&6Old Running Shoes"
            "medium" -> "&6Running Shoes"
            "fast" -> "&6Improved Running Shoes"
            else -> "&6Running Shoes"
        }
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName))

        shoes.itemMeta = itemMeta
        return shoes
    }
}