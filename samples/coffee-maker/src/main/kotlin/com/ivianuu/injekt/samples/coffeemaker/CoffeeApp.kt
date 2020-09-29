package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.rootFactory

fun main() {
    val component = rootFactory<CoffeeComponentFactory>()(CoffeeModule)
    component.brewCoffee()
}

@Given
fun brewCoffee(heater: Heater, pump: Pump) {
    heater.on()
    pump.pump()
    println(" [_]P coffee! [_]P ")
    heater.off()
}
