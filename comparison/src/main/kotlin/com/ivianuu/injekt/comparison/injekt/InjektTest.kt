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

package com.ivianuu.injekt.comparison.injekt

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib1
import com.ivianuu.injekt.comparison.fibonacci.Fib2
import com.ivianuu.injekt.comparison.fibonacci.Fib3
import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import com.ivianuu.injekt.comparison.fibonacci.Fib7
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

object InjektTest : InjectionTest {

    override val name = "Injekt"

    private var context: Context? = null

    override fun setup() {
        context = rootContext {
            unscoped { Fib1() }
            unscoped { Fib2() }
            unscoped { Fib3(given(), given()) }
            unscoped { Fib4(given(), given()) }
            unscoped { Fib5(given(), given()) }
            unscoped { Fib6(given(), given()) }
            unscoped { Fib7(given(), given()) }
            unscoped { Fib8(given(), given()) }
        }
    }

    override fun inject() {
        context!!.runReader { given<Fib8>() }
    }

    override fun shutdown() {
        context = null
    }

}
