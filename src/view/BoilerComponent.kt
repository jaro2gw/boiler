package view

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

interface BoilerComponentProps : RProps {
    var waterTemperature: Double
    var waterLevel: Double
}

class BoilerComponent(props: BoilerComponentProps) : RComponent<BoilerComponentProps, RState>(props) {
    override fun RBuilder.render() {
        //TODO represent data
    }
}