package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component

@Component
abstract class CoffeeComponent {
    abstract val brewCoffee: brewCoffee

    @Binding
    protected val ElectricHeater.heater: Heater
        get() = this

    @Binding
    protected val Thermosiphon.givenPump: Pump
        get() = this
}
