package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.component

fun main() {
    val component = component<CoffeeComponent>()
    component.brewCoffee()
}

@Binding
fun brewCoffee(heater: Heater, pump: Pump) {
    heater.on()
    pump.pump()
    println(" [_]P coffee! [_]P ")
    heater.off()
}
