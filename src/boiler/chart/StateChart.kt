package boiler.chart

import react.*
import react.dom.div

private fun <T> useMemo(dependencies: RDependenciesArray = emptyArray(), callback: () -> T) = useMemo(callback, dependencies)

class StateChart : RComponent<RProps, RState>() {
    override fun RBuilder.render() {
        div("boiler-state-chart") {

        }
    }

}