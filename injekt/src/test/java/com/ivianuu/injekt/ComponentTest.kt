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

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    private object Named

    @Test
    fun testGet() {
        val typed = TestDep1()
        val named = TestDep1()

        val component = component {
            modules(
                module {
                    factory { typed }
                    factory(Named) { named }
                }
            )
        }

        val typedGet = component.get<TestDep1>()
        assertEquals(typed, typedGet)

        val namedGet = component.get<TestDep1>(Named)
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
            modules(
                module {
                    factory { TestDep2(get()) }
                }
            )
            dependencies(dependency)
        }

        component.get<TestDep2>()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetUnknownDefinitionThrows() {
        val component = component()
        component.get<TestDep1>()
    }

    @Test
    fun testGetLazy() {
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

        val depLazy = component.get<Lazy<TestDep1>>()
        assertFalse(called)
        depLazy.value
        assertTrue(called)
    }

    @Test
    fun testGetProvider() {
        var called = 0
        val component = component {
            modules(
                module {
                    factory {
                        ++called
                        TestDep1()
                    }
                }
            )
        }

        assertEquals(0, called)

        val depProvider = component.get<Provider<TestDep1>>()
        assertEquals(0, called)
        depProvider.get()
        assertEquals(1, called)
        depProvider.get()
        assertEquals(2, called)
    }

    @Test
    fun testExplicitOverride() {
        val module1 = module {
            factory { "my_value" }
        }

        val module2 = module {
            factory(override = true) { "my_overridden_value" }
        }

        val component = component { modules(module1, module2) }

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testExplicitOverrideInNestedComponents() {
        val parentComponent = component {
            modules(
                module {
                    factory { "my_value" }
                }
            )
        }

        val childComponent = component {
            modules(
                module {
                    factory(override = true) { "my_overridden_value" }
                }
            )
        }

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

        component { modules(module1, module2) }
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsNestedImplicitOverride() {
        val rootComponent = component {
            modules(
                module {
                    factory { "my_value" }
                }
            )
        }

        component {
            dependencies(rootComponent)
            modules(
                module {
                    factory { "my_overriden_value" }
                }
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsIfDependenciesOverrideEachOther() {
        val dependency1 = component {
            modules(
                module {
                    factory { "value_1" }
                }
            )
        }

        val dependency2 = component {
            modules(
                module {
                    factory { "value_2" }
                }
            )
        }

        component { dependencies(dependency1, dependency2) }
    }

    @Test
    fun testTypeDistinction() {
        val component = component {
            modules(
                module {
                    factory { listOf(1, 2, 3) }
                    factory { listOf("one", "two", "three") }
                }
            )
        }

        val ints = component.get<List<Int>>()
        val strings = component.get<List<String>>()

        assertNotSame(ints, strings)
    }

    @Test
    fun testNullableDistinction() {
        val component = component {
            modules(
                module {
                    factory<String> { "string" }
                    factory<String?> { "nullable string" }
                }
            )
        }

        val string = component.get<String>()
        assertEquals("string", string)
        val nullableString = component.get<String?>()
        assertEquals("nullable string", nullableString)
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsIfScopeIsNullWhileDependencyHasScope() {
        val dependency = component {
            scopes<TestScope>()
        }

        component { dependencies(dependency) }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsWhenOverridingScope() {
        val dependency = component {
            scopes<TestScope>()
        }

        component {
            scopes<TestScope>()
            dependencies(dependency)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnDependenciesWithSameScope() {
        val dependency1 = component {
            scopes<TestScope>()
        }

        val dependency2 = component {
            scopes<TestScope>()
        }

        component {
            scopes<OtherTestScope>()
            dependencies(dependency1, dependency2)
        }
    }
}