package boiler.canvas

import kotlinx.css.*
import kotlinx.css.properties.border
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

    private fun repaint() {
        val canvas = document.getElementById("myCanvas") as? HTMLCanvasElement
        with(canvas?.getContext("2d")!! as CanvasRenderingContext2D) {
            clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
            lineWidth = 1.0
            fillText(props.level.toString(), 50.0, 50.0)
        }
    }

    override fun RBuilder.render() {
        styledCanvas {
            css {
                width = 400.px
                height = 400.px
                border(2.px, BorderStyle.solid, Color.black)
            }
            attrs.id = "myCanvas"
        }
    }
}

fun RBuilder.boilerCanvas(handler: CanvasProps.() -> Unit) {
    child(Canvas::class) { attrs(handler) }
}