package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.rootFactory

fun main() {
    val component = rootFactory<CoffeeComponentFactory>()(CoffeeModule)
    val heater = component.heater
    heater.on()
    component.pump.pump()
    println(" [_]P coffee! [_]P ")
    heater.off()
}
