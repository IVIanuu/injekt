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

        val component = component(
            modules = listOf(
                module {
                    factory { typed }
                    single("named") { named }
                }
            )
        )

        val typedGet = component.get<TestDep1>()
        assertEquals(typed, typedGet)

        val namedGet = component.get<TestDep1>("named")
        assertEquals(named, namedGet)
    }

    @Test
    fun testGetNested() {
        val dependency = component(
            modules = listOf(
                module {
                    factory { TestDep1() }
                }
            )
        )

        val component = component(
            modules = listOf(
                module {
                    factory { TestDep2(get()) }
                }
            ),
            dependencies = listOf(dependency)
        )

        component.get<TestDep2>()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetUnknownDefinitionThrows() {
        val component = component()
        component.get<TestDep1>()
    }

    @Test
    fun testLazy() {
        var called = false

        val component = component(
            modules = listOf(
                module {
                    factory {
                        called = true
                        TestDep1()
                    }
                }
            )
        )

        assertFalse(called)

        val depLazy = component.inject<TestDep1>()
        assertFalse(called)
        depLazy.value
        assertTrue(called)
    }

    @Test
    fun testAddDependency() {
        val dependency = component()
        val component = component(dependencies = listOf(dependency))

        assertTrue(component.dependencies.contains(dependency))
    }

    @Test
    fun testAddModule() {
        val binding = binding(FactoryKind) { "value" }
        val module = module { bind(binding) }
        val component = component(modules = listOf(module))
        assertTrue(component.bindings.values.contains(binding))
    }

    @Test
    fun testAddBinding() {
        val binding = binding(FactoryKind) { "value" }
        val component = component(
            modules = listOf(
                module { bind(binding) }
            )
        )
        assertTrue(component.bindings.containsValue(binding))
    }

    @Test
    fun testExplicitOverride() {
        val module1 = module {
            factory { "my_value" }
        }

        val module2 = module {
            factory(override = true) { "my_overridden_value" }
        }

        val component = component(
            modules = listOf(module1, module2)
        )

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testExplicitOverrideInNestedComponents() {
        val parentComponent = component(
            modules = listOf(
                module {
                    factory { "my_value" }
                }
            )
        )

        val childComponent = component(
            modules = listOf(
                module {
                    factory(override = true) { "my_overridden_value" }
                }
            )
        )

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_overridden_value", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsImplicitOverride() {
        val module1 = module {
            factory { "my_value" }
        }

        val module2 = module {
            factory { "my_overridden_value" }
        }

        component(
            modules = listOf(module1, module2)
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsNestedImplicitOverride() {
        val rootComponent = component(
            modules = listOf(
                module {
                    factory { "my_value" }
                }
            )
        )

        component(
            dependencies = listOf(rootComponent),
            modules = listOf(
                module {
                    factory { "my_overridden_value" }
                }
            )
        )
    }

}