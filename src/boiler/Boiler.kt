package boiler

import react.*
import react.dom.p
import kotlin.math.max
import kotlin.math.min

const val waterSpecificHeat = 4200.0 /*[J/(kg*°C)]*/

interface BoilerProps : RProps {
    var crossSectionArea: Double /*[m2]*/

    var inflowMax: Double /*[m3/s]*/
    var inflowTemperature: Double /*[°C]*/

    var heaterPowerMax: Double /*[W]*/
    var heaterEfficiency: Double /*[scalar]*/
    var energyDrainCoefficient: Double /*[W]*/

    var initialWaterLevel: Double /*[m]*/
    var initialWaterTemperature: Double /*[°C]*/

    var timeStep: Double /*[s]*/
}

interface BoilerState : RState {
    var requiredLevel: Double /*[m]*/
    var requiredTemperature: Double /*[°C]*/
    var requiredOutflow: Double /*[m3/s]*/

    var currentLevel: Double /*[m]*/
    var currentTemperature: Double /*[°C]*/
    var currentOutflow: Double /*[m3/s]*/

    var currentInflow: Double /*[m3/s]*/
    var currentPower: Double /*[W]*/
}

class Boiler(props: BoilerProps) : RComponent<BoilerProps, BoilerState>(props) {
    override fun BoilerState.init(props: BoilerProps) {
        currentLevel = props.initialWaterLevel
        currentTemperature = props.initialWaterTemperature
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

    private val currentVolume: Double get() = state.currentLevel * props.crossSectionArea /*[m * m2 = m3]*/
    private val currentMass: Double get() = currentVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
    private val currentEnergy: Double get() = waterSpecificHeat * currentMass * state.currentTemperature /*[J/(kg*°C) * kg * °C = J]*/

    private fun calculateLevelAndTemperature(): Pair<Double, Double> {
        val suppliedVolume = state.currentInflow * props.timeStep /*[m3/s * s = m3]*/
        val suppliedMass = suppliedVolume * waterDensity(props.inflowTemperature) /*[m3 * kg/m3 = kg]*/
        val suppliedEnergy = waterSpecificHeat * suppliedMass * props.inflowTemperature + state.currentPower * props.timeStep /*[J/(kg*°C) * kg * °C + W * s = J]*/

        val drainedVolume = state.currentOutflow * props.timeStep /*[m3/s * s = m3]*/
        val drainedMass = drainedVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
        val drainedEnergy = waterSpecificHeat * drainedMass * state.currentTemperature + props.energyDrainCoefficient * props.timeStep /*[J/(kg*°C) * kg * °C + W * s = J]*/

        val newMass = max(suppliedMass + currentMass - drainedMass, 0.0) /*[kg]*/
        val newEnergy = max(currentEnergy + suppliedEnergy - drainedEnergy, 0.0) /*[J]*/
        val newTemperature = if (newMass == 0.0) 0.0 else newEnergy / waterSpecificHeat / newMass /*[J * (kg*°C) / J / kg = °C]*/
        val newVolume = newMass / waterDensity(newTemperature) /*[kg * m3/kg = m3]*/
        val newLevel = newVolume / props.crossSectionArea /*[m3 / m2 = m]*/

        return newLevel to newTemperature
    }

    private fun calculateInflowAndPower(): Triple<Double, Double, Double> {
        val requiredVolume = state.requiredLevel * props.crossSectionArea /*[m * m2 = m3]*/
        val requiredMass = requiredVolume * waterDensity(state.requiredTemperature) /*[m3 * kg/m3 = kg]*/
        val requiredEnergy = waterSpecificHeat * requiredMass * state.requiredTemperature /*[J/(kg*°C) * kg * °C = J]*/

        val outflowVolume = state.requiredOutflow * props.timeStep /*[m3/s * s = m3]*/
        val outflowMass = outflowVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/

        val actualOutflowVolume = min(outflowVolume, currentVolume) /*[m3]*/
        val actualOutflow = actualOutflowVolume / props.timeStep /*[m3 / s]*/
        val actualOutflowMass = actualOutflowVolume * waterDensity(state.currentTemperature) /*[m3 * kg/m3 = kg]*/
        val actualOutflowEnergy = waterSpecificHeat * actualOutflowMass * state.currentTemperature /*[J/(kg*°C) * kg * °C]*/

        val requiredInflowMass = max(requiredMass - currentMass + outflowMass, 0.0) /*[kg]*/
        val requiredInflowVolume = requiredInflowMass / waterDensity(props.inflowTemperature) /*[kg * m3/kg = m3]*/
        val requiredInflow = requiredInflowVolume / props.timeStep /*[m3 / s]*/

        val actualInflow = min(requiredInflow, props.inflowMax) /*[m3/s]*/
        val actualInflowVolume = actualInflow * props.timeStep /*[m3/s * s = m3]*/
        val actualInflowMass = actualInflowVolume * waterDensity(props.inflowTemperature) /*[m3 * kg/m3 = kg]*/
        val actualInflowEnergy = waterSpecificHeat * actualInflowMass * props.inflowTemperature /*[J/(kg*°C) * kg * °C = J]*/

        val requiredEnergyIncome = max(requiredEnergy - currentEnergy + actualOutflowEnergy - actualInflowEnergy + props.energyDrainCoefficient * props.timeStep, 0.0) /*[J]*/
        val requiredPower = requiredEnergyIncome / props.timeStep / props.heaterEfficiency /*[J / s = W]*/
        val actualPower = min(requiredPower, props.heaterPowerMax) /*[W]*/

        return Triple(actualInflow, actualOutflow, actualPower)
    }

    fun calculateNewState() {
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
        p { +"WATER LEVEL: ${state.currentLevel}" }
        p { +"WATER TEMPERATURE: ${state.currentTemperature}" }
        p { +"HEATER POWER: ${state.currentPower}" }
        p { +"WATER INFLOW: ${state.currentInflow}" }
        p { +"WATER OUTFLOW: ${state.currentOutflow}" }
    }
}

fun RBuilder.boiler(handler: BoilerProps.() -> Unit) = child(Boiler::class) {
    attrs(handler)
}