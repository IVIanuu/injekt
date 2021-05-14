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
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.samples.typeclasses.Ord.Companion.compareWith

@Extension fun interface Ord<in T> {
  infix fun T.compareWith(other: T): Int
}

fun <T> List<T>.sorted(@Given ord: Ord<T>): List<T> = sortedWith { a, b -> a compareWith b }

@Given val IntOrd = Ord<Int> { compareTo(it) }

fun main() {
  val items = listOf(5, 3, 4, 1, 2).sorted()
}
