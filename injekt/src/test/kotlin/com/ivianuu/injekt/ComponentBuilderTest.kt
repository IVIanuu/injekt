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
            scopes(TestScope1)
        }

        Component {
            scopes(TestScope1)
            parents(parent)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnParentsWithSameScope() {
        val parent1 = Component {
            scopes(TestScope1)
        }

        val parent2 = Component {
            scopes(TestScope1)
        }

        Component {
            scopes(TestScope2)
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

    @Test
    fun testOverride() {
        val component = Component {
            factory { "my_value" }
            factory(duplicateStrategy = DuplicateStrategy.Override) { "my_overridden_value" }
        }

        assertEquals("my_overridden_value", component.get<String>())
    }

    @Test
    fun testOverrideDrop() {
        val component = Component {
            factory { "my_value" }
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "my_overridden_value" }
        }

        assertEquals("my_value", component.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        Component {
            factory { "my_value" }
            factory { "my_overridden_value" }
        }
    }

    @Test
    fun testNestedOverride() {
        val parentComponent = Component {
            factory { "my_value" }
        }

        val childComponent = Component {
            parents(parentComponent)
            factory(duplicateStrategy = DuplicateStrategy.Override) { "my_overridden_value" }
        }

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_overridden_value", childComponent.get<String>())
    }

    @Test
    fun testNestedOverrideDrop() {
        val parentComponent = Component {
            factory { "my_value" }
        }

        val childComponent = Component {
            parents(parentComponent)
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "my_overridden_value" }
        }

        assertEquals("my_value", parentComponent.get<String>())
        assertEquals("my_value", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testNestedOverrideFail() {
        val parentComponent = Component {
            factory { "my_value" }
        }

        val childComponent = Component {
            parents(parentComponent)
            factory(duplicateStrategy = DuplicateStrategy.Fail) { "my_overridden_value" }
        }
    }

    @Test
    fun testParentOverride() {
        val parentComponentA = Component {
            factory { "value_a" }
        }
        val parentComponentB = Component {
            factory(duplicateStrategy = DuplicateStrategy.Override) { "value_b" }
        }

        val childComponent = Component {
            parents(parentComponentA, parentComponentB)
        }

        assertEquals("value_b", childComponent.get<String>())
    }

    // todo how should we fix this
    //@Test
    fun testParentOverrideDrop() {
        val parentComponentA = Component {
            factory { "value_a" }
        }
        val parentComponentB = Component {
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "value_b" }
        }

        val childComponent = Component {
            parents(parentComponentA, parentComponentB)
        }

        assertEquals("value_a", childComponent.get<String>())
    }

    @Test(expected = IllegalStateException::class)
    fun testParentOverrideFail() {
        val parentComponentA = Component {
            factory { "value_a" }
        }
        val parentComponentB = Component {
            factory(duplicateStrategy = DuplicateStrategy.Fail) { "value_b" }
        }

        val childComponent = Component {
            parents(parentComponentA, parentComponentB)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun testReverseParentOverrideFail() {
        val parentComponentA = Component {
            factory(duplicateStrategy = DuplicateStrategy.Override) { "value_a" }
        }
        val parentComponentB = Component {
            factory(duplicateStrategy = DuplicateStrategy.Fail) { "value_b" }
        }

        val childComponent = Component {
            parents(parentComponentA, parentComponentB)
        }
    }
}
