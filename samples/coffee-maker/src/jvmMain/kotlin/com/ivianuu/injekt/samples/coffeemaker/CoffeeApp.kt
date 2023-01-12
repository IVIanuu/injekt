/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
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

  private var heating = false

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
  if (heater.isHot)
    println("=> => pumping => =>")
}
