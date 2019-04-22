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
            module {
                factoryBuilder<String>(NameOne) {
                    definition { "value_one" }
                    bindIntoSet(setValues)
                }
                factoryBuilder<String>(NameTwo) {
                    definition { "value_two" }
                    bindIntoSet(setValues)
                }
                factoryBuilder<String>(NameThree) {
                    definition { "value_three" }
                    bindIntoSet(setValues)
                }
            }
        }

        val set = component.getSet(setValues)

        assertEquals(3, set.size)
        assertEquals("value_one", set.toList()[0])
        assertEquals("value_two", set.toList()[1])
        assertEquals("value_three", set.toList()[2])

        val lazySet = component.getLazySet(setValues)

        assertEquals(3, lazySet.size)
        assertEquals("value_one", lazySet.toList()[0].value)
        assertEquals("value_two", lazySet.toList()[1].value)
        assertEquals("value_three", lazySet.toList()[2].value)

        val providerSet = component.getProviderSet(setValues)

        assertEquals(3, providerSet.size)
        assertEquals("value_one", providerSet.toList()[0].get())
        assertEquals("value_two", providerSet.toList()[1].get())
        assertEquals("value_three", providerSet.toList()[2].get())
    }

    @Test
    fun testOverride() {
        val component1 = component {
            module {
                factoryBuilder<String>(NameOne) {
                    definition { "value_one" }
                    bindIntoSet(setValues)
                }
            }
        }

        val component2 = component {
            dependencies(component1)
            module {
                factoryBuilder<String>(NameTwo) {
                    definition { "value_two" }
                    bindIntoSet(setValues, true)
                }
            }
        }

        assertEquals("my_overridden_value", component2.getSet(setValues).first())
    }

    @Test
    fun testAllowValidOverride() {
        val component1 = component {
            module {
                factoryBuilder<String>(NameOne) {
                    definition { "value_one" }
                    bindIntoSet(setValues)
                }
            }
        }

        val component2 = component {
            dependencies(component1)
            module {
                factoryBuilder<String>(NameTwo) {
                    definition { "value_two" }
                    bindIntoSet(setValues, true)
                }
            }
        }

        var throwed = false

        try {
            component2.getSet(setValues)
        } catch (e: Exception) {
            throwed = true
        }

        assertFalse(throwed)
    }

    @Test
    fun testDisallowInvalidOverride() {
        val component1 = component {
            module {
                factoryBuilder<String>(NameOne) {
                    definition { "value_one" }
                    bindIntoSet(setValues)
                }
            }
        }

        val component2 = component {
            dependencies(component1)
            module {
                factoryBuilder<String>(NameTwo) {
                    definition { "value_two" }
                    bindIntoSet(setValues, true)
                }
            }
        }

        var throwed = false
        try {
            component2.getSet(setValues)
        } catch (e: Exception) {
            throwed = true
        }
        assertTrue(throwed)
    }


}