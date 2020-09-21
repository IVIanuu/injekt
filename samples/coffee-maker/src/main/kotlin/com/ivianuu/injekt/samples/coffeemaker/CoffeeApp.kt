package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

@InitializeInjekt
fun main() {
    val context = rootContext<ApplicationContext>()
    context.runReader { brewCoffee() }
}

@Reader
private fun brewCoffee() {
    val heater = given<Heater>()
    heater.on()
    given<Pump>().pump()
    println(" [_]P coffee! [_]P ")
    heater.off()
}
