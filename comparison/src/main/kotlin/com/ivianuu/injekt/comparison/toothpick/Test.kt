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

package com.ivianuu.injekt.comparison.toothpick

import com.ivianuu.injekt.comparison.Fib8
import com.ivianuu.injekt.comparison.InjectionTest
import toothpick.Scope
import toothpick.Toothpick

object ToothpickTest : InjectionTest {
    override val name: String
        get() = "Toothpick"

    private var scope: Scope? = null

    override fun moduleCreation() {
    }

    override fun setup() {
        scope = Toothpick.openScope(this)
    }

    override fun inject() {
        scope!!.getInstance(Fib8::class.java)
    }

    override fun shutdown() {
        Toothpick.closeScope(this)
    }
}
