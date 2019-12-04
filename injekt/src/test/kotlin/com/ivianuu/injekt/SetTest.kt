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

class SetTest {

    @Test
    fun testSetBinding() {
        val component = component {
            modules(
                module {
                    factory(NameOne) { "value_one" }.intoSet<CharSequence>(setName = Values)
                    factory(NameTwo) { "value_two" }.intoSet<CharSequence>(setName = Values)
                    factory(NameThree) { "value_three" }.intoSet<CharSequence>(setName = Values)
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

        val lazySet = component.get<Set<Lazy<CharSequence>>>(Values)
        assertEquals(3, providerSet.size)
        assertEquals("value_one", lazySet.toList()[0]())
        assertEquals("value_two", lazySet.toList()[1]())
        assertEquals("value_three", lazySet.toList()[2]())
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

    @Test
    fun testNestedMapBindings() {
        val componentA = component {
            modules(
                module {
                    factory(NameOne) { "value_one" }
                        .intoSet<String>()
                }
            )
        }

        val setA = componentA.get<Set<String>>()
        assertEquals(1, setA.size)
        assertEquals("value_one", setA.toList()[0])

        val componentB = component {
            dependencies(componentA)

            modules(
                module {
                    factory(NameTwo) { "value_two" }
                        .intoSet<String>()
                }
            )
        }

        val setB = componentB.get<Set<String>>()
        assertEquals(2, setB.size)
        assertEquals("value_one", setA.toList()[0])
        assertEquals("value_two", setB.toList()[1])

        val componentC = component {
            dependencies(componentB)

            modules(
                module {
                    factory(NameThree) { "value_three" }
                        .intoSet<String>()
                }
            )
        }

        val setC = componentC.get<Set<String>>()
        assertEquals(3, setC.size)
        assertEquals("value_one", setA.toList()[0])
        assertEquals("value_two", setB.toList()[1])
        assertEquals("value_three", setC.toList()[2])
    }

    @Test
    fun testOverridesLegalOverride() {
        val originalValueComponent = component {
            modules(
                module { factory { "value" }.intoSet<String>() }
            )
        }
        val overriddenValueComponent = component {
            dependencies(originalValueComponent)
            modules(
                module {
                    factory(override = true) { "overridden_value" }.intoSet<String>(override = true)
                }
            )
        }

        assertEquals("overridden_value", overriddenValueComponent.get<Set<String>>().single())
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnIllegalOverride() {
        component {
            modules(
                module {
                    factory { "value" }.intoSet<String>()
                    factory { "overridden_value" }.intoSet<String>()
                }
            )
        }
    }

    @Test
    fun testOverridesLegalNestedOverride() {
        val componentA = component {
            modules(
                module {
                    factory { "value" }
                        .intoSet<String>()
                }
            )
        }
        val componentB = component {
            dependencies(componentA)

            modules(
                module {
                    factory(override = true) { "overridden_value" }
                        .intoSet<String>(override = true)
                }
            )
        }

        val setA = componentA.get<Set<String>>()
        assertEquals("value", setA.toList()[0])
        val setB = componentB.get<Set<String>>()
        assertEquals("overridden_value", setB.toList()[0])
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnIllegalNestedOverride() {
        val componentA = component {
            modules(
                module {
                    factory { "value" }
                        .intoSet<String>()
                }
            )
        }
        val componentB = component {
            dependencies(componentA)

            modules(
                module {
                    factory(override = true) { "overridden_value" }
                        .intoSet<String>(override = false)
                }
            )
        }
    }
}
