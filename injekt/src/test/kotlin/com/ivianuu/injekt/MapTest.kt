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
import org.junit.Test

class MapTest {

    @Test
    fun testMapBinding() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" }
                        .intoMap<String, String, CharSequence>("key_one", mapName = Values)
                    factory(NameTwo) { "value_two" }
                        .intoMap<String, String, CharSequence>("key_two", mapName = Values)
                    factory(NameThree) { "value_three" }
                        .intoMap<String, String, CharSequence>("key_three", mapName = Values)
                }
            )
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
        val component = component()
        component.get<Map<String, Int>>()
    }

    @Test
    fun testReturnsEmptyOnADeclaredMapBindingWithoutElements() {
        val component = component {
            modules(
                module {
                    map<String, Int>()
                }
            )
        }

        assertEquals(0, component.get<Map<String, Int>>().size)
    }

    @Test
    fun testNestedMapBindings() {
        val componentA = component {
            modules(
                module {
                    factory(NameOne) { "value_one" }
                        .intoMap<String, String, String>("key_one")
                }
            )
        }

        val mapA = componentA.get<Map<String, String>>()
        assertEquals(1, mapA.size)
        assertEquals("value_one", mapA["key_one"])

        val componentB = component {
            dependencies(componentA)

            modules(
                module {
                    factory(NameTwo) { "value_two" }
                        .intoMap<String, String, String>("key_two")
                }
            )
        }

        val mapB = componentB.get<Map<String, String>>()
        assertEquals(2, mapB.size)
        assertEquals("value_one", mapA["key_one"])
        assertEquals("value_two", mapB["key_two"])

        val componentC = component {
            dependencies(componentB)

            modules(
                module {
                    factory(NameThree) { "value_three" }
                        .intoMap<String, String, String>("key_three")
                }
            )
        }

        val mapC = componentC.get<Map<String, String>>()
        assertEquals(3, mapC.size)
        assertEquals("value_one", mapA["key_one"])
        assertEquals("value_two", mapB["key_two"])
        assertEquals("value_three", mapC["key_three"])
    }

    @Test
    fun testOverridesLegalOverride() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value" }
                        .intoMap<String, String, String>("key")
                    factory(NameTwo) { "overridden_value" }
                        .intoMap<String, String, String>("key", override = true)
                }
            )
        }

        assertEquals(
            "overridden_value",
            component.get<Map<String, String>>()["key"]
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnIllegalOverride() {
        component {
            module {
                factory { "value" }.intoMap<String, String, String>("key")
                factory { "overridden_value" }.intoMap<String, String, String>("key")
            }
        }
    }

    @Test
    fun testOverridesLegalNestedOverride() {
        val componentA = component {
            modules(
                module {
                    factory { "value" }
                        .intoMap<String, String, String>("key")
                }
            )
        }
        val componentB = component {
            dependencies(componentA)

            modules(
                module {
                    factory(NameOne) { "overridden_value" }
                        .intoMap<String, String, String>("key", override = true)
                }
            )
        }

        val mapA = componentA.get<Map<String, String>>()
        assertEquals("value", mapA["key"])
        val mapB = componentB.get<Map<String, String>>()
        assertEquals("overridden_value", mapB["key"])
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnIllegalNestedOverride() {
        val componentA = component {
            modules(
                module {
                    factory { "value" }
                        .intoMap<String, String, String>("key")
                }
            )
        }
        val componentB = component {
            dependencies(componentA)

            modules(
                module {
                    factory(NameOne) { "overridden_value" }
                        .intoMap<String, String, String>("key")
                }
            )
        }
    }

}