package model

const val waterSpecificHeat = 4200.0 /*[J/(kg*°C)]*/

class BoilerController(
        var crossSectionArea: Double, /*[m2]*/
        var inflowCapacity: Double, /*[m3/s]*/
        var inflowTemperature: Double, /*[°C]*/
        var heaterPower: Double, /*[W]*/
        var heaterEfficiency: Double, /*[scalar]*/
        var currentWaterTemperature: Double, /*[°C]*/
        var currentWaterLevel: Double = 0.0, /*[m]*/
        var timeStepSeconds: Double = 60.0 /*[s]*/
) {

    private fun waterDensity(temperature: Double): Double /*[kg/m3]*/ {
        //TODO maybe improve how the water density is calculated
        require(temperature > 0)
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

    private val currentWaterDensity: Double get() = waterDensity(currentWaterTemperature) /*[kg/m3]*/
    private val currentWaterMass: Double get() = currentWaterDensity * currentWaterLevel * crossSectionArea /*[kg/m3 * m * m2 = kg]*/
    private val currentWaterEnergy: Double get() = waterSpecificHeat * currentWaterMass * currentWaterTemperature /*[J/(kg*°C) * kg * °C = J]*/

    fun calculateNewState(requiredTemperature: Double, requiredLevel: Double): Pair<Double, Double> {
        var actualHeaterPower = 0.0 /*[W]*/
        var actualWaterInflow = 0.0 /*[m3/s]*/
        val temperatureDifference = requiredTemperature - currentWaterTemperature /*[°C]*/
        if (temperatureDifference > 0) {
            val requiredEnergy = waterSpecificHeat * currentWaterMass * temperatureDifference /*[J/(kg*°C) * kg * °C = J]*/
            val requiredHeaterPower = requiredEnergy / timeStepSeconds / heaterEfficiency /*[J / s = W]*/
            actualHeaterPower = requiredHeaterPower.coerceAtMost(heaterPower) /*[W]*/
            val suppliedEnergy = actualHeaterPower * timeStepSeconds /*[W * s = J]*/

            val temperatureIncrease = suppliedEnergy / waterSpecificHeat / currentWaterMass /*[J / (J/kg*°C) / kg = °C]*/
            currentWaterTemperature += temperatureIncrease /*[°C]*/
            currentWaterLevel = currentWaterMass / currentWaterDensity / crossSectionArea /*[kg / (kg/m3) / m2 = m]*/
        }
        val levelDifference = requiredLevel - currentWaterLevel /*[m]*/
        if (levelDifference > 0) {
            val requiredInflow = crossSectionArea * levelDifference / timeStepSeconds /*[m2 * m / s = m3/s]*/
            actualWaterInflow = requiredInflow.coerceAtMost(inflowCapacity) /*[m3/s]*/
            val inflowMass = waterDensity(inflowTemperature) * timeStepSeconds * actualWaterInflow /*[kg/m3 * s * m3/s = kg]*/
            val inflowEnergy = waterSpecificHeat * inflowMass * inflowTemperature /*[J/(kg*°C) * kg * °C = J]*/
            val totalEnergy = currentWaterEnergy + inflowEnergy /*[J]*/
            val totalMass = currentWaterMass + inflowMass /*[kg]*/
            currentWaterTemperature = totalEnergy / waterSpecificHeat / totalMass /*[J / (J/(kg*°C)) / kg = °C]*/
            val totalVolume = totalMass / currentWaterDensity /*[kg / (kg/m3) = m3]*/
            currentWaterLevel = totalVolume / crossSectionArea /*[m3 / m2 = m]*/
        }
        return (actualHeaterPower to actualWaterInflow)
    }
}