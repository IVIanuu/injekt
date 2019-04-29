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
import com.ivianuu.injekt.util.TestDep2
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
    fun testGetNested() {
        val dependency = component {
            modules(
                module {
                    factory { TestDep1() }
                }
            )
        }

        val component = component {
            dependencies(dependency)
            modules(
                module {
                    factory { TestDep2(get()) }
                }
            )
        }

        val throwed = try {
            component.get<TestDep2>()
            false
        } catch (e: Exception) {
            true
        }

        assertFalse(throwed)
    }

    @Test
    fun testGetUnknownDefinitionThrows() {
        val component = component {}

        val throwed = try {
            component.get<TestDep1>()
            false
        } catch (e: Exception) {
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
    fun testAddDependency() {
        val dependency = component()
        val component = component {
            dependencies(dependency)
        }

        assertTrue(component.dependencies.contains(dependency))
    }

    @Test
    fun testAddModule() {
        val binding = binding(FactoryKind) { "value" }
        val module = module { bind(binding) }
        val component = component { modules(module) }
        assertTrue(component.bindings.contains(binding))
    }

    @Test
    fun testAddBinding() {
        val binding = binding(FactoryKind) { "value" }
        val component = component { addBinding(binding) }
        assertTrue(component.bindings.contains(binding))
    }

    @Test
    fun testOverride() {
        val component = component {
            modules(
                module {
                    factory { "my_value" }
                    single { "my_overridden_value" }
                }
            )
        }

        assertEquals("my_overridden_value", component.get<String>())
    }

}