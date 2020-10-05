package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.FunBinding

fun main() {
    val component = CoffeeComponentImpl()
    component.brewCoffee()
}

@FunBinding
fun brewCoffee(heater: Heater, pump: Pump) {
    heater.on()
    pump.pump()
    println(" [_]P coffee! [_]P ")
    heater.off()
}
