package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Given

interface Pump {
    fun pump()
}

@Given
class Thermosiphon : Pump {
    override fun pump() {
        if (given<Heater>().isHot) {
            println("=> => pumping => =>")
        }
    }
}

@Given
fun givenPump(): Pump = given<Thermosiphon>()
