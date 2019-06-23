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
    fun testSetBinding() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" }.intoSet<String, CharSequence>(setName = Values)
                    factory(NameTwo) { "value_two" }.intoSet<String, CharSequence>(setName = Values)
                    factory(NameThree) { "value_three" }.intoSet<String, CharSequence>(setName = Values)
                }
            )
        }

        val set = component.get<Set<CharSequence>>(Values)

        assertEquals(3, set.size)
        assertEquals("value_one", set.toList()[0])
        assertEquals("value_two", set.toList()[1])
        assertEquals("value_three", set.toList()[2])

        val providerSet = component.get<Set<Provider<CharSequence>>>(Values)

        assertEquals(3, providerSet.size)
        assertEquals("value_one", providerSet.toList()[0]())
        assertEquals("value_two", providerSet.toList()[1]())
        assertEquals("value_three", providerSet.toList()[2]())
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnNonDeclaredSetBinding() {
        val component = component()
        component.get<Set<String>>()
    }

    @Test
    fun testReturnsEmptyOnADeclaredMapBindingWithoutElements() {
        val component = component {
            modules(
                module {
                    set<String>()
                }
            )
        }

        assertEquals(0, component.get<Set<String>>().size)
    }

    // todo test nested

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnIllegalOverride() {
        component {
            modules(
                module {
                    factory { "value" }.intoSet()
                    factory { "overridden_value" }.intoSet()
                }
            )
        }
    }

    @Test
    fun testOverridesLegalOverride() {
        val originalValueComponent = component {
            modules(
                module { factory { "value" }.intoSet() }
            )
        }
        val overriddenValueComponent = component {
            dependencies(originalValueComponent)
            modules(
                module {
                    factory(override = true) { "overridden_value" }.intoSet(override = true)
                }
            )
        }

        assertEquals("overridden_value", overriddenValueComponent.get<Set<String>>().first())
    }

}