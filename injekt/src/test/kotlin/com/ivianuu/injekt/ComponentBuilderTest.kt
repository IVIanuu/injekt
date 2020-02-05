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

import junit.framework.Assert.assertEquals
import org.junit.Test

class ComponentBuilderTest {

    @Test
    fun testInstance() {
        val component = Component {
            instance("string")
            instance(1)
        }

        assertEquals("string", component.get<String>())
        assertEquals(1, component.get<Int>())
    }

    @Test
    fun testOverride() {
        val module1 = Module {
            factory { "my_value" }
        }

        val module2 = Module {
            factory(overrideStrategy = OverrideStrategy.Override) { "my_overridden_value" }
        }

        val component = Component { modules(module1, module2) }

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testOverrideDrop() {
        val module1 = Module {
            factory { "my_value" }
        }

        val module2 = Module {
            factory(overrideStrategy = OverrideStrategy.Drop) { "my_overridden_value" }
        }

        val component = Component { modules(module1, module2) }

        assertEquals("my_value", component.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        val module1 = Module {
            factory { "my_value" }
        }

        val module2 = Module {
            factory { "my_overridden_value" }
        }

        Component { modules(module1, module2) }
    }

    @Test
    fun testNestedOverride() {
        val parentComponent = Component {
            modules(
                Module {
                    factory { "my_value" }
                }
            )
        }

        val childComponent = Component {
            dependencies(parentComponent)
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Override) { "my_overridden_value" }
                }
            )
        }

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_overridden_value", childComponent.get<String>())
    }

    @Test
    fun testNestedOverrideDrop() {
        val parentComponent = Component {
            modules(
                Module {
                    factory { "my_value" }
                }
            )
        }

        val childComponent = Component {
            dependencies(parentComponent)
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Drop) { "my_overridden_value" }
                }
            )
        }

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_value", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testNestedOverrideFail() {
        val parentComponent = Component {
            modules(
                Module {
                    factory { "my_value" }
                }
            )
        }

        Component {
            dependencies(parentComponent)
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Fail) { "my_overridden_value" }
                }
            )
        }
    }

    @Test
    fun testDependencyOverride() {
        val dependencyComponentA = Component {
            modules(
                Module {
                    factory { "value_a" }
                }
            )
        }
        val dependencyComponentB = Component {
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Override) { "value_b" }
                }
            )
        }

        val childComponent = Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }

        assertEquals("value_b", childComponent.get<String>())
    }

    @Test
    fun testDependencyOverrideDrop() {
        val dependencyComponentA = Component {
            modules(
                Module {
                    factory { "value_a" }
                }
            )
        }
        val dependencyComponentB = Component {
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Drop) { "value_b" }
                }
            )
        }

        val childComponent = Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }

        assertEquals("value_a", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testDependencyOverrideFail() {
        val dependencyComponentA = Component {
            modules(
                Module {
                    factory { "value_a" }
                }
            )
        }
        val dependencyComponentB = Component {
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Fail) { "value_b" }
                }
            )
        }

        Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testReverseDependencyOverrideFail() {
        val dependencyComponentA = Component {
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Override) { "value_a" }
                }
            )
        }
        val dependencyComponentB = Component {
            modules(
                Module {
                    factory(overrideStrategy = OverrideStrategy.Fail) { "value_b" }
                }
            )
        }

        Component {
            dependencies(dependencyComponentA, dependencyComponentB)
        }
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
                    factory { "my_overridden_value" }
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

    @Test(expected = java.lang.IllegalStateException::class)
    fun testThrowsOnUnknownScoping() {
        Component {
            modules(
                Module {
                    factory(scoping = Scoping.Scoped(name = "unknown")) { }
                }
            )
        }
    }
}