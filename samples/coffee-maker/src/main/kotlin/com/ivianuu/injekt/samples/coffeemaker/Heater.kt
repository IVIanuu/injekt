package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given

interface Heater {
    fun on()
    fun off()
    val isHot: Boolean
}

@Given(ApplicationContext::class)
class ElectricHeater : Heater {
    private var heating: Boolean = false

    override fun on() {
        println("~ ~ ~ heating ~ ~ ~")
        heating = true
    }

    override fun off() {
        heating = false
    }

    override val isHot: Boolean
        get() = heating
}

@Given
fun givenHeater(): Heater = given<ElectricHeater>()
