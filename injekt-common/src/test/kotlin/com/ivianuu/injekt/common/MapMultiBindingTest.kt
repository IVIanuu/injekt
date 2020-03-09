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

class MapMultiBindingTest {

    @Test
    fun testMapBinding() {
        val component = Component {
            factory(qualifier = TestQualifier1) { "value_one" }
                .intoMap(
                    entryKey = "key_one",
                    mapKey = keyOf<Map<String, String>>()
                )
            factory(qualifier = TestQualifier2) { "value_two" }
                .intoMap(
                    entryKey = "key_two",
                    mapKey = keyOf<Map<String, String>>()
                )
            factory(qualifier = TestQualifier3) { "value_three" }
                .intoMap(
                    entryKey = "key_three",
                    mapKey = keyOf<Map<String, String>>()
                )
        }

        val map = component.get<Map<String, String>>()
        assertEquals(3, map.size)
        assertEquals(map["key_one"], "value_one")
        assertEquals(map["key_two"], "value_two")
        assertEquals(map["key_three"], "value_three")

        val providerMap = component.get<Map<String, Provider<String>>>()
        assertEquals(3, providerMap.size)
        assertEquals(providerMap.getValue("key_one")(), "value_one")
        assertEquals(providerMap.getValue("key_two")(), "value_two")
        assertEquals(providerMap.getValue("key_three")(), "value_three")

        val lazyMap = component.get<Map<String, Lazy<String>>>()
        assertEquals(3, lazyMap.size)
        assertEquals(lazyMap.getValue("key_one")(), "value_one")
        assertEquals(lazyMap.getValue("key_two")(), "value_two")
        assertEquals(lazyMap.getValue("key_three")(), "value_three")
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnNonDeclaredMapBinding() {
        val component = Component()
        component.get<Map<String, Int>>()
    }

    @Test
    fun testReturnsEmptyOnADeclaredMapBindingWithoutElements() {
        val component = Component {
            map<String, Int>()
        }

        assertEquals(0, component.get<Map<String, Int>>().size)
    }

    @Test
    fun testNestedMapBindings() {
        val componentA = Component {
            factory(qualifier = TestQualifier1) { "value_one" }
                .intoMap(
                    entryKey = "key_one",
                    mapKey = keyOf<Map<String, String>>()
                )
        }

        val mapA = componentA.get<Map<String, String>>()
        assertEquals(1, mapA.size)
        assertEquals("value_one", mapA["key_one"])

        val componentB = Component {
            dependencies(
                Component {
                    dependencies(componentA)
                }
            )
            factory(qualifier = TestQualifier2) { "value_two" }
                .intoMap(
                    entryKey = "key_two",
                    mapKey = keyOf<Map<String, String>>()
                )
        }

        val mapB = componentB.get<Map<String, String>>()
        assertEquals(2, mapB.size)
        assertEquals("value_one", mapA["key_one"])
        assertEquals("value_two", mapB["key_two"])

        val componentC = Component {
            dependencies(
                Component {
                    dependencies(componentB)
                }
            )
            factory(qualifier = TestQualifier3) { "value_three" }
                .intoMap(
                    entryKey = "key_three",
                    mapKey = keyOf<Map<String, String>>()
                )
        }

        val mapC = componentC.get<Map<String, String>>()
        assertEquals(3, mapC.size)
        assertEquals("value_one", mapA["key_one"])
        assertEquals("value_two", mapB["key_two"])
        assertEquals("value_three", mapC["key_three"])
    }

    @Test
    fun testOverride() {
        val component = Component {
            factory(qualifier = TestQualifier1) { "value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>()
                )

            factory(qualifier = TestQualifier2) { "overridden_value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>(),
                    duplicateStrategy = DuplicateStrategy.Permit
                )
        }

        assertEquals(
            "overridden_value",
            component.get<Map<String, String>>()["key"]
        )
    }

    @Test
    fun testOverrideDrop() {
        val component = Component {
            factory(qualifier = TestQualifier1) { "value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>()
                )
            factory(qualifier = TestQualifier2) { "overridden_value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>(),
                    duplicateStrategy = DuplicateStrategy.Drop
                )
        }

        assertEquals(
            "value",
            component.get<Map<String, String>>()["key"]
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        Component {
            factory { "value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>()
                )
            factory { "overridden_value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>(),
                    duplicateStrategy = DuplicateStrategy.Fail
                )
        }
    }

    @Test
    fun testNestedOverride() {
        val componentA = Component {
            factory { "value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>()
                )
        }
        val componentB = Component {
            dependencies(Component {
                dependencies(componentA)
            })
            factory(qualifier = TestQualifier1) { "overridden_value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>(),
                    duplicateStrategy = DuplicateStrategy.Permit
                )
        }

        val mapA = componentA.get<Map<String, String>>()
        assertEquals("value", mapA["key"])
        val mapB = componentB.get<Map<String, String>>()
        assertEquals("overridden_value", mapB["key"])
    }

    @Test
    fun testNestedOverrideDrop() {
        val componentA = Component {
            factory { "value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>()
                )
        }
        val componentB = Component {
            dependencies(Component {
                dependencies(componentA)
            })
            factory(qualifier = TestQualifier1) { "overridden_value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>(),
                    duplicateStrategy = DuplicateStrategy.Drop
                )
        }

        val mapA = componentA.get<Map<String, String>>()
        assertEquals("value", mapA["key"])
        val mapB = componentB.get<Map<String, String>>()
        assertEquals("value", mapB["key"])
    }

    @Test(expected = IllegalStateException::class)
    fun testNestedOverrideFail() {
        val componentA = Component {
            factory { "value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>()
                )
        }
        val componentB = Component {
            dependencies(Component {
                dependencies(componentA)
            })
            factory(qualifier = TestQualifier1) { "overridden_value" }
                .intoMap(
                    entryKey = "key",
                    mapKey = keyOf<Map<String, String>>(),
                    duplicateStrategy = DuplicateStrategy.Fail
                )
        }
    }
}
