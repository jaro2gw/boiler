package index

import app.app
import kotlinext.js.require
import kotlinext.js.requireAll
import parameter.Parameter
import react.dom.render
import kotlin.browser.document

fun main() {
    requireAll(require.context("src", true, js("/\\.css$/")))

    render(document.getElementById("root")) {
        app {
            parameters = arrayOf(
                    Parameter("Cross Section Area", "m2", 10, 100, 0.1),
                    Parameter("Max Inflow", "m3/s", 1, 25, 0.01),
                    Parameter("Inflow Temperature", "°C", 5, 15),
                    Parameter("Max Heater Power", "W", 1, 12, 500.0),
                    Parameter("Heater Efficiency", "%", 15, 20, 5.0),
                    Parameter("Energy Drain Coefficient", "W", 0, 5, 100.0),
                    Parameter("Initial Water Level", "m", 0, 20, 0.1),
                    Parameter("Initial Water Temperature", "°C", 40, 60)
            )
        }
    }
}
