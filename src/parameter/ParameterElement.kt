package parameter

import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.br
import react.dom.code
import styled.css
import styled.styledButton
import styled.styledDiv

interface ParameterProps : RProps {
    var parameter: Parameter
    var onValueChanged: () -> Unit
}

class ParameterElement(props: ParameterProps) : RComponent<ParameterProps, RState>(props) {
    override fun RBuilder.render() {
        styledDiv {
            css {
                backgroundColor = Color.aliceBlue
                padding(10.px)
                margin(10.px)
            }

            code { +"${props.parameter.name} [${props.parameter.unit}]:" }
            br { }

            styledButton {
                css {
                    float = Float.left
                    overflow = Overflow.auto
                }
                +"-"
                attrs {
                    disabled = props.parameter.curValue <= props.parameter.minValue
                    onClickFunction = {
                        --props.parameter.curValue
                        props.onValueChanged()
                    }
                }
            }

            styledButton {
                css {
                    float = Float.right
                    overflow = Overflow.auto
                }
                +"+"
                attrs {
                    disabled = props.parameter.curValue >= props.parameter.maxValue
                    onClickFunction = {
                        ++props.parameter.curValue
                        props.onValueChanged()
                    }
                }
            }

            code { +"${props.parameter.toDouble().twoDecimalPlaces()}" }
        }
    }
}

fun RBuilder.parameterElement(handler: ParameterProps.() -> Unit) = child(ParameterElement::class) {
    attrs(handler)
}