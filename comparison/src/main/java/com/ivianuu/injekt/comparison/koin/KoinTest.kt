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

package com.ivianuu.injekt.comparison.koin

import com.ivianuu.injekt.comparison.Fib8
import com.ivianuu.injekt.comparison.InjectionTest
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.get

object KoinTest : InjectionTest, KoinComponent {

    override val name = "Koin"

    override fun moduleCreation() {
        createModule()
    }

    override fun setup() {
        startKoin { modules(koinModule) }
    }

    override fun firstInject() {
        get<Fib8>()
    }

    override fun secondInject() {
        get<Fib8>()
    }

    override fun shutdown() {
        stopKoin()
    }

}