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

import com.ivianuu.injekt.comparison.dagger2.Dagger2Test
import com.ivianuu.injekt.comparison.guice.GuiceTest
import com.ivianuu.injekt.comparison.injektcodegen.InjektCodegenTest
import com.ivianuu.injekt.comparison.injektdsl.InjektDslTest
import com.ivianuu.injekt.comparison.injektop.InjektOpTest
import com.ivianuu.injekt.comparison.injektoptimized.InjektOptimizedDslTest
import com.ivianuu.injekt.comparison.injektunoptimizeddsl.InjektUnoptimizedDslTest
import com.ivianuu.injekt.comparison.katana.KatanaTest
import com.ivianuu.injekt.comparison.kodein.KodeinTest
import com.ivianuu.injekt.comparison.koin.KoinTest
import com.ivianuu.injekt.comparison.toothpick.ToothpickTest
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
            KodeinTest,
            Dagger2Test,
            ToothpickTest,
            KoinTest,
            KatanaTest,
            GuiceTest,
            InjektOpTest,
            InjektOptimizedDslTest,
            InjektDslTest,
            InjektUnoptimizedDslTest,
            InjektCodegenTest
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
            timingsPerTest.getOrPut(test.name) { arrayListOf() }
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
    val firstInjection = measureNanoTime { test.inject() }
    val secondInjection = measureNanoTime { test.inject() }
    test.shutdown()
    return Timings(test.name, moduleCreation, setup, firstInjection, secondInjection)
}

data class Timings(
    val injectorName: String,
    val moduleCreation: Long,
    val setup: Long,
    val firstInjection: Long,
    val secondInjection: Long
)

data class Result(
    val name: String,
    val timings: Iterable<Long>
) {
    val average = timings.average()
    val median = timings.median()
    val min = timings.min()!!.toDouble()
    val max = timings.max()!!.toDouble()
}

data class Results(
    val injectorName: String,
    val moduleCreation: Result,
    val setup: Result,
    val firstInjection: Result,
    val secondInjection: Result
)

fun Iterable<Timings>.results(): Results {
    return Results(
        injectorName = first().injectorName, // todo dirty
        moduleCreation = Result("Module creation", map { it.moduleCreation }),
        setup = Result("Setup", map { it.setup }),
        firstInjection = Result("First injection", map { it.firstInjection }),
        secondInjection = Result("Second injection", map { it.secondInjection })
    )
}

fun Double.format(config: Config): String {
    return when (config.timeUnit) {
        TimeUnit.MILLIS -> String.format("%.5f ms", this / 1000000.0)
        TimeUnit.NANOS -> this.toString()
    }
}

fun Result.print(name: String, config: Config) {
    println(
        "$name | " +
                "${average.format(config)} | " +
                "${median.format(config)} | " +
                "${min.format(config)} | " +
                "${max.format(config)}"
    )
}

fun Map<String, Results>.print(config: Config) {
    println("Module:")
    println("Library | Average | Median | Min | Max")
    toList()
        .sortedBy { it.second.moduleCreation.min }
        .forEach { (name, results) ->
            results.setup.print(name, config)
        }

    println()

    println("Setup:")
    println("Library | Average | Median | Min | Max")
    toList()
        .sortedBy { it.second.setup.min }
        .forEach { (name, results) ->
            results.setup.print(name, config)
        }

    println()

    println("First injection")
    println("Library | Average | Median | Min | Max")

    toList()
        .sortedBy { it.second.firstInjection.min }
        .forEach { (name, results) ->
            results.firstInjection.print(name, config)
        }

    println()

    println("Second injection")
    println("Library | Average | Median | Min | Max")
    toList()
        .sortedBy { it.second.secondInjection.min }
        .forEach { (name, results) ->
            results.secondInjection.print(name, config)
        }

    println()

}