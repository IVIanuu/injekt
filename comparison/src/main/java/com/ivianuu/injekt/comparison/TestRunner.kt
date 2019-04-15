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

package com.ivianuu.injekt.comparison

import com.ivianuu.injekt.comparison.custom.CustomTest
import com.ivianuu.injekt.comparison.dagger2.DaggerTest
import com.ivianuu.injekt.comparison.injekt.InjektTest
import com.ivianuu.injekt.comparison.katana.KatanaTest
import com.ivianuu.injekt.comparison.kodein.KodeinTest
import com.ivianuu.injekt.comparison.koin.KoinTest
import org.nield.kotlinstatistics.median

private const val ROUNDS = 1_000

fun runInjektKatanaTests() {
    runInjectionTests(listOf(KatanaTest(), InjektTest()))
}

fun runAllInjectionTests() {
    runInjectionTests(
        listOf(
            DaggerTest(),
            CustomTest(),
            KodeinTest(),
            KoinTest(),
            KatanaTest(),
            InjektTest()
        )
    )
}

fun runInjectionTests(tests: List<InjectionTest>) {
    println("Running $ROUNDS iterations. Please stand by...")

    val timingsPerTest =
        mutableMapOf<String, MutableList<Timings>>()

    repeat(ROUNDS) {
        tests.forEach { test ->
            timingsPerTest.getOrPut(test.name) { mutableListOf() }
                .add(measure(test))
        }
    }

    val results = timingsPerTest
        .mapValues { it.value.results() }

    println()

    results.print()
}

fun measure(test: InjectionTest): Timings {
    val setup = measureCall { test.setup() }
    val injection = measureCall { test.inject() }
    test.shutdown()
    return Timings(test.name, setup, injection)
}

data class Timings(
    val injectorName: String,
    val setup: Long,
    val injection: Long
)

data class Results(
    val injectorName: String,
    val setupAverage: Double,
    val setupMedian: Double,
    val setupMin: Double,
    val setupMax: Double,
    val injectionAverage: Double,
    val injectionMedian: Double,
    val injectionMin: Double,
    val injectionMax: Double
)

inline fun measureCall(body: () -> Unit): Long {
    val before = System.nanoTime()
    body()
    val after = System.nanoTime()
    return after - before
}

fun Iterable<Timings>.results(): Results {
    return Results(
        injectorName = first().injectorName, // todo dirty
        setupAverage = map { it.setup }.average(),
        setupMedian = map { it.setup }.median(),
        setupMin = map { it.setup }.min()?.toDouble() ?: 0.0,
        setupMax = map { it.setup }.max()?.toDouble() ?: 0.0,
        injectionAverage = map { it.injection }.average(),
        injectionMedian = map { it.injection }.median(),
        injectionMin = map { it.injection }.min()?.toDouble() ?: 0.0,
        injectionMax = map { it.injection }.max()?.toDouble() ?: 0.0
    )
}

fun Map<String, Results>.print() {
    println("Setup:")
    println("Library | Average | Median | Min | Max")
    forEach { (name, results) ->
        println(
            "$name | " +
                    "${results.setupAverage} | " +
                    "${results.setupMedian} | " +
                    "${results.setupMin} | " +
                    "${results.setupMax}"
        )
    }

    println()

    println("Best:")
    println("Average | Median | Min | Max")
    println(
        "${minBy { it.value.setupAverage }?.key} | " +
                "${minBy { it.value.setupMedian }?.key} | " +
                "${minBy { it.value.setupMin }?.key} | " +
                "${maxBy { it.value.setupMax }?.key}"
    )

    println()

    println("Injection:")
    println("Library | Average | Median | Min | Max")
    forEach { (name, results) ->
        println(
            "$name | " +
                    "${results.injectionAverage} | " +
                    "${results.injectionMedian} | " +
                    "${results.injectionMin} | " +
                    "${results.injectionMax}"
        )
    }

    println()

    println("Best:")
    println("Average | Median | Min | Max")
    println(
        "${minBy { it.value.injectionAverage }?.key} | " +
                "${minBy { it.value.injectionMedian }?.key} | " +
                "${minBy { it.value.injectionMin }?.key} | " +
                "${maxBy { it.value.injectionMax }?.key}"
    )

}