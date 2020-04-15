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

import com.ivianuu.injekt.comparison.fibonacci.Fib1
import com.ivianuu.injekt.comparison.fibonacci.Fib2
import com.ivianuu.injekt.comparison.fibonacci.Fib3
import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import com.ivianuu.injekt.comparison.fibonacci.Fib7
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import dagger.Component
import dagger.Module
import dagger.Provides

@Component
interface Dagger2Component {
    val fib8: Fib8

    @Component.Factory
    interface Factory {
        fun create(): Dagger2Component
    }
}

@Component(modules = [Dagger2Module::class])
interface Dagger2ComponentModules {
    val fib8: Fib8

    @Component.Factory
    interface Factory {
        fun create(): Dagger2ComponentModules
    }
}

@Module
class Dagger2Module {
    @Provides
    fun fib1() = Fib1()
    @Provides
    fun fib2() = Fib2()
    @Provides
    fun fib3(fib2: Fib2, fib1: Fib1) = Fib3(fib2, fib1)
    @Provides
    fun fib4(fib2: Fib3, fib1: Fib2) = Fib4(fib2, fib1)
    @Provides
    fun fib5(fib2: Fib4, fib1: Fib3) = Fib5(fib2, fib1)
    @Provides
    fun fib6(fib2: Fib5, fib1: Fib4) = Fib6(fib2, fib1)
    @Provides
    fun fib7(fib2: Fib6, fib1: Fib5) = Fib7(fib2, fib1)
    @Provides
    fun fib8(fib2: Fib7, fib1: Fib6) = Fib8(fib2, fib1)

}

@Component(modules = [Dagger2StaticModule::class])
interface Dagger2ComponentStaticModules {
    val fib8: Fib8

    @Component.Factory
    interface Factory {
        fun create(): Dagger2ComponentStaticModules
    }
}

@Module
object Dagger2StaticModule {
    @Provides
    fun fib1() = Fib1()
    @Provides
    fun fib2() = Fib2()
    @Provides
    fun fib3(fib2: Fib2, fib1: Fib1) = Fib3(fib2, fib1)
    @Provides
    fun fib4(fib2: Fib3, fib1: Fib2) = Fib4(fib2, fib1)
    @Provides
    fun fib5(fib2: Fib4, fib1: Fib3) = Fib5(fib2, fib1)
    @Provides
    fun fib6(fib2: Fib5, fib1: Fib4) = Fib6(fib2, fib1)
    @Provides
    fun fib7(fib2: Fib6, fib1: Fib5) = Fib7(fib2, fib1)
    @Provides
    fun fib8(fib2: Fib7, fib1: Fib6) = Fib8(fib2, fib1)

}