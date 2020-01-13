package boiler

import kotlinx.css.Float
import kotlinx.css.float
import kotlinx.css.pct
import kotlinx.css.width
import parameter.Parameter
import parameter.parameterElement
import parameter.twoDecimalPlaces
import react.*
import react.dom.div
import react.dom.p
import styled.css
import styled.styledDiv
import ticker.ticker
import kotlin.browser.window
import kotlin.math.max
import kotlin.math.min

const val waterSpecificHeat = 4200.0 /*[J/(kg*°C)]*/

interface BoilerProps : RProps {
    var boilerParameters: DoubleArray
}

interface BoilerState : RState {
    var timeStep: Parameter /*[s]*/
    var requiredLevel: Parameter /*[m]*/
    var requiredTemperature: Parameter /*[°C]*/
    var requiredOutflow: Parameter /*[m3/s]*/

    var currentLevel: Double /*[m]*/
    var currentTemperature: Double /*[°C]*/
    var currentOutflow: Double /*[m3/s]*/

    var currentInflow: Double /*[m3/s]*/
    var currentPower: Double /*[W]*/
}

class Boiler(props: BoilerProps) : RComponent<BoilerProps, BoilerState>(props) {
    private val crossSectionArea: Double get() = props.boilerParameters[0] /*[m2]*/
    private val inflowMax: Double get() = props.boilerParameters[1] /*[m3/s]*/
    private val inflowTemperature: Double get() = props.boilerParameters[2] /*[°C]*/
    private val heaterPowerMax: Double get() = props.boilerParameters[3] /*[W]*/
    private val heaterEfficiency: Double get() = props.boilerParameters[4] /*[%]*/
    private val energyDrainCoefficient: Double get() = props.boilerParameters[5] /*[W]*/
    private val initialWaterLevel: Double get() = props.boilerParameters[6] /*[m]*/
    private val initialWaterTemperature: Double get() = props.boilerParameters[7] /*[°C]*/
    private val refreshTimeout = 100
    private val refreshRate = 1000.0 / refreshTimeout
    private val actualTimeStep: Double get() = state.timeStep.toDouble() / refreshRate
    private var timerID: Int? = null

    override fun componentDidMount() {
        timerID = window.setInterval({
            calculateNewState()
        }, refreshTimeout)
    }

    override fun componentWillUnmount() {
        window.clearInterval(timerID!!)
    }

    private fun BoilerState.update() {
        currentLevel = initialWaterLevel
        currentTemperature = initialWaterTemperature
        currentInflow = 0.0
        currentOutflow = 0.0
        currentPower = 0.0
    }

    override fun BoilerState.init(props: BoilerProps) {
        update()
        timeStep = Parameter("Time Step", "s", 1, 60)
        requiredLevel = Parameter("Required Water Level", "m", 0, 20, 0.1)
        requiredTemperature = Parameter("Required Water Temperature", "°C", 40, 60)
        requiredOutflow = Parameter("Required Water Outflow", "m3/s", 0, 25, 0.01)
    }

    override fun componentWillReceiveProps(nextProps: BoilerProps) {
        state.update()
    }

    private fun waterDensity(temperature: Double): Double /*[kg/m3]*/ {
        //TODO maybe improve how the water density is calculated
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
        val suppliedEnergy = waterSpecificHeat * suppliedMass * inflowTemperature + state.currentPower * actualTimeStep /*[J/(kg*°C) * kg * °C + W * s = J]*/

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

    private fun calculateInflowAndPower(): Triple<Double, Double, Double> {
        val requiredVolume = state.requiredLevel.toDouble() * crossSectionArea /*[m * m2 = m3]*/
        val requiredMass = requiredVolume * waterDensity(state.requiredTemperature.toDouble()) /*[m3 * kg/m3 = kg]*/
        val requiredEnergy = waterSpecificHeat * requiredMass * state.requiredTemperature.toDouble() /*[J/(kg*°C) * kg * °C = J]*/

        val outflowVolume = state.requiredOutflow.toDouble() * actualTimeStep /*[m3/s * s = m3]*/
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
        val (inflow, outflow, power) = calculateInflowAndPower()
        setState {
            currentInflow = inflow
            currentOutflow = outflow
            currentPower = power
        }
    }

    override fun RBuilder.render() {
        styledDiv {
            css {
                width = 20.pct
                float = Float.right
            }

            parameterElement {
                parameter = state.requiredLevel
                onValueChanged = { setState { } }
            }

            parameterElement {
                parameter = state.requiredTemperature
                onValueChanged = { setState { } }
            }

            parameterElement {
                parameter = state.requiredOutflow
                onValueChanged = { setState { } }
            }

            parameterElement {
                parameter = state.timeStep
                onValueChanged = { setState { } }
            }
        }

        div {
            p { +"WATER LEVEL: ${state.currentLevel.twoDecimalPlaces()}" }
            p { +"WATER TEMPERATURE: ${state.currentTemperature.twoDecimalPlaces()}" }
            p { +"HEATER POWER: ${state.currentPower.twoDecimalPlaces()}" }
            p { +"WATER INFLOW: ${state.currentInflow.twoDecimalPlaces()}" }
            p { +"WATER OUTFLOW: ${state.currentOutflow.twoDecimalPlaces()}" }
        }

        p("App-ticker") {
            ticker()
        }
    }
}

fun RBuilder.boiler(handler: BoilerProps.() -> Unit) = child(Boiler::class) {
    attrs(handler)
}