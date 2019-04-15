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

import kotlin.system.measureNanoTime

data class TestResult(
    val injectorName: String,
    val startupTime: List<Double>,
    val injectionTime: List<Double>
)

fun Double?.format() = String.format("%.2f ms", this)

fun measureTime(block: () -> Unit): Double = measureNanoTime(block) / 1000000.0

fun List<Double>.median() = sorted().let { (it[it.size / 2] + it[(it.size - 1) / 2]) / 2 }

fun log(msg: String) {
    println("DI-TEST: $msg")
}