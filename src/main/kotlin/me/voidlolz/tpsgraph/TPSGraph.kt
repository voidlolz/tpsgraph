package me.voidlolz.tpsgraph

import com.mojang.brigadier.Command
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class TPSGraph : JavaPlugin() {
    private val tracker = TPSTracker(135)

    override fun onEnable() {
        var lastRun = System.nanoTime()

        server.scheduler.runTaskTimer(this, Runnable {
            val now = System.nanoTime()
            val seconds = (now - lastRun) / 1_000_000_000.0

            lastRun = now

            tracker.add((20.0 / seconds).coerceAtMost(20.0))
        }, 20L, 20L)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val tps = Commands.literal("tps")
                .requires { it.sender.hasPermission("tpsgraph.use") }
                .executes { ctx ->
                    ctx.source.sender.sendMessage(tracker.render(server.tps))
                    Command.SINGLE_SUCCESS
                }
                .build()

            event.registrar().register(tps)
        }
    }
}
