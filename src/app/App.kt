package app

import boiler.boiler
import kotlinx.css.*
import parameter.Parameter
import parameter.parameterElement
import react.*
import react.dom.h2
import react.dom.p
import styled.css
import styled.styledDiv
import ticker.ticker

interface AppProps : RProps {
    var parameters: Array<Parameter>
}

interface AppState : RState {
    var values: DoubleArray
}

class App(props: AppProps) : RComponent<AppProps, AppState>(props) {
    override fun AppState.init(props: AppProps) {
        values = props.parameters.map { it.toDouble() }.toDoubleArray()
    }

    override fun RBuilder.render() {
        styledDiv {
            css {
                height = 60.px
                padding(20.px)
                backgroundColor = Color("#0096ff")
                color = Color.white
            }
            h2 {
                +"Boiler Simulator"
            }
        }
        /*div("App-header") {
//            logo()
            h2 {
                +"Boiler Simulator"
            }
        }*/

        styledDiv {
            css {
                width = 20.pct
                float = Float.left
            }

            props.parameters.forEachIndexed { i, p ->
                parameterElement {
                    parameter = p
                    onValueChanged = { setState { values[i] = p.toDouble() } }
                }
            }
        }

        boiler {
            boilerParameters = state.values
        }

        p("App-ticker") {
            ticker()
        }
    }
}

fun RBuilder.app(handler: AppProps.() -> Unit) = child(App::class) { attrs(handler) }
