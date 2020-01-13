package boiler

import boiler.parameter.Parameter
import boiler.parameter.parameterElement
import boiler.parameter.twoDecimalPlaces
import kotlinx.css.Float
import kotlinx.css.float
import kotlinx.css.pct
import kotlinx.css.width
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div
import react.dom.p
import styled.css
import styled.styledButton
import styled.styledDiv
import utils.StatisticsList
import kotlin.browser.window
import kotlin.math.max
import kotlin.math.min

const val waterSpecificHeat = 4200.0 /*[J/(kg*°C)]*/

interface BoilerProps : RProps {
//    var initialState: Array<Parameter>
}

interface BoilerState : RState {
    var parameters: Array<Parameter>
    var requirements: Array<Parameter>

    var count: Long

    var currentLevel: Double /*[m]*/
    var currentTemperature: Double /*[°C]*/
    var currentOutflow: Double /*[m3/s]*/

    var currentInflow: Double /*[m3/s]*/
    var currentPower: Double /*[W]*/
}

class Boiler(props: BoilerProps) : RComponent<BoilerProps, BoilerState>(props) {
    private val requiredLevel get() = state.requirements[0].normalized() /*[m]*/
    private val requiredTemperature get() = state.requirements[1].normalized()/*[°C]*/
    private val requiredOutflow get() = state.requirements[2].normalized() /*[l/s]*/

    private val crossSectionArea get() = state.parameters[0].normalized() /*[m2]*/
    private val inflowMax get() = state.parameters[1].normalized() /*[m3/s]*/
    private val inflowTemperature get() = state.parameters[2].normalized() /*[°C]*/
    private val heaterPowerMax get() = state.parameters[3].normalized() /*[W]*/
    private val heaterEfficiency get() = state.parameters[4].normalized() /*[%]*/
    private val energyDrainCoefficient get() = state.parameters[5].normalized() /*[W]*/

    private val timeStepParameter get() = state.parameters[6]
    private val timeStep get() = timeStepParameter.normalized().toLong() /*[s]*/

    private val refreshTimeout = 100
    private val refreshRate = 1000.0 / refreshTimeout
    private val actualTimeStep: Double get() = timeStep / refreshRate

    private var timeStepID: Int? = null
    private var timerID: Int? = null

    private fun setIntervals() {
        timeStepID = window.setInterval({ calculateNewState() }, refreshTimeout)
        timerID = window.setInterval({ setState { count += timeStep } }, 1000)
    }

    private fun clearIntervals() {
        timeStepID?.let { window.clearInterval(it) }
        timerID?.let { window.clearInterval(it) }
    }

    override fun componentDidMount() = setIntervals()

    override fun componentWillUnmount() = clearIntervals()

    private fun BoilerState.reset() {
        count = 0
        currentLevel = 0.0
        currentTemperature = 0.0
        currentInflow = 0.0
        currentOutflow = 0.0
        currentPower = 0.0
    }

    override fun BoilerState.init(props: BoilerProps) {
        parameters = arrayOf(
                Parameter(name = "Cross Section Area", unit = "m2", minValue = 10, maxValue = 100, scale = 0.1, curValue = 10),
                Parameter(name = "Max Inflow", unit = "l/s", minValue = 1, maxValue = 15, scale = 0.1, normalizationCoefficient = 0.001),
                Parameter(name = "Inflow Temperature", unit = "°C", minValue = 5, maxValue = 15),
                Parameter(name = "Max Heater Power", unit = "kW", minValue = 1, maxValue = 12, scale = 0.5, normalizationCoefficient = 1000.0),
                Parameter(name = "Heater Efficiency", unit = "%", minValue = 15, maxValue = 20, scale = 5.0, normalizationCoefficient = 0.01),
                Parameter(name = "Energy Drain Coefficient", unit = "W", minValue = 0, maxValue = 5, scale = 100.0, curValue = 1),
                Parameter(name = "Time Step", unit = "s", minValue = 1, maxValue = 60, scale = 60.0, curValue = 1)
        )

        requirements = arrayOf(
                Parameter(name = "Required Water Level", unit = "m", minValue = 0, maxValue = 20, scale = 0.1),
                Parameter(name = "Required Water Temperature", unit = "°C", minValue = 40, maxValue = 60),
                Parameter(name = "Required Water Outflow", unit = "l/s", minValue = 0, maxValue = 15, scale = 0.1, normalizationCoefficient = 0.001, curValue = 0)
        )

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
    private val currentEnergy: Double get() = waterSpecificHeat * currentMass * state.currentTemperature /*[J/(kg*°C) * kg * °C = J]*/

    private fun calculateLevelAndTemperature(): Pair<Double, Double> {
        val suppliedVolume = state.currentInflow * actualTimeStep /*[m3/s * s = m3]*/
        val suppliedMass = suppliedVolume * waterDensity(inflowTemperature) /*[m3 * kg/m3 = kg]*/
        val suppliedEnergy = waterSpecificHeat * suppliedMass * inflowTemperature + state.currentPower * heaterEfficiency * actualTimeStep /*[J/(kg*°C) * kg * °C + W * s = J]*/

        val drainedVolume = state.currentOutflow * actualTimeStep /*[m3/s * s = m3]*/
        val drainedMass = drainedVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
        val drainedEnergy = waterSpecificHeat * drainedMass * state.currentTemperature + energyDrainCoefficient * actualTimeStep /*[J/(kg*°C) * kg * °C + W * s = J]*/

        val newMass = max(suppliedMass + currentMass - drainedMass, 0.0) /*[kg]*/
        val newEnergy = max(currentEnergy + suppliedEnergy - drainedEnergy, 0.0) /*[J]*/
        val newTemperature = if (newMass == 0.0) 0.0 else newEnergy / waterSpecificHeat / newMass /*[J * (kg*°C) / J / kg = °C]*/
        val newVolume = newMass / waterDensity(newTemperature) /*[kg * m3/kg = m3]*/
        val newLevel = newVolume / crossSectionArea /*[m3 / m2 = m]*/

        return newLevel to newTemperature
    }

    private val levelStats = StatisticsList()
    private val temperatureStats = StatisticsList()

    private fun calculateFlowAndPower2(): Triple<Double, Double, Double> {

        return Triple(0.0, 0.0, 0.0)
    }

    private fun calculateFlowAndPower(): Triple<Double, Double, Double> {
        val requiredVolume = requiredLevel * crossSectionArea /*[m * m2 = m3]*/
        val requiredMass = requiredVolume * waterDensity(requiredTemperature) /*[m3 * kg/m3 = kg]*/
        val requiredEnergy = waterSpecificHeat * requiredMass * requiredTemperature /*[J/(kg*°C) * kg * °C = J]*/

        val outflowVolume = requiredOutflow * actualTimeStep /*[m3/s * s = m3]*/
        val outflowMass = outflowVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/

        val actualOutflowVolume = min(outflowVolume, currentVolume) /*[m3]*/
        val actualOutflowMass = actualOutflowVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
        val actualOutflowEnergy = waterSpecificHeat * actualOutflowMass * state.currentTemperature /*[J/(kg*°C) * kg * °C]*/
        val actualOutflow = actualOutflowVolume / actualTimeStep /*[m3 / s]*/

        val requiredInflowMass = max(requiredMass - currentMass + outflowMass, 0.0) /*[kg]*/
        val requiredInflowVolume = requiredInflowMass / waterDensity(inflowTemperature) /*[kg * m3/kg = m3]*/
        val requiredInflow = requiredInflowVolume / actualTimeStep /*[m3 / s]*/

        val actualInflow = min(requiredInflow, inflowMax) /*[m3/s]*/
        val actualInflowVolume = actualInflow * actualTimeStep /*[m3/s * s = m3]*/
        val actualInflowMass = actualInflowVolume * waterDensity(inflowTemperature) /*[m3 * kg/m3 = kg]*/
        val actualInflowEnergy = waterSpecificHeat * actualInflowMass * inflowTemperature /*[J/(kg*°C) * kg * °C = J]*/

        val requiredEnergyIncome = max(requiredEnergy - currentEnergy + actualOutflowEnergy - actualInflowEnergy + energyDrainCoefficient * actualTimeStep, 0.0) /*[J]*/
        val requiredPower = requiredEnergyIncome / actualTimeStep / heaterEfficiency /*[J / s = W]*/
        val actualPower = min(requiredPower, heaterPowerMax) /*[W]*/

        return Triple(actualInflow, actualOutflow, actualPower)
    }

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

    private fun RBuilder.parameterArray(floatSide: Float, parameters: Array<Parameter>) {
        styledDiv {
            css {
                width = 20.pct
                float = floatSide
            }

            parameters.forEach { parameterElement { parameter = it } }
        }
    }

    override fun RBuilder.render() {
        parameterArray(Float.left, state.parameters)
        parameterArray(Float.right, state.requirements)

        div {
            p { +"WATER LEVEL: ${state.currentLevel.twoDecimalPlaces()}" }
            p { +"WATER TEMPERATURE: ${state.currentTemperature.twoDecimalPlaces()}" }
            p { +"HEATER POWER: ${state.currentPower.twoDecimalPlaces()}" }
            p { +"WATER INFLOW: ${state.currentInflow.times(1000).twoDecimalPlaces()}" }
            p { +"WATER OUTFLOW: ${state.currentOutflow.times(1000).twoDecimalPlaces()}" }
            p("App-boiler.ticker") {
                +"The boiler has been running for ${state.count} seconds"
//                ticker { timeStep = timeStepParameter }
            }
        }

        styledButton {
            +"RESET"
            attrs.onClickFunction = { setState { reset() } }
        }
    }
}

fun RBuilder.boiler(handler: BoilerProps.() -> Unit) = child(Boiler::class) { attrs(handler) }