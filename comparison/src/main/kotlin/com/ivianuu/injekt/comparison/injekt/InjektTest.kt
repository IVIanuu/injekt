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

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.InitializeInjekt
import com.ivianuu.injekt.comparison.base.InjectionTest
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.runReader

@InitializeInjekt
object InjektTest : InjectionTest {

    override val name = "Injekt"

    private var context: ApplicationContext? = null

    override fun setup() {
        this.context = rootContext()
    }

    override fun inject() {
        context!!.runReader { given<Fib8>() }
    }

    override fun shutdown() {
        context = null
    }

}
