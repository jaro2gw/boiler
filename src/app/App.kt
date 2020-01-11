package app

import logo.logo
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.code
import react.dom.div
import react.dom.h2
import react.dom.p
import ticker.ticker

interface AppState : RState {
    var requiredWaterLevel: NumberParameter /*[m]*/
    var requiredWaterTemperature: NumberParameter /*[°C]*/
    var requiredWaterOutflow: NumberParameter /*[m3/s]*/
}

const val requiredWaterLevelID = "requiredWaterLevel"

class App : RComponent<RProps, AppState>() {
    override fun RBuilder.render() {
        div("App-header") {
            logo()
            h2 {
                +"Boiler Simulator"
            }
        }
        div {
            p {
                parameter {
                    name = "Required Water Level: "
                    minValue = 0.0
                    maxValue = 100.0
                }
            }
            p {
                parameter {
                    name = "Required Water Temperature: "
                    minValue = 40.0
                    maxValue = 60.0
                }
            }
            p {
                parameter {
                    name = "Required Water Outflow: "
                    minValue = 0.0
                    maxValue = 100.0
                }
            }
        }

        /*div {
            boiler {
                crossSectionArea = 10.0
                waterInflowMax = 100.0
                waterInflowTemperature = 10.0
                heaterPowerMax = 1000.0
                heaterEfficiency = 0.9
                initialWaterLevel = 0.0
                initialWaterTemperature = 0.0
                timeStep = 60.0
                requiredWaterLevel = state.requiredWaterLevel
                requiredWaterTemperature = state.requiredWaterTemperature
                requiredWaterOutflow = state.requiredWaterOutflow
            }
        }*/

        p("App-intro") {
            +"To get started, edit "
            code { +"app/App.kt" }
            +" and save to reload."
        }
        p("App-ticker") {
            ticker()
        }
    }
}

fun RBuilder.app() = child(App::class) {}
