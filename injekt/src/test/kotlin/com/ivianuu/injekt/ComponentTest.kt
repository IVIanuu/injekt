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
        val componentA = component {
            modules(
                module {
                    factory { TestDep1() }
                }
            )
        }
        val componentB = component {
            dependencies(componentA)
            modules(
                module {
                    factory { TestDep2(get()) }
                }
            )
        }

        val componentC = component {
            dependencies(componentB)
            modules(
                module {
                    factory { TestDep3(get(), get()) }
                }
            )
        }

        componentC.get<TestDep3>()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetUnknownDefinitionThrows() {
        val component = component()
        component.get<Int>()
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
        depLazy()
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
        depProvider()
        assertEquals(1, called)
        depProvider()
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
        val componentA = component {
            modules(
                module {
                    factory { "my_value" }
                }
            )
        }

        component {
            dependencies(componentA)
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
                    factory { "string" }
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
            scopes(TestScopeOne)
        }

        component { dependencies(dependency) }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsWhenOverridingScope() {
        val dependency = component {
            scopes(TestScopeOne)
        }

        component {
            scopes(TestScopeOne)
            dependencies(dependency)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnDependenciesWithSameScope() {
        val dependency1 = component {
            scopes(TestScopeOne)
        }

        val dependency2 = component {
            scopes(TestScopeOne)
        }

        component {
            scopes(TestScopeTwo)
            dependencies(dependency1, dependency2)
        }
    }

    @Test
    fun testImplicitComponentBindings() {
        val componentA = component { scopes(TestScopeOne) }
        val componentB = component {
            scopes(TestScopeTwo)
            dependencies(componentA)
        }

        assertEquals(componentA, componentA.get<Component>())
        assertEquals(componentA, componentA.get<Component>(TestScopeOne))

        assertEquals(componentB, componentB.get<Component>())
        assertEquals(componentB, componentB.get<Component>(TestScopeTwo))
        assertEquals(componentA, componentB.get<Component>(TestScopeOne))
    }

    @Test
    fun testReusesSingleBindings() {
        val componentA = component {
            modules(
                module {
                    single { TestDep1() }
                }
            )
        }

        val componentB = component { dependencies(componentA) }
        val componentC = component { dependencies(componentB) }

        val depA = componentA.get<TestDep1>()
        val depA2 = componentA.get<TestDep1>()
        val depB = componentB.get<TestDep1>()
        val depC = componentC.get<TestDep1>()

        assertEquals(depA, depA2)
        assertEquals(depA, depB)
        assertEquals(depA, depC)
    }

    @Test
    fun testReusesSingleJustInTimeBindings() {
        val componentA = component { scopes(TestScopeOne) }

        val componentB = component {
            scopes(TestScopeTwo)
            dependencies(componentA)
        }
        val componentC = component {
            scopes(TestScopeThree)
            dependencies(componentB)
        }

        val depA = componentA.get<SingleJustInTimeBinding>()
        val depA2 = componentA.get<SingleJustInTimeBinding>()
        val depB = componentB.get<SingleJustInTimeBinding>()
        val depC = componentC.get<SingleJustInTimeBinding>()

        assertEquals(depA, depA2)
        assertEquals(depA, depB)
        assertEquals(depA, depC)
    }

    @Test
    fun testInstantiatesUnscopedBindingsInTheRequestingComponent() {
        val componentA = component {
            modules(
                module {
                    single(scoped = false) { Context(get()) }
                        .bindType<Environment>()
                }
            )
        }
        val componentB = component { dependencies(componentA) }
        val componentC = component { dependencies(componentB) }

        val contextA = componentA.get<Context>()
        val contextB = componentB.get<Context>()
        val contextC = componentC.get<Context>()

        assertEquals(componentA, contextA.component)
        assertEquals(componentB, contextB.component)
        assertEquals(componentC, contextC.component)

        val environmentA = componentA.get<Environment>()
        val environmentB = componentB.get<Environment>()
        val environmentC = componentC.get<Environment>()

        environmentA as Context
        environmentB as Context
        environmentC as Context

        assertEquals(componentA, environmentA.component)
        assertEquals(componentB, environmentB.component)
        assertEquals(componentC, environmentC.component)
    }

    @Test
    fun testComponentBuilderAddInstance() {
        val component = component {
            instance("string")
            instance(1)
        }

        assertEquals("string", component.get<String>())
        assertEquals(1, component.get<Int>())
    }

    @Test
    fun testInstantiatesEagerBindingOnStart() {
        var called = false
        component {
            modules(
                module {
                    single(eager = false) { called = true }
                }
            )
        }
        assertFalse(called)
        component {
            modules(
                module {
                    single(eager = true) { called = true }
                }
            )
        }
        assertTrue(called)
    }
}

class Context(val component: Component) : Environment

interface Environment

@TestScopeOne
@Single
class SingleJustInTimeBinding