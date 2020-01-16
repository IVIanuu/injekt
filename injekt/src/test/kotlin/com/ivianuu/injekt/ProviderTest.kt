/*
 * Copyright 2019 Manuel Wrage
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProviderTest {

    @Test
    fun testProviderNotReturnsSameValue() {
        val component = Component {
            modules(
                Module {
                    factory { TestDep1() }
                }
            )
        }
        val provider = component.get<Provider<TestDep1>>()
        val value1 = provider()
        val value2 = provider()
        assertNotEquals(value1, value2)
    }

    @Test
    fun testProviderPassesParams() {
        var usedParams: Parameters? = null

        val component = Component {
            modules(
                Module {
                    factory {
                        usedParams = it
                        TestDep1()
                    }
                }
            )
        }

        val parameters = parametersOf("one", "two")

        val provider = component.get<Provider<TestDep1>>()

        provider { parameters }

        assertEquals(parameters, usedParams)
    }
}
