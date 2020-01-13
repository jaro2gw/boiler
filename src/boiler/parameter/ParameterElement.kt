package boiler.parameter

import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.br
import styled.css
import styled.styledButton
import styled.styledDiv

interface ParameterProps : RProps {
    var parameter: Parameter
}

class ParameterElement(props: ParameterProps) : RComponent<ParameterProps, RState>(props) {
    override fun RBuilder.render() {
        styledDiv {
            css {
                backgroundColor = Color.aliceBlue
                padding(10.px)
                margin(10.px)
            }

            +"${props.parameter.name} [${props.parameter.unit}]:"
            br { }

            styledButton {
                css { float = Float.left }
                +"-"
                attrs {
                    disabled = props.parameter.reachedMin()
                    onClickFunction = { props.parameter.dec() }
                }
            }

            styledButton {
                css { float = Float.right }
                +"+"
                attrs {
                    disabled = props.parameter.reachedMax()
                    onClickFunction = { props.parameter.inc() }
                }
            }

            +"${props.parameter.toDouble().twoDecimalPlaces()}"
        }
    }
}

fun RBuilder.parameterElement(handler: ParameterProps.() -> Unit) = child(ParameterElement::class) {
    attrs(handler)
}