package plugin.interaction.`object`

import core.cache.def.impl.ObjectDefinition
import core.game.interaction.OptionHandler
import core.game.node.Node
import core.game.node.`object`.GameObject
import core.game.node.`object`.ObjectBuilder
import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.system.SystemLogger
import core.game.system.task.Pulse
import core.game.world.GameWorld
import core.game.world.update.flag.context.Animation
import core.plugin.InitializablePlugin
import core.plugin.Plugin
import core.tools.Items
import java.util.concurrent.TimeUnit

@InitializablePlugin
class GutanothChestOptionHandler : OptionHandler(){
    override fun handle(player: Player?, node: Node?, option: String?): Boolean {
        player ?: return false
        val delay = player.getAttribute("gutanoth-chest-delay", 0L)
        SystemLogger.log(System.currentTimeMillis().toString())
        SystemLogger.log(delay.toString())
        GameWorld.Pulser.submit(ChestPulse(player,System.currentTimeMillis() > delay, node as GameObject))
        return true
    }

    override fun newInstance(arg: Any?): Plugin<Any> {
        ObjectDefinition.forId(2827).handlers["option:open"] = this
        return this
    }

    class ChestPulse(val player: Player, val isLoot: Boolean, val chest: GameObject): Pulse(){
        var ticks = 0
        override fun pulse(): Boolean {
            when(ticks++){
                0 -> {
                    player.lock()
                    player.animator.animate(Animation(536))
                    ObjectBuilder.replace(chest, GameObject(2828,chest.location,chest.rotation), 5)
                }
                2 -> {lootChest(player)}
                3 -> {player.unlock(); return true}
            }
            return false
        }


        fun lootChest(player: Player){
            if (isLoot) {
                player.setAttribute("/save:gutanoth-chest-delay", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15))
            } else {
                player.sendMessage("You open the chest and find nothing.")
                return
            }
            val reward = Rewards.values().random()
            player.sendChat(reward.message)
            when(reward.type){
                Type.ITEM -> if(!player.inventory.add(Item(reward.id))) GroundItemManager.create(Item(reward.id),player.location)
                Type.NPC -> {
                    val npc = NPC(reward.id)
                    npc.location = player.location
                    npc.isAggressive = true
                    npc.isRespawn = false
                    npc.properties.combatPulse.attack(player)
                    npc.init()
                }
            }
        }

        enum class Rewards(val id: Int,val type: Type,val message: String){
            BONES(Items.BONES_2530, Type.ITEM, "Oh! Some bones. Delightful."),
            EMERALD(Items.EMERALD_1605, Type.ITEM, "Ooh! A lovely emerald!"),
            ROTTEN_APPLE(Items.ROTTEN_APPLE_1984, Type.ITEM, "Oh, joy, spoiled fruit! My favorite!"),
            CHAOS_DWARF(119, Type.NPC, "You've gotta be kidding me, a dwarf?!"),
            RAT(47, Type.NPC, "Eek!"),
            SCORPION(1477, Type.NPC, "Zoinks!"),
            SPIDER(1004, Type.NPC, "Awh, a cute lil spidey!")
        }

        enum class Type {
            ITEM,
            NPC
        }
    }
}