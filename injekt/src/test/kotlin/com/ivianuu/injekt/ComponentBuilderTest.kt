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

import org.junit.Assert.assertEquals
import org.junit.Test

class ComponentBuilderTest {

    @Test(expected = IllegalStateException::class)
    fun testThrowsWhenOverridingScope() {
        val parent = Component {
            scopes(TestScopeOne)
        }

        Component {
            scopes(TestScopeOne)
            parents(parent)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnParentsWithSameScope() {
        val parent1 = Component {
            scopes(TestScopeOne)
        }

        val parent2 = Component {
            scopes(TestScopeOne)
        }

        Component {
            scopes(TestScopeTwo)
            parents(parent1, parent2)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsNestedImplicitOverride() {
        val componentA = Component {
            factory { "my_value" }
        }

        Component {
            parents(componentA)
            factory { "my_overridden_value" }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsIfParentsOverrideEachOther() {
        val parent1 = Component {
            factory { "value_1" }
        }

        val parent2 = Component {
            factory { "value_2" }
        }

        Component { parents(parent1, parent2) }
    }

    @Test
    fun testBind() {
        val binding = Binding(key = keyOf()) { "value" }
        val component = Component { bind(binding) }
        assertEquals(binding, component.bindings[keyOf<String>()])
    }
}
