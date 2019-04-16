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


package com.ivianuu.injekt

import com.ivianuu.injekt.util.TestDep1
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    @Test
    fun testGet() {
        val typed = TestDep1()
        val named = TestDep1()

        val component = component {
            modules(
                module {
                    factory { typed }
                    single("named") { named }
                }
            )
        }

        val typedGet = component.get<TestDep1>()
        assertEquals(typed, typedGet)

        val namedGet = component.get<TestDep1>("named")
        assertEquals(named, namedGet)
    }

    @Test
    fun testGetUnknownDefinitionThrows() {
        val component = component {}

        val throwed = try {
            component.get<TestDep1>()
            false
        } catch (e: BindingNotFoundException) {
            true
        }

        assertTrue(throwed)
    }

    @Test
    fun testLazy() {
        var called = false

        val component = component {
            modules(
                module {
                    factory {
                        called = true
                        TestDep1()
                    }
                }
            )
        }

        assertFalse(called)

        val depLazy = component.inject<TestDep1>()
        assertFalse(called)
        depLazy.value
        assertTrue(called)
    }

    @Test
    fun testInstanceResolving() {
        val typed = TestDep1()
        val named = TestDep1()

        val component = component {
            modules(
                module {
                    factory { typed }
                    factory("named") { named }
                }
            )
        }

        assertEquals(typed, component.get<TestDep1>())
        assertEquals(named, component.get<TestDep1>("named"))
    }
}