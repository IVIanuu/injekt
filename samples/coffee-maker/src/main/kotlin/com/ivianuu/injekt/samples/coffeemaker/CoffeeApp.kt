// injekt-incremental-fix 1614888972121 injekt-end
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given

fun main() {
    val heater = given<Heater>()
    heater.on()
    given<Pump>().pump()
    println(" [_]P coffee! [_]P ")
    heater.off()
}

interface Heater {
    fun on()
    fun off()
    val isHot: Boolean
}

@Given object ElectricHeater : @Given Heater {
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

@Given class Thermosiphon(@Given private val heater: Heater) : @Given Pump {
    override fun pump() {
        if (heater.isHot) {
            println("=> => pumping => =>")
        }
    }
}
