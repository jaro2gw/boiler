package boiler.canvas

import kotlinx.css.BorderStyle
import kotlinx.css.Color
import kotlinx.css.properties.border
import kotlinx.css.px
import kotlinx.html.id
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import styled.css
import styled.styledCanvas
import kotlin.browser.document
import kotlin.browser.window

interface CanvasProps : RProps {
    var level: Double
    var temperature: Double
    var power: Double
    var inflow: Double
    var inflowTemperature: Double
    var outflow: Double
}

class Canvas : RComponent<CanvasProps, RState>() {
    private var timerID: Int? = null

    override fun componentDidMount() {
        timerID = window.setInterval({ repaint() }, 100)
    }

    override fun componentWillUnmount() {
        timerID?.let { window.clearInterval(it) }
    }

    private fun color(temperature: Double): String = "rgb(${(temperature + 20) * 2.56}, 0, ${(80 - temperature) * 2.56})"

    private fun repaint() {
        val canvas = document.getElementById("myCanvas") as? HTMLCanvasElement
        with(canvas?.getContext("2d")!! as CanvasRenderingContext2D) {
            val width = canvas.width.toDouble()
            val height = canvas.height.toDouble()
            clearRect(0.0, 0.0, width, height)
            lineWidth = 1.0
            fillStyle = color(props.temperature)
            fillRect(0.0, height * (1 - props.level / 2), width, height)
        }
    }

    override fun RBuilder.render() {
        styledCanvas {
            css {
                classes.plusAssign("boiler-canvas")
                border(2.px, BorderStyle.solid, Color.black)
            }
            attrs.id = "myCanvas"
        }
    }
}

fun RBuilder.boilerCanvas(handler: CanvasProps.() -> Unit) {
    child(Canvas::class) { attrs(handler) }
}