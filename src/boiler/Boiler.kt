package boiler

import boiler.parameter.Parameter
import boiler.parameter.parameterElement
import boiler.parameter.twoDecimalPlaces
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.button
import react.dom.div
import react.dom.p
import react.dom.strong
import styled.css
import styled.styledDiv
import kotlin.browser.window
import kotlin.math.max
import kotlin.math.min

interface BoilerProps : RProps {
    var specificHeatCapacity: Double /*[J/(kg*°C)]*/
}

interface BoilerState : RState {
    var attributes: Array<Parameter>
    var requirements: Array<Parameter>
//    var regulations: Array<Parameter>

    var simulationTime: Long

    var currentLevel: Double /*[m]*/
    var currentTemperature: Double /*[°C]*/
    var currentOutflow: Double /*[m3/s]*/

    var currentInflow: Double /*[m3/s]*/
    var currentPower: Double /*[W]*/
}

class Boiler(props: BoilerProps) : RComponent<BoilerProps, BoilerState>(props) {
    private val crossSectionArea get() = state.attributes[0].normalized() /*[m2]*/
    private val inflowMax get() = state.attributes[1].normalized() /*[m3/s]*/
    private val inflowTemperature get() = state.attributes[2].normalized() /*[°C]*/
    private val heaterPowerMax get() = state.attributes[3].normalized() /*[W]*/
    private val heaterEfficiency get() = state.attributes[4].normalized() /*[%]*/
    private val energyDrainCoefficient get() = state.attributes[5].normalized() /*[W]*/

    private val requiredLevel get() = state.requirements[0].normalized() /*[m]*/
    private val requiredTemperature get() = state.requirements[1].normalized()/*[°C]*/
    private val requiredOutflow get() = state.requirements[2].normalized() /*[l/s]*/

    private val timeStep get() = state.attributes[6].normalized().toLong() /*[s]*/

    private val refreshTimeout = 100
    private val refreshRate = 1000.0 / refreshTimeout
    private val actualTimeStep: Double get() = timeStep / refreshRate

    private var timeStepID: Int? = null
    private var timerID: Int? = null

    private fun setIntervals() {
        timeStepID = window.setInterval({ calculateNewState() }, refreshTimeout)
        timerID = window.setInterval({ setState { simulationTime += timeStep } }, 1000)
    }

    private fun clearIntervals() {
        timeStepID?.let { window.clearInterval(it) }
        timerID?.let { window.clearInterval(it) }
    }

    override fun componentDidMount() = setIntervals()

    override fun componentWillUnmount() = clearIntervals()

    private fun BoilerState.reset() {
        simulationTime = 0
        currentLevel = 0.0
        currentTemperature = 0.0
        currentInflow = 0.0
        currentOutflow = 0.0
        currentPower = 0.0
    }

    override fun BoilerState.init(props: BoilerProps) {
        attributes = arrayOf(
                Parameter(name = "Cross Section Area", unit = "m2", minValue = 10, maxValue = 20, scale = 0.1, curValue = 10),
                Parameter(name = "Max Inflow", unit = "l/s", minValue = 1, maxValue = 15, scale = 0.1, normalizationCoefficient = 0.001),
                Parameter(name = "Inflow Temperature", unit = "°C", minValue = 5, maxValue = 15),
                Parameter(name = "Max Heater Power", unit = "kW", minValue = 1, maxValue = 12, scale = 0.5, normalizationCoefficient = 1000.0),
                Parameter(name = "Heater Efficiency", unit = "%", minValue = 15, maxValue = 20, scale = 5.0, normalizationCoefficient = 0.01),
                Parameter(name = "Energy Drain Coefficient", unit = "W", minValue = 0, maxValue = 5, scale = 100.0, curValue = 1),
                Parameter(name = "Time Step", unit = "s", minValue = 1, maxValue = 60, scale = 60.0, curValue = 1)
        )

        requirements = arrayOf(
                Parameter(name = "Water Level", unit = "m", minValue = 0, maxValue = 20, scale = 0.1),
                Parameter(name = "Water Temperature", unit = "°C", minValue = 40, maxValue = 60),
                Parameter(name = "Water Outflow", unit = "l/s", minValue = 0, maxValue = 15, scale = 0.1, normalizationCoefficient = 0.001, curValue = 0)
        )

        /*regulations = arrayOf(
                Parameter(name = "Level Regulator Enhancement", unit = "%", minValue = 1, maxValue = 20, scale = 10.0, normalizationCoefficient = 0.01),
                Parameter(name = "Level Regulator Advance Time", unit = "s", minValue = 1, maxValue = 60, scale = 60.0),
                Parameter(name = "Level Regulator Doubling Time", unit = "s", minValue = 1, maxValue = 60, scale = 60.0),
                Parameter(name = "Level Regulator Sampling Time", unit = "s", minValue = 1, maxValue = 60, scale = 60.0),
                Parameter(name = "Temperature Regulator Enhancement", unit = "%", minValue = 1, maxValue = 20, scale = 10.0, normalizationCoefficient = 0.01),
                Parameter(name = "Temperature Regulator Advance Time", unit = "s", minValue = 1, maxValue = 60, scale = 60.0),
                Parameter(name = "Temperature Regulator Doubling Time", unit = "s", minValue = 1, maxValue = 60, scale = 60.0),
                Parameter(name = "Temperature Regulator Sampling Time", unit = "s", minValue = 1, maxValue = 60, scale = 60.0)
        )*/

        reset()
    }

    private fun waterDensity(temperature: Double): Double /*[kg/m3]*/ {
        require(temperature >= 0)
        return when {
            temperature < 4  -> 999.8
            temperature < 10 -> 999.972
            temperature < 15 -> 999.97
            temperature < 20 -> 999.1
            temperature < 25 -> 998.2
            temperature < 30 -> 997.0
            temperature < 40 -> 995.7
            temperature < 60 -> 992.2
            temperature < 80 -> 983.2
            else             -> 971.8
        }
    }

    private val currentVolume: Double get() = state.currentLevel * crossSectionArea /*[m * m2 = m3]*/
    private val currentMass: Double get() = currentVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
    private val currentEnergy: Double get() = props.specificHeatCapacity * currentMass * state.currentTemperature /*[J/(kg*°C) * kg * °C = J]*/

    private fun calculateLevelAndTemperature(): Pair<Double, Double> {
        val suppliedVolume = state.currentInflow * actualTimeStep /*[m3/s * s = m3]*/
        val suppliedMass = suppliedVolume * waterDensity(inflowTemperature) /*[m3 * kg/m3 = kg]*/
        val suppliedEnergy = props.specificHeatCapacity * suppliedMass * inflowTemperature + state.currentPower * heaterEfficiency * actualTimeStep /*[J/(kg*°C) * kg * °C + W * s = J]*/

        val drainedVolume = state.currentOutflow * actualTimeStep /*[m3/s * s = m3]*/
        val drainedMass = drainedVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
        val drainedEnergy = props.specificHeatCapacity * drainedMass * state.currentTemperature + energyDrainCoefficient * actualTimeStep /*[J/(kg*°C) * kg * °C + W * s = J]*/

        val newMass = max(suppliedMass + currentMass - drainedMass, 0.0) /*[kg]*/
        val newEnergy = max(currentEnergy + suppliedEnergy - drainedEnergy, 0.0) /*[J]*/
        val newTemperature = if (newMass == 0.0) 0.0 else newEnergy / props.specificHeatCapacity / newMass /*[J * (kg*°C) / J / kg = °C]*/
        val newVolume = newMass / waterDensity(state.currentTemperature) /*[kg * m3/kg = m3]*/
        val newLevel = newVolume / crossSectionArea /*[m3 / m2 = m]*/

        return newLevel to newTemperature
    }

    private fun calculateFlowAndPower(): Triple<Double, Double, Double> {
        val requiredVolume = requiredLevel * crossSectionArea /*[m * m2 = m3]*/
        val requiredMass = requiredVolume * waterDensity(requiredTemperature) /*[m3 * kg/m3 = kg]*/
        val requiredEnergy = props.specificHeatCapacity * max(requiredMass, currentMass) * requiredTemperature /*[J/(kg*°C) * kg * °C = J]*/

        val outflowVolume = requiredOutflow * actualTimeStep /*[m3/s * s = m3]*/
        val outflowMass = outflowVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/

        val actualOutflowVolume = min(outflowVolume, currentVolume) /*[m3]*/
        val actualOutflowMass = actualOutflowVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
        val actualOutflowEnergy = props.specificHeatCapacity * actualOutflowMass * state.currentTemperature /*[J/(kg*°C) * kg * °C]*/
        val actualOutflow = actualOutflowVolume / actualTimeStep /*[m3 / s]*/

        val requiredInflowMass = max(requiredMass - currentMass + outflowMass, 0.0) /*[kg]*/
        val requiredInflowVolume = requiredInflowMass / waterDensity(inflowTemperature) /*[kg * m3/kg = m3]*/
        val requiredInflow = requiredInflowVolume / actualTimeStep /*[m3 / s]*/

        val actualInflow = min(requiredInflow, inflowMax) /*[m3/s]*/
        val actualInflowVolume = actualInflow * actualTimeStep /*[m3/s * s = m3]*/
        val actualInflowMass = actualInflowVolume * waterDensity(inflowTemperature) /*[m3 * kg/m3 = kg]*/
        val actualInflowEnergy = props.specificHeatCapacity * actualInflowMass * inflowTemperature /*[J/(kg*°C) * kg * °C = J]*/

        val requiredEnergyIncome = max(requiredEnergy - currentEnergy + actualOutflowEnergy - actualInflowEnergy + energyDrainCoefficient * actualTimeStep, 0.0) /*[J]*/
        val requiredPower = requiredEnergyIncome / actualTimeStep / heaterEfficiency /*[J / s = W]*/
        val actualPower = min(requiredPower, heaterPowerMax) /*[W]*/

        return Triple(actualInflow, actualOutflow, actualPower)
    }

    /*private fun calculateInflow(levelDiff: Double) = levelDiff
            .times(crossSectionArea)
            .div(actualTimeStep)
            .coerceIn(0.0, inflowMax) *//*[m * m2 / s = m3/s]*/

    /*private fun calculatePower(temperatureDiff: Double) = temperatureDiff
            .times(props.fluidSpecificHeat)
            .times(currentMass)
            .div(actualTimeStep)
            .coerceIn(0.0, heaterPowerMax) *//*[°C * J/(kg*°C) * kg / s = J/s = W]*/

    /*private class Regulator(
            private val enhancement: () -> Double,
            private val advanceTime: () -> Double,
            private val doublingTime: () -> Double,
            private val samplingTime: () -> Double
    ) {
        private var last = 0.0
        private var sum = 0.0

        operator fun invoke(diff: Double): Double {
            sum += diff
            val integralPart = sum * samplingTime() / doublingTime()
            val differentialPart = (diff - last) * advanceTime() / samplingTime()
            last = diff
            return enhancement() * (diff + integralPart + differentialPart)
        }
    }*/

    /*private val levelRegulator = Regulator(
            { state.regulations[0].normalized() },
            { state.regulations[1].normalized() },
            { state.regulations[2].normalized() },
            { state.regulations[3].normalized() }
    )*/

    /*private val temperatureRegulator = Regulator(
            { state.regulations[4].normalized() },
            { state.regulations[5].normalized() },
            { state.regulations[6].normalized() },
            { state.regulations[7].normalized() }
    )*/

    /*private fun calculateFlowAndPower() = setState {
        currentInflow = calculateInflow(levelRegulator(requiredLevel - state.currentLevel))
        currentPower = calculatePower(temperatureRegulator(requiredTemperature - state.currentTemperature))
        currentOutflow = requiredOutflow.coerceAtMost(currentVolume / actualTimeStep)
    }*/

    private fun calculateNewState() {
        val (level, temperature) = calculateLevelAndTemperature()
        setState {
            currentLevel = level
            currentTemperature = temperature
        }
        val (inflow, outflow, power) = calculateFlowAndPower()
        setState {
            currentInflow = inflow
            currentOutflow = outflow
            currentPower = power
        }
    }

    private fun RBuilder.parameterArray(name: String, background: Color, parameters: Array<Parameter>) {
        styledDiv {
            css {
                classes.plusAssign("bordered-element")
                backgroundColor = background.lighten(20)
            }

            strong { +"$name:" }
            parameters.forEach {
                parameterElement {
                    parameter = it
                    color = background
                }
            }
        }
    }

    override fun RBuilder.render() {
        div("grid-container") {
            parameterArray("Parameters", Color.lightSkyBlue, state.attributes)
            div("bordered-element") {
                p { +"Water Level: ${state.currentLevel.twoDecimalPlaces()} [m]" }
                p { +"Water Temperature: ${state.currentTemperature.twoDecimalPlaces()} [°C]" }
                p { +"Heater Power: ${state.currentPower.twoDecimalPlaces()} [W]" }
                p { +"Water Inflow: ${state.currentInflow.times(1000).twoDecimalPlaces()} [l/s]" }
                p { +"Water Outflow: ${state.currentOutflow.times(1000).twoDecimalPlaces()} [l/s]" }
                p("App-boiler.ticker") { +"The boiler has been running for ${state.simulationTime} [s]" }

                button {
                    +"RESET"
                    attrs.onClickFunction = { setState { reset() } }
                }
            }
            parameterArray("Requirements", Color.lightSteelBlue, state.requirements)
        }
    }
}

fun RBuilder.boiler(handler: BoilerProps.() -> Unit) = child(Boiler::class) { attrs(handler) }