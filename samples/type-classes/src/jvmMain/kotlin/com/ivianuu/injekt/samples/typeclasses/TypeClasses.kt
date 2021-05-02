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

package com.ivianuu.injekt.samples.typeclasses

import com.ivianuu.injekt.*

interface Ord<in T> {
    fun compareTo(a: T, b: T): Int
}

infix fun <T> T.compareTo(other: T, @Given ord: Ord<T>): Int = ord.compareTo(this, other)

fun <T> List<T>.ordered(@Given ord: Ord<T>): List<T> =
    sortedWith { a, b -> a compareTo b }

@Given
object IntOrd : Ord<Int> {
    override fun compareTo(a: Int, b: Int): Int = a.compareTo(b)
}

fun main() {
    val items = listOf(5, 3, 4, 1, 2).ordered()
}
