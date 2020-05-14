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

package com.ivianuu.injekt.comparison.kodein

import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.erased.instance

object KodeinTest : InjectionTest {

    override val name = "Kodein"

    private var kodein: Kodein? = null

    override fun setup() {
        kodein = Kodein { import(createModule()) }
    }

    override fun inject() {
        kodein!!.direct.instance<Fib8>()
    }

    override fun shutdown() {
        kodein = null
    }
}
