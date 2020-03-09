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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Component
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.keyOf
import junit.framework.Assert.assertEquals
import org.junit.Test

class SetMultiBindingTest {

    @Test
    fun testSetBinding() {
        val component = Component {
            factory(qualifier = TestQualifier1) { "value_one" }
                .intoSet(setKey = keyOf<Set<String>>())
            factory(qualifier = TestQualifier2) { "value_two" }
                .intoSet(setKey = keyOf<Set<String>>())
            factory(qualifier = TestQualifier3) { "value_three" }
                .intoSet(setKey = keyOf<Set<String>>())
        }

        val set = component.get<Set<String>>()
        assertEquals(3, set.size)
        assertEquals("value_one", set.toList()[0])
        assertEquals("value_two", set.toList()[1])
        assertEquals("value_three", set.toList()[2])

        val providerSet = component.get<Set<Provider<String>>>()
        assertEquals(3, providerSet.size)
        assertEquals("value_one", providerSet.toList()[0]())
        assertEquals("value_two", providerSet.toList()[1]())
        assertEquals("value_three", providerSet.toList()[2]())

        val lazySet = component.get<Set<Lazy<String>>>()
        assertEquals(3, providerSet.size)
        assertEquals("value_one", lazySet.toList()[0]())
        assertEquals("value_two", lazySet.toList()[1]())
        assertEquals("value_three", lazySet.toList()[2]())
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnNonDeclaredSetBinding() {
        val component = Component()
        component.get<Set<String>>()
    }

    @Test
    fun testReturnsEmptyOnADeclaredSetBindingWithoutElements() {
        val component = Component {
            set<String>()
        }

        assertEquals(0, component.get<Set<String>>().size)
    }

    @Test
    fun testNestedSetBindings() {
        val componentA = Component {
            factory(qualifier = TestQualifier1) { "value_one" }
                .intoSet(setKey = keyOf<Set<String>>())
        }

        val setA = componentA.get<Set<String>>()
        assertEquals(1, setA.size)
        assertEquals("value_one", setA.toList()[0])

        val componentB = Component {
            dependencies(componentA)
            factory(qualifier = TestQualifier2) { "value_two" }
                .intoSet(setKey = keyOf<Set<String>>())
        }

        val setB = componentB.get<Set<String>>()
        assertEquals(2, setB.size)
        assertEquals("value_one", setA.toList()[0])
        assertEquals("value_two", setB.toList()[1])

        val componentC = Component {
            dependencies(componentB)
            factory(qualifier = TestQualifier3) { "value_three" }
                .intoSet(setKey = keyOf<Set<String>>())
        }

        val setC = componentC.get<Set<String>>()
        assertEquals(3, setC.size)
        assertEquals("value_one", setA.toList()[0])
        assertEquals("value_two", setB.toList()[1])
        assertEquals("value_three", setC.toList()[2])
    }

    @Test
    fun testOverride() {
        val originalValueComponent = Component {
            factory { "value" }
                .intoSet(setKey = keyOf<Set<String>>())
        }
        val overriddenValueComponent = Component {
            dependencies(originalValueComponent)
            factory(duplicateStrategy = DuplicateStrategy.Override) { "overridden_value" }
                .intoSet(
                    setKey = keyOf<Set<String>>(),
                    duplicateStrategy = DuplicateStrategy.Override
                )
        }

        assertEquals("overridden_value", overriddenValueComponent.get<Set<String>>().single())
    }

    @Test
    fun testOverrideDrop() {
        val originalValueComponent = Component {
            factory { "value" }
                .intoSet(setKey = keyOf<Set<String>>())
        }
        val overriddenValueComponent = Component {
            dependencies(originalValueComponent)
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "overridden_value" }
                .intoSet(setKey = keyOf<Set<String>>(), duplicateStrategy = DuplicateStrategy.Drop)
        }

        assertEquals("value", overriddenValueComponent.get<Set<String>>().single())
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        Component {
            factory { "value" }
                .intoSet(setKey = keyOf<Set<String>>())
            factory { "overridden_value" }
                .intoSet(setKey = keyOf<Set<String>>(), duplicateStrategy = DuplicateStrategy.Fail)
        }
    }

    @Test
    fun testNestedOverride() {
        val componentA = Component {
            factory { "value" }
                .intoSet(setKey = keyOf<Set<String>>())
        }
        val componentB = Component {
            dependencies(componentA)
            factory(duplicateStrategy = DuplicateStrategy.Override) { "overridden_value" }
                .intoSet(
                    setKey = keyOf<Set<String>>(),
                    duplicateStrategy = DuplicateStrategy.Override
                )
        }

        val setA = componentA.get<Set<String>>()
        assertEquals("value", setA.toList()[0])
        val setB = componentB.get<Set<String>>()
        assertEquals("overridden_value", setB.toList()[0])
    }

    @Test
    fun testNestedOverrideDrop() {
        val componentA = Component {
            factory { "value" }
                .intoSet(setKey = keyOf<Set<String>>())
        }
        val componentB = Component {
            dependencies(componentA)
            factory(duplicateStrategy = DuplicateStrategy.Drop) { "overridden_value" }
                .intoSet(setKey = keyOf<Set<String>>(), duplicateStrategy = DuplicateStrategy.Drop)
        }

        val setA = componentA.get<Set<String>>()
        assertEquals("value", setA.toList()[0])
        val setB = componentB.get<Set<String>>()
        assertEquals("value", setB.toList()[0])
    }

    @Test(expected = IllegalStateException::class)
    fun testNestedOverrideFail() {
        val componentA = Component {
            factory { "value" }
                .intoSet(setKey = keyOf<Set<String>>())
        }
        val componentB = Component {
            dependencies(componentA)
            factory(duplicateStrategy = DuplicateStrategy.Fail) { "overridden_value" }
                .intoSet(setKey = keyOf<Set<String>>(), duplicateStrategy = DuplicateStrategy.Fail)
        }
    }
}
