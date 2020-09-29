package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Given

interface Pump {
    fun pump()
}

@Given
class Thermosiphon(private val heater: Heater) : Pump {
    override fun pump() {
        if (heater.isHot) {
            println("=> => pumping => =>")
        }
    }
}
