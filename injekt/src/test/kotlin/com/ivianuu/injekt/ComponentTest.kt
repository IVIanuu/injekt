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
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Test

class ComponentTest {

    private object Named

    @Test
    fun testGet() {
        val typed = TestDep1()
        val named = TestDep1()

        val component = Component {
            modules(
                Module {
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
        val componentA = Component {
            modules(
                Module {
                    factory { TestDep1() }
                }
            )
        }
        val componentB = Component {
            dependencies(componentA)
            modules(
                Module {
                    factory { TestDep2(get()) }
                }
            )
        }

        val componentC = Component {
            dependencies(componentB)
            modules(
                Module {
                    factory { TestDep3(get(), get()) }
                }
            )
        }

        componentC.get<TestDep3>()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetUnknownDefinitionThrows() {
        val component = Component()
        component.get<Int>()
    }

    @Test
    fun testGetOrNullUnknownReturnsNull() {
        val component = Component()
        assertNull(component.getOrNull<Int>())
    }

    @Test
    fun testGetLazy() {
        var called = false

        val component = Component {
            modules(
                Module {
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
        val component = Component {
            modules(
                Module {
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
        val module1 = Module {
            factory { "my_value" }
        }

        val module2 = Module {
            factory(override = true) { "my_overridden_value" }
        }

        val component = Component { modules(module1, module2) }

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testExplicitOverrideInNestedComponents() {
        val parentComponent = Component {
            modules(
                Module {
                    factory { "my_value" }
                }
            )
        }

        val childComponent = Component {
            modules(
                Module {
                    factory(override = true) { "my_overridden_value" }
                }
            )
        }

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_overridden_value", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsImplicitOverride() {
        val module1 = Module {
            factory { "my_value" }
        }

        val module2 = Module {
            factory { "my_overridden_value" }
        }

        Component { modules(module1, module2) }
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsNestedImplicitOverride() {
        val componentA = Component {
            modules(
                Module {
                    factory { "my_value" }
                }
            )
        }

        Component {
            dependencies(componentA)
            modules(
                Module {
                    factory { "my_overriden_value" }
                }
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsIfDependenciesOverrideEachOther() {
        val dependency1 = Component {
            modules(
                Module {
                    factory { "value_1" }
                }
            )
        }

        val dependency2 = Component {
            modules(
                Module {
                    factory { "value_2" }
                }
            )
        }

        Component { dependencies(dependency1, dependency2) }
    }

    @Test
    fun testTypeDistinction() {
        val component = Component {
            modules(
                Module {
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
        val component = Component {
            modules(
                Module {
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
        val dependency = Component {
            scopes(TestScopeOne)
        }

        Component { dependencies(dependency) }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsWhenOverridingScope() {
        val dependency = Component {
            scopes(TestScopeOne)
        }

        Component {
            scopes(TestScopeOne)
            dependencies(dependency)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnDependenciesWithSameScope() {
        val dependency1 = Component {
            scopes(TestScopeOne)
        }

        val dependency2 = Component {
            scopes(TestScopeOne)
        }

        Component {
            scopes(TestScopeTwo)
            dependencies(dependency1, dependency2)
        }
    }

    @Test
    fun testImplicitComponentBindings() {
        val componentA = Component { scopes(TestScopeOne) }
        val componentB = Component {
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
        val componentA = Component {
            modules(
                Module {
                    single { TestDep1() }
                }
            )
        }

        val componentB = Component { dependencies(componentA) }
        val componentC = Component { dependencies(componentB) }

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
        val componentA = Component { scopes(TestScopeOne) }

        val componentB = Component {
            scopes(TestScopeTwo)
            dependencies(componentA)
        }
        val componentC = Component {
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
        val componentA = Component {
            modules(
                Module {
                    single(scoped = false) { Context(get()) }
                        .bindAlias<Environment>()
                }
            )
        }
        val componentB = Component { dependencies(componentA) }
        val componentC = Component { dependencies(componentB) }

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
        val component = Component {
            instance("string")
            instance(1)
        }

        assertEquals("string", component.get<String>())
        assertEquals(1, component.get<Int>())
    }

    @Test
    fun testInstantiatesEagerBindingOnStart() {
        var called = false
        Component {
            modules(
                Module {
                    single(eager = false) { called = true }
                }
            )
        }
        assertFalse(called)
        Component {
            modules(
                Module {
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
