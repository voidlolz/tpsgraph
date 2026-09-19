package me.voidlolz.tpsgraph

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.`object`.ObjectContents
import kotlin.math.roundToInt

class TPSTracker(private val maxSamples: Int) {
    private val history = ArrayDeque<Double>()

    // versions belowe 1.21.9 crash since no player head component
    private val head: Component? = try {
        Component.`object`(ObjectContents.playerHead("CONSOLE"))
    } catch (e: LinkageError) {
        null
    }

    fun add(tps: Double) {
        if (history.size == maxSamples) {
            history.removeFirst()
        }

        history.addLast(tps)
    }

    fun render(tps: DoubleArray): Component {
        return Component.join(JoinConfiguration.newlines(), listOf(header(tps)) + graph())
    }

    private fun header(tps: DoubleArray): Component {
        val averages = listOf(
            "5s" to average(5),
            "10s" to average(10),
            "1m" to tps[0],
            "5m" to tps[1],
            "15m" to tps[2],
        )

        val header = Component.text()
        if (head != null) {
            header.append(head).append(Component.space())
        }

        averages.forEachIndexed { i, (name, avg) ->
            val value = avg.coerceAtMost(20.0)
            if (i > 0) {
                header.append(Component.text(" / ", NamedTextColor.DARK_GRAY))
            }

            header.append(Component.text("$name ", NamedTextColor.GRAY))
            header.append(Component.text("%.1f".format(value), tpsColor(value)))
        }

        return header.build()
    }

    private fun average(seconds: Int): Double {
        if (history.isEmpty()) {
            return 20.0
        }
        
        return history.takeLast(seconds).average()
    }

    private fun graph(): List<Component> {
        val empty = maxSamples - history.size
        val heights = history.map { (it / 20.0 * HEIGHT).roundToInt() }

        return (HEIGHT downTo 1).map { row ->
            val line = Component.text()
            var bars = StringBuilder()
            var current: TextColor = NamedTextColor.DARK_GRAY

            fun addBar(color: TextColor) {
                if (color != current && bars.isNotEmpty()) {
                    line.append(Component.text(bars.toString(), current))
                    bars = StringBuilder()
                }

                current = color
                bars.append('|')
            }

            repeat(empty) {
                addBar(NamedTextColor.DARK_GRAY)
            }

            history.forEachIndexed { i, tps ->
                addBar(if (heights[i] >= row) tpsColor(tps) else NamedTextColor.DARK_GRAY)
            }

            line.append(Component.text(bars.toString(), current))
            line.build()
        }
    }

    private fun tpsColor(tps: Double): TextColor = when {
        tps >= 18.0 -> NamedTextColor.GREEN
        tps >= 15.0 -> NamedTextColor.YELLOW
        else -> NamedTextColor.RED
    }

    private companion object {
        const val HEIGHT = 6
    }
}
