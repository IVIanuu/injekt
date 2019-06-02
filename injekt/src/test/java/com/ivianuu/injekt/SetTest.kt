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

class SetTest {

    @Test
    fun testSetMultiBinding() {
        val component = component(
            modules = listOf(
                module {
                    factory(NameOne) { "value_one" } bindIntoSet SetValues
                    factory(NameTwo) { "value_two" } bindIntoSet SetValues
                    factory(NameThree) { "value_three" } bindIntoSet SetValues
                }
            )
        )

        val set = component.get<Set<String>>(SetValues)

        assertEquals(3, set.size)
        assertEquals("value_one", set.toList()[0])
        assertEquals("value_two", set.toList()[1])
        assertEquals("value_three", set.toList()[2])

        val lazySet = component.get<Set<Lazy<String>>>(SetValues)

        assertEquals(3, lazySet.size)
        assertEquals("value_one", lazySet.toList()[0].value)
        assertEquals("value_two", lazySet.toList()[1].value)
        assertEquals("value_three", lazySet.toList()[2].value)

        val providerSet = component.get<Set<Provider<String>>>(SetValues)

        assertEquals(3, providerSet.size)
        assertEquals("value_one", providerSet.toList()[0].get())
        assertEquals("value_two", providerSet.toList()[1].get())
        assertEquals("value_three", providerSet.toList()[2].get())
    }

}