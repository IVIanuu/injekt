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

package com.ivianuu.injekt.comparison.dagger2

import com.ivianuu.injekt.comparison.fibonacci.Fib8
import dagger.BindsInstance
import dagger.Component
import javax.inject.Inject
import javax.inject.Singleton

@Component
interface Dagger2Component {
    val fib8: Fib8

    @Component.Factory
    interface Factory {
        fun create(): Dagger2Component
    }
}


@Component
interface DepComponent {
    val string: String

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance string: String): DepComponent
    }
}

@Singleton
@Component(dependencies = [DepComponent::class])
interface ChildComponent {
    val string: String
    val lol: Lol

    @Component.Factory
    interface Factory {
        fun create(depComponent: DepComponent): ChildComponent
    }
}

@Singleton
class Lol @Inject constructor(val string: String)
