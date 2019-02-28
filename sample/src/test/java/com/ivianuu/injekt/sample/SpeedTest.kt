/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.sample

import com.ivianuu.injekt.component
import com.ivianuu.injekt.get
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.sample.speed.Fib8
import com.ivianuu.injekt.sample.speed.fibonacciModule
import org.junit.Test

class SpeedTest {

    @Test
    fun testManualSpeed() {
        println("Run manual speed test..")
        val startup =
            (1..ROUNDS).map { measureDurationOnly { component("test") { modules(fibonacciModule) } } }
                .median().format()

        val component = component("test") { modules(fibonacciModule) }
        val testDurations = (1..ROUNDS).map { measureDurationOnly { component.get<Fib8>() } }

        println("Manual Speed test completed -> startup: $startup, injection time: ${testDurations.median().format()}")
    }

    @Test
    fun testGeneratedSpeed() {
        println("Run generated speed test..")
        val startup =
            (1..ROUNDS).map { measureDurationOnly { component("test") } }
                .median().format()

        val component = component("test")
        val testDurations = (1..ROUNDS).map { measureDurationOnly { component.get<Fib8>() } }

        println("Generated Speed test completed -> startup: $startup, injection time: ${testDurations.median().format()}")
    }

    private fun List<Double>.median(): Double = sorted()
        .let { (it[it.size / 2] + it[(it.size - 1) / 2]) / 2 }

    private fun Double?.format(): String = String.format("%.2f ms", this)


    inline fun measureDurationOnly(code: () -> Unit): Double {
        val start = System.nanoTime()
        code()
        return (System.nanoTime() - start) / 1000000.0
    }

    private companion object {
        private const val ROUNDS = 100
    }
}