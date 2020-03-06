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

class MapTest {

    @Test
    fun testMapBinding() {
        val component = Component {
            factory(name = NameOne) { "value_one" }
                .intoMap<String, CharSequence>("key_one", mapName = Values)
            factory(name = NameTwo) { "value_two" }
                .intoMap<String, CharSequence>("key_two", mapName = Values)
            factory(name = NameThree) { "value_three" }
                .intoMap<String, CharSequence>("key_three", mapName = Values)
        }

        val map = component.get<Map<String, CharSequence>>(Values)
        assertEquals(3, map.size)
        assertEquals(map["key_one"], "value_one")
        assertEquals(map["key_two"], "value_two")
        assertEquals(map["key_three"], "value_three")

        val providerMap = component.get<Map<String, Provider<CharSequence>>>(Values)
        assertEquals(3, providerMap.size)
        assertEquals(providerMap.getValue("key_one")(), "value_one")
        assertEquals(providerMap.getValue("key_two")(), "value_two")
        assertEquals(providerMap.getValue("key_three")(), "value_three")

        val lazyMap = component.get<Map<String, Lazy<CharSequence>>>(Values)
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
            factory(name = NameOne) { "value_one" }
                .intoMap<String, String>("key_one")
        }

        val mapA = componentA.get<Map<String, String>>()
        assertEquals(1, mapA.size)
        assertEquals("value_one", mapA["key_one"])

        val componentB = Component {
            dependencies(componentA)
            factory(name = NameTwo) { "value_two" }
                .intoMap<String, String>("key_two")
        }

        val mapB = componentB.get<Map<String, String>>()
        assertEquals(2, mapB.size)
        assertEquals("value_one", mapA["key_one"])
        assertEquals("value_two", mapB["key_two"])

        val componentC = Component {
            dependencies(componentB)
            factory(name = NameThree) { "value_three" }
                .intoMap<String, String>("key_three")
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
            factory(name = NameOne) { "value" }
                .intoMap<String, String>("key")
            factory(name = NameTwo) { "overridden_value" }
                .intoMap<String, String>(
                    "key",
                    overrideStrategy = OverrideStrategy.Override
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
            factory(name = NameOne) { "value" }
                .intoMap<String, String>("key")
            factory(name = NameTwo) { "overridden_value" }
                .intoMap<String, String>("key", overrideStrategy = OverrideStrategy.Drop)
        }

        assertEquals(
            "value",
            component.get<Map<String, String>>()["key"]
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        Component {
            factory { "value" }.intoMap<String, String>("key")
            factory { "overridden_value" }.intoMap<String, String>("key")
        }
    }

    @Test
    fun testNestedOverride() {
        val componentA = Component {
            factory { "value" }
                .intoMap<String, String>("key")
        }
        val componentB = Component {
            dependencies(componentA)
            factory(name = NameOne) { "overridden_value" }
                .intoMap<String, String>(
                    "key",
                    overrideStrategy = OverrideStrategy.Override
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
                .intoMap<String, String>("key")
        }
        val componentB = Component {
            dependencies(componentA)
            factory(name = NameOne) { "overridden_value" }
                .intoMap<String, String>("key", overrideStrategy = OverrideStrategy.Drop)
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
                .intoMap<String, String>("key")
        }
        val componentB = Component {
            dependencies(componentA)
            factory(name = NameOne) { "overridden_value" }
                .intoMap<String, String>("key")
        }
    }
}
