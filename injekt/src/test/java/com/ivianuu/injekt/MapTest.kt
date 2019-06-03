/*
 * Copyright 2018 Manuel Wrage
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
        InjektPlugins.logger = PrintLogger()

        val component = component(
            ApplicationScope,
            modules = listOf(
            module {
                factory(NameOne) { "value_one" } bindIntoMap mapBinding("key_one")
                factory(NameTwo) { "value_two" } bindIntoMap mapBinding("key_two")
                factory(NameThree) { "value_three" } bindIntoMap mapBinding("key_three")
            }
        ))

        val map = component.get<Map<String, String>>()

        assertEquals(3, map.size)
        assertEquals(map["key_one"], "value_one")
        assertEquals(map["key_two"], "value_two")
        assertEquals(map["key_three"], "value_three")

        val lazyMap = component.get<Map<String, Lazy<String>>>()

        assertEquals(3, lazyMap.size)
        assertEquals(lazyMap.getValue("key_one").value, "value_one")
        assertEquals(lazyMap.getValue("key_two").value, "value_two")
        assertEquals(lazyMap.getValue("key_three").value, "value_three")

        val providerMap = component.get<Map<String, Provider<String>>>()

        assertEquals(3, providerMap.size)
        assertEquals(providerMap.getValue("key_one").get(), "value_one")
        assertEquals(providerMap.getValue("key_two").get(), "value_two")
        assertEquals(providerMap.getValue("key_three").get(), "value_three")
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnNonDeclaredMapBinding() {
        val component = component()
        component.get<Map<String, Int>>()
    }

    @Test
    fun testReturnsEmptyOnADeclaredMapBindingWithoutElements() {
        val component = component(
            modules = listOf(
                module {
                    mapBinding<String, Int>()
                }
            )
        )

        assertEquals(0, component.get<Map<String, Int>>().size)
    }

    // todo test nested

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnIllegalOverride() {
        component(
            modules = listOf(
                module {
                    factory { "value" }.bindIntoMap("key")
                    factory { "overridden_value" }.bindIntoMap("key")
                }
            )
        )
    }

    @Test
    fun testOverridesLegalOverride() {
        val component = component(modules = listOf(
            module {
                factory(NameOne) { "value" }.bindIntoMap("key")
                factory(NameTwo) { "overridden_value" }.bindIntoMap("key", override = true)
            }
        ))

        assertEquals(
            "overridden_value",
            component.get<Map<String, String>>()["key"]
        )
    }

}