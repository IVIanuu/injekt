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

import com.ivianuu.injekt.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class SetTest {

    @Test
    fun testSetMultiBinding() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" } bindIntoSet Values
                    factory(NameTwo) { "value_two" } bindIntoSet Values
                    factory(NameThree) { "value_three" } bindIntoSet Values
                }
            )
        }

        val set = component.getSet<String>(Values)

        assertEquals(3, set.size)
        assertTrue(set.contains("value_one"))
        assertTrue(set.contains("value_two"))
        assertTrue(set.contains("value_three"))
    }

    @Test
    fun testOverride() {
        val component1 = component {
            modules(
                module {
                    factory { "my_value" } bindIntoSet Values
                }
            )
        }

        val component2 = component {
            dependencies(component1)
            modules(
                module {
                    factory { "my_overridden_value" } bindIntoSet SetBinding(Values, true)
                }
            )
        }

        assertEquals("my_overridden_value", component2.getSet<String>(Values).first())
    }

    @Test
    fun testAllowValidOverride() {
        val component1 = component {
            modules(
                module {
                    factory { "my_value" } bindIntoSet Values
                }
            )
        }

        val component2 = component {
            dependencies(component1)
            modules(
                module {
                    factory { "my_value" } bindIntoSet SetBinding(Values, override = true)
                }
            )
        }

        var throwed = false

        try {
            component2.getSet<String>(Values)
        } catch (e: Exception) {
            throwed = true
        }

        assertFalse(throwed)
    }

    /*@Test
    fun testDisallowInvalidOverride() {
        val component1 = component {
            modules(
                module {
                    factory { "my_value" } bindIntoSet "values"
                }
            )
        }

        val component2 = component {
            dependencies(component1)
            modules(
                module {
                    factory { "my_value" } bindIntoSet "values"
                }
            )
        }

        var throwed = false

        try {
            component2.getSet<String>("values")
        } catch (e: Exception) {
            throwed = true
        }

        assertTrue(throwed)
    }*/

}