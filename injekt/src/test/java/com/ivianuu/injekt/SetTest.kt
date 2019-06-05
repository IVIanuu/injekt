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

/**
class SetTest {

    @Test
    fun testSetBinding() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" }.bindIntoSet(setName = Values)
                    factory(NameTwo) { "value_two" }.bindIntoSet(setName = Values)
                    factory(NameThree) { "value_three" }.bindIntoSet(setName = Values)
                }
            )
        }

        val set = component.get<Set<String>>(Values)

        assertEquals(3, set.size)
        assertEquals("value_one", set.toList()[0])
        assertEquals("value_two", set.toList()[1])
        assertEquals("value_three", set.toList()[2])

        val lazySet = component.get<Set<Lazy<String>>>(Values)

        assertEquals(3, lazySet.size)
        assertEquals("value_one", lazySet.toList()[0].value)
        assertEquals("value_two", lazySet.toList()[1].value)
        assertEquals("value_three", lazySet.toList()[2].value)

        val providerSet = component.get<Set<Provider<String>>>(Values)

        assertEquals(3, providerSet.size)
        assertEquals("value_one", providerSet.toList()[0].get())
        assertEquals("value_two", providerSet.toList()[1].get())
        assertEquals("value_three", providerSet.toList()[2].get())
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
                    bindSet<String>()
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
                    factory { "value" }.bindIntoSet()
                    factory { "overridden_value" }.bindIntoSet()
                }
            )
        }
    }

    /*@Test
    fun testOverridesLegalOverride() {
        val component = component {
            modules(
                module {
                    factory { "value" }.bindIntoSet()
                    factory { "overridden_value" }.bindIntoSet()
                }
            )
        }

        assertEquals("overridden_value", component.get<Set<String>>().first())
    }*/

} */