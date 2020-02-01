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

}