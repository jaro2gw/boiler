package index

import boiler.boiler
import kotlinext.js.require
import kotlinext.js.requireAll
import kotlinx.css.*
import react.dom.h2
import react.dom.render
import styled.css
import styled.styledDiv
import kotlin.browser.document

fun main() {
    requireAll(require.context("src", true, js("/\\.css$/")))

    render(document.getElementById("root")) {
        styledDiv {
            css {
                height = 60.px
                padding(20.px)
                backgroundColor = Color("#0096ff")
                color = Color.white
            }
            h2 { +"Boiler Simulator" }
        }

        boiler { fluidSpecificHeat = 4200.0 }
    }
}
