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

package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.component
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import junit.framework.Assert.assertEquals
import org.junit.Test

class MapTest {

    @Test
    fun testMapMultiBinding() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" } bindIntoMap (Values to "key_one")
                    factory(NameTwo) { "value_two" } bindIntoMap (Values to "key_two")
                    factory(NameThree) { "value_three" } bindIntoMap (Values to "key_three")
                }
            )
        }

        val map = component.getMap<String, String>(Values)

        assertEquals(3, map.size)
        assertEquals(map["key_one"], "value_one")
        assertEquals(map["key_two"], "value_two")
        assertEquals(map["key_three"], "value_three")
    }

    @Test
    fun testOverride() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" } bindIntoMap (Values to "key_one")
                    factory(NameTwo) { "value_two" } bindIntoMap MapBinding(
                        Values,
                        "key_one"
                    )
                }
            )
        }

        assertEquals("value_two", component.getMap<String, String>(Values)["key_one"])
    }

}