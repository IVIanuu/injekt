package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Binding

interface Pump {
    fun pump()
}

@Binding
class Thermosiphon(private val heater: Heater) : Pump {
    override fun pump() {
        if (heater.isHot) {
            println("=> => pumping => =>")
        }
    }
}
