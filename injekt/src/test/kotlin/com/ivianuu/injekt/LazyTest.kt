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

package com.ivianuu.injekt

import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test

class LazyTest {

    @Test
    fun testLazyInstantiatesOnce() {
        val component = Component {
            factory { TestDep1() }
        }
        val lazy = component.get<Lazy<TestDep1>>()
        val value1 = lazy()
        val value2 = lazy()
        assertTrue(value1 === value2)
    }

    @Test
    fun testLazyPassesParams() {
        var usedParams: Parameters? = null

        val component = Component {
            factory {
                usedParams = it
                TestDep1()
            }
        }

        val parameters = parametersOf("one", "two")

        val lazy = component.get<Lazy<TestDep1>>()

        lazy(parameters)

        Assert.assertEquals(parameters, usedParams)
    }
}
