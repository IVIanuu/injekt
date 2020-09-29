package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.RootFactory

interface CoffeeComponent {
    val pump: Pump
    val heater: Heater
}

@RootFactory
typealias CoffeeComponentFactory = (CoffeeModule) -> CoffeeComponent

@Module
object CoffeeModule {
    @Given
    val ElectricHeater.heater: Heater
        get() = this

    @Given
    val Thermosiphon.givenPump: Pump
        get() = this
}
