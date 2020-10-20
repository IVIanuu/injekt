/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andF
 * limitations under the License.
 */

package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.FunBinding
import com.ivianuu.injekt.component

fun main() {
    val component = component<CoffeeComponent>()
    component.brewCoffee()
}

typealias brewCoffee = () -> Unit
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

@Binding(CoffeeComponent::class)
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

@Binding
val ElectricHeater.heater: Heater get() = this

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

@Binding
val Thermosiphon.pump: Pump
    get() = this