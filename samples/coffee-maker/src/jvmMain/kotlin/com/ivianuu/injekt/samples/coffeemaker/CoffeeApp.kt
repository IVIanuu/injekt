/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.inject

fun main() {
  val heater = inject<Heater>()
  heater.on()
  inject<Pump>().pump()
  println(" [_]P coffee! [_]P ")
  heater.off()
}

interface Heater {
  fun on()
  fun off()
  val isHot: Boolean
}

@Provide object ElectricHeater : Heater {
  override val isHot: Boolean
    get() = heating

  private var heating: Boolean = false

  override fun on() {
    println("~ ~ ~ heating ~ ~ ~")
    heating = true
  }

  override fun off() {
    heating = false
  }
}

fun interface Pump {
  fun pump()
}

@Provide fun thermosiphon(heater: Heater) = Pump {
  if (heater.isHot) {
    println("=> => pumping => =>")
  }
}
