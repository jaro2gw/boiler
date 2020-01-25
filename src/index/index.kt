package index

import boiler.boiler
import kotlinext.js.require
import kotlinext.js.requireAll
import react.dom.div
import react.dom.h2
import react.dom.render
import kotlin.browser.document

fun main() {
    requireAll(require.context("src", true, js("/\\.css$/")))

    render(document.getElementById("root")) {
        div("header") { h2 { +"Symulator Pracy Wymiennika Ciepła" } }
        boiler { specificHeatCapacity = 4200.0 }
    }
}
