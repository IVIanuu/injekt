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

package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Readable
import com.ivianuu.injekt.create
import com.ivianuu.injekt.get
import com.ivianuu.injekt.scoped
import com.ivianuu.injekt.transient

class Foo
class Bar(foo: Foo)
class Store {
    fun <T> remember(init: () -> T) = init()
}

@Readable
fun <T> remember(init: () -> T) = get<Store>().remember(init)

interface BarComponent {
    val bar: Bar
}

@Factory
fun myFactory(): BarComponent {
    transient { Foo() }
    transient { remember { Bar(get()) } }
    scoped { Store() }
    return create()
}

fun main() {
    val barComponent = myFactory()
    println("${barComponent.bar} ${barComponent.bar}")
}
