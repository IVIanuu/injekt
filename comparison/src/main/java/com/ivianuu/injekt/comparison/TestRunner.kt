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

import com.ivianuu.injekt.comparison.dagger2.DaggerTest
import com.ivianuu.injekt.comparison.injekt.InjektTest
import com.ivianuu.injekt.comparison.katana.KatanaTest
import com.ivianuu.injekt.comparison.kodein.KodeinTest
import com.ivianuu.injekt.comparison.koin.KoinTest
import org.nield.kotlinstatistics.median
import kotlin.system.measureNanoTime

val defaultConfig = Config(
    rounds = 100_000,
    timeUnit = TimeUnit.MILLIS
)

data class Config(
    val rounds: Int,
    val timeUnit: TimeUnit
)

enum class TimeUnit {
    NANOS, MILLIS
}

fun runAllInjectionTests(config: Config = defaultConfig) {
    runInjectionTests(
        listOf(
            DaggerTest,
            KodeinTest,
            KoinTest,
            KatanaTest,
            InjektTest
        ),
        config
    )
}

fun runInjectionTests(vararg tests: InjectionTest, config: Config = defaultConfig) {
    runInjectionTests(tests.toList(), config)
}

fun runInjectionTests(tests: Iterable<InjectionTest>, config: Config = defaultConfig) {
    println("Running ${config.rounds} iterations. Please stand by...")

    val timingsPerTest = linkedMapOf<String, MutableList<Timings>>()

    repeat(config.rounds) {
        tests.forEach { test ->
            timingsPerTest.getOrPut(test.name) { mutableListOf() }
                .add(measure(test))
        }
    }

    val results = timingsPerTest
        .mapValues { it.value.results() }

    println()

    results.print(config)
}

fun measure(test: InjectionTest): Timings {
    val moduleCreation = measureNanoTime { test.moduleCreation() }
    val setup = measureNanoTime { test.setup() }
    val injection = measureNanoTime { test.inject() }
    test.shutdown()
    return Timings(test.name, moduleCreation, setup, injection)
}

data class Timings(
    val injectorName: String,
    val moduleCreation: Long,
    val setup: Long,
    val injection: Long
)

data class Results(
    val injectorName: String,
    val moduleCreationAverage: Double,
    val moduleCreationMedian: Double,
    val moduleCreationMin: Double,
    val moduleCreationMax: Double,
    val setupAverage: Double,
    val setupMedian: Double,
    val setupMin: Double,
    val setupMax: Double,
    val injectionAverage: Double,
    val injectionMedian: Double,
    val injectionMin: Double,
    val injectionMax: Double
)

fun Iterable<Timings>.results(): Results {
    return Results(
        injectorName = first().injectorName, // todo dirty
        moduleCreationAverage = map { it.moduleCreation }.average(),
        moduleCreationMedian = map { it.moduleCreation }.median(),
        moduleCreationMin = map { it.moduleCreation }.min()!!.toDouble(),
        moduleCreationMax = map { it.moduleCreation }.max()!!.toDouble(),
        setupAverage = map { it.setup }.average(),
        setupMedian = map { it.setup }.median(),
        setupMin = map { it.setup }.min()!!.toDouble(),
        setupMax = map { it.setup }.max()!!.toDouble(),
        injectionAverage = map { it.injection }.average(),
        injectionMedian = map { it.injection }.median(),
        injectionMin = map { it.injection }.min()!!.toDouble(),
        injectionMax = map { it.injection }.max()!!.toDouble()
    )
}

fun Double.format(config: Config): String {
    return when (config.timeUnit) {
        TimeUnit.MILLIS -> String.format("%.5f ms", this / 1000000.0)
        TimeUnit.NANOS -> this.toString()
    }
}

fun Map<String, Results>.print(config: Config) {
    println("Module:")
    println("Library | Average | Median | Min | Max")
    forEach { (name, results) ->
        println(
            "$name | " +
                    "${results.moduleCreationAverage.format(config)} | " +
                    "${results.moduleCreationMedian.format(config)} | " +
                    "${results.moduleCreationMin.format(config)} | " +
                    "${results.moduleCreationMax.format(config)}"
        )
    }

    println()

    println("Best:")
    println("Average | Median | Min | Max")
    println(
        "${minBy { it.value.moduleCreationAverage }?.key} | " +
                "${minBy { it.value.moduleCreationMedian }?.key} | " +
                "${minBy { it.value.moduleCreationMin }?.key} | " +
                "${maxBy { it.value.moduleCreationMax }?.key}"
    )

    println()

    println("Setup:")
    println("Library | Average | Median | Min | Max")
    forEach { (name, results) ->
        println(
            "$name | " +
                    "${results.setupAverage.format(config)} | " +
                    "${results.setupMedian.format(config)} | " +
                    "${results.setupMin.format(config)} | " +
                    "${results.setupMax.format(config)}"
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
                    "${results.injectionAverage.format(config)} | " +
                    "${results.injectionMedian.format(config)} | " +
                    "${results.injectionMin.format(config)} | " +
                    "${results.injectionMax.format(config)}"
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