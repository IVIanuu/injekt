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

package com.ivianuu.injekt.comparison.dagger2module

import com.ivianuu.injekt.comparison.InjectionTest

object Dagger2ModuleTest : InjectionTest {

    override val name = "Dagger 2 Module"

    private var component: Dagger2ModuleComponent? = null

    override fun moduleCreation() {
        Dagger2Module()
    }

    override fun setup() {
        component = DaggerDagger2ModuleComponent.create()
    }

    override fun firstInject() {
        component!!.fib8
    }

    override fun secondInject() {
        component!!.fib8
    }

    override fun shutdown() {
        component = null
    }

}