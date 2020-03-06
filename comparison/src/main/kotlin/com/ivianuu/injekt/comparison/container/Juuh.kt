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

package com.ivianuu.injekt.comparison.container

import com.ivianuu.injekt.comparison.container.impl.Container
import com.ivianuu.injekt.comparison.container.impl.factory
import com.ivianuu.injekt.comparison.container.impl.get
import com.ivianuu.injekt.comparison.container.impl.plus

class MyClass

fun TwilightContainer() = Container {
    factory<MyClass>()
    factory(name = "twilight") { "hehe" }
}

fun GesturesContainer() = Container {
    factory(name = "gestures") { "haha" }
}

fun ConditionalContainer(parent: Container) = parent + Container {
    if (parent.get(name = "is_debug")) {
        factory { "debug" }
    } else {
        factory { "release" }
    }
}

// usage
fun main() {
    val activityContainer = TwilightContainer() + GesturesContainer() + GesturesContainer()
    val fragmentContainer = ConditionalContainer(activityContainer + activityContainer)
    activityContainer.get<MyClass>()
}
