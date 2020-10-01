package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Binding

fun main() {
    val component = CoffeeComponentImpl()
    component.brewCoffee()
}

@Binding
fun brewCoffee(heater: Heater, pump: Pump) {
    heater.on()
    pump.pump()
    println(" [_]P coffee! [_]P ")
    heater.off()
}
