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

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Transient
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.composition.BindingEffect
import com.ivianuu.injekt.composition.CompositionComponent
import com.ivianuu.injekt.composition.CompositionFactory
import com.ivianuu.injekt.composition.Readable
import com.ivianuu.injekt.composition.compositionFactoryOf
import com.ivianuu.injekt.composition.get
import com.ivianuu.injekt.composition.initializeCompositions
import com.ivianuu.injekt.composition.installIn
import com.ivianuu.injekt.composition.parent
import com.ivianuu.injekt.composition.runReading
import com.ivianuu.injekt.create
import com.ivianuu.injekt.scoped
import com.ivianuu.injekt.transient

class Foo
class Bar(foo: Foo)

@CompositionComponent
interface TestCompositionComponent

@CompositionFactory
fun factory(): TestCompositionComponent {
    transient { Foo() }
    return create()
}

@Readable
fun func(foo: Foo = get()): Foo {
    return foo
}

@Readable
fun other() {
}

@Readable
fun <R> withFoo(block: @Readable (Foo) -> R): R = block(func())

fun invoke(): Foo {
    initializeCompositions()
    val component =
        compositionFactoryOf<TestCompositionComponent, () -> TestCompositionComponent>()()
    return component.runReading {
        withFoo {
            other()
            it
        }
    }
}
