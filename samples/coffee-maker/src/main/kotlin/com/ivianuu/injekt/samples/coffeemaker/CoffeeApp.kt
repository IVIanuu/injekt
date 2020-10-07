package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.FunBinding
import com.ivianuu.injekt.ImplBinding

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


@Component
abstract class CoffeeComponent {
    abstract val brewCoffee: brewCoffee
}

interface Heater {
    fun on()
    fun off()
    val isHot: Boolean
}

@ImplBinding(CoffeeComponent::class)
class ElectricHeater : Heater {
    private var heating: Boolean = false

    override fun on() {
        println("~ ~ ~ heating ~ ~ ~")
        heating = true
    }

    override fun off() {
        heating = false
    }

    override val isHot: Boolean
        get() = heating
}

interface Pump {
    fun pump()
}

@ImplBinding
class Thermosiphon(private val heater: Heater) : Pump {
    override fun pump() {
        if (heater.isHot) {
            println("=> => pumping => =>")
        }
    }
}
