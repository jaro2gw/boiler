package app

import kotlinx.html.js.onClickFunction
import react.*
import react.dom.button

interface NumberParameterProps : RProps {
    var name: String
    var minValue: Double
    var maxValue: Double
}

interface NumberParameterState : RState {
    var value: Double
}

class NumberParameter(props: NumberParameterProps) : RComponent<NumberParameterProps, NumberParameterState>(props) {
    override fun NumberParameterState.init(props: NumberParameterProps) {
        value = props.minValue
    }

    override fun RBuilder.render() {
        +props.name
        button {
            +"-"
            attrs {
                disabled = state.value <= props.minValue
                onClickFunction = { setState { --value } }
            }
        }
        +state.value.toString()
        button {
            +"+"
            attrs {
                disabled = state.value >= props.maxValue
                onClickFunction = { setState { ++value } }
            }
        }
    }
}

fun RBuilder.parameter(handler: NumberParameterProps.() -> Unit) = child(NumberParameter::class) {
    attrs(handler)
}