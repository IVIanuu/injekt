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

class MapMultiBindingTest {

    @Test
    fun testMapBinding() {
        val component = Component {
            map<String, Command>(mapQualifier = TestQualifier1) {
                put("one") { Command1 }
                put("two") { Command2 }
                put("three") { Command3 }
            }
        }

        val map = component.get<Map<String, Command>>(qualifier = TestQualifier1)
        assertEquals(3, map.size)
        assertEquals(map["one"], Command1)
        assertEquals(map["two"], Command2)
        assertEquals(map["three"], Command3)

        val providerMap = component.get<Map<String, Provider<Command>>>(qualifier = TestQualifier1)
        assertEquals(3, providerMap.size)
        assertEquals(providerMap.getValue("one")(), Command1)
        assertEquals(providerMap.getValue("two")(), Command2)
        assertEquals(
            providerMap.getValue("three")(),
            Command3
        )

        val lazyMap = component.get<Map<String, Lazy<Command>>>(qualifier = TestQualifier1)
        assertEquals(3, lazyMap.size)
        assertEquals(lazyMap.getValue("one")(), Command1)
        assertEquals(lazyMap.getValue("two")(), Command2)
        assertEquals(lazyMap.getValue("three")(), Command3)
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsOnNonDeclaredMapBinding() {
        val component = Component()
        component.get<Map<String, Int>>()
    }

    @Test
    fun testReturnsEmptyOnADeclaredMapBindingWithoutElements() {
        val component = Component {
            com.ivianuu.injekt.map<String, Int>()
        }

        assertEquals(0, component.get<Map<String, Int>>().size)
    }

    @Test
    fun testNestedMapBindings() {
        val componentA = Component {
            com.ivianuu.injekt.map<String, Command> { put("one") { Command1 } }
        }

        val mapA = componentA.get<Map<String, Command>>()
        assertEquals(1, mapA.size)
        assertEquals(Command1, mapA["one"])

        val componentB = Component {
            parents(
                Component {
                    parents(componentA)
                }
            )
            com.ivianuu.injekt.map<String, Command> { put("two") { Command2 } }
        }

        val mapB = componentB.get<Map<String, Command>>()
        assertEquals(2, mapB.size)
        assertEquals(Command1, mapA["one"])
        assertEquals(Command2, mapB["two"])

        val componentC = Component {
            parents(
                Component {
                    parents(componentB)
                }
            )
            com.ivianuu.injekt.map<String, Command> { put("three") { Command3 } }
        }

        val mapC = componentC.get<Map<String, Command>>()
        assertEquals(3, mapC.size)
        assertEquals(Command1, mapA["one"])
        assertEquals(Command2, mapB["two"])
        assertEquals(Command3, mapC["three"])
    }

    @Test
    fun testOverride() {
        val component = Component {
            com.ivianuu.injekt.map<String, Command> {
                put("key") { Command1 }
                put(
                    "key",
                    duplicateStrategy = DuplicateStrategy.Override
                ) {
                    Command2
                }
            }
        }

        assertEquals(
            Command2,
            component.get<Map<String, Command>>()["key"]
        )
    }

    @Test
    fun testOverrideDrop() {
        val component = Component {
            com.ivianuu.injekt.map<String, Command> {
                put("key") { Command1 }
                put(
                    "key",
                    duplicateStrategy = DuplicateStrategy.Drop
                ) {
                    Command2
                }
            }
        }

        assertEquals(
            Command1,
            component.get<Map<String, Command>>()["key"]
        )
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        Component {
            com.ivianuu.injekt.map<String, Command> {
                put("key") { Command1 }
                put(
                    "key",
                    duplicateStrategy = DuplicateStrategy.Fail
                ) {
                    Command2
                }
            }
        }
    }

    @Test
    fun testNestedOverride() {
        val componentA = Component {
            com.ivianuu.injekt.factory { Command1 }
            com.ivianuu.injekt.map<String, Command> {
                put(
                    "key",
                    keyOf<Command1>()
                )
            }
        }
        val componentB = Component {
            parents(Component {
                parents(componentA)
            })
            com.ivianuu.injekt.factory { Command2 }
            com.ivianuu.injekt.map<String, Command> {
                put(
                    "key",
                    keyOf<Command2>(),
                    duplicateStrategy = DuplicateStrategy.Override
                )
            }
        }

        val mapA = componentA.get<Map<String, Command>>()
        assertEquals(Command1, mapA["key"])
        val mapB = componentB.get<Map<String, Command>>()
        assertEquals(Command2, mapB["key"])
    }

    @Test
    fun testNestedOverrideDrop() {
        val componentA = Component {
            com.ivianuu.injekt.factory { Command1 }
            com.ivianuu.injekt.map<String, Command> {
                put(
                    "key",
                    keyOf<Command1>()
                )
            }
        }
        val componentB = Component {
            parents(Component {
                parents(componentA)
            })
            com.ivianuu.injekt.factory { Command2 }
            com.ivianuu.injekt.map<String, Command> {
                put(
                    "key",
                    keyOf<Command2>(),
                    duplicateStrategy = DuplicateStrategy.Drop
                )
            }
        }

        val mapA = componentA.get<Map<String, Command>>()
        assertEquals(Command1, mapA["key"])
        val mapB = componentB.get<Map<String, Command>>()
        assertEquals(Command1, mapB["key"])
    }

    @Test(expected = IllegalStateException::class)
    fun testNestedOverrideFail() {
        val componentA = Component {
            com.ivianuu.injekt.factory { Command1 }
            com.ivianuu.injekt.map<String, Command> {
                put(
                    "key",
                    keyOf<Command1>()
                )
            }
        }
        val componentB = Component {
            parents(Component {
                parents(componentA)
            })
            com.ivianuu.injekt.factory { Command2 }
            com.ivianuu.injekt.map<String, Command> {
                put(
                    "key",
                    keyOf<Command2>(),
                    duplicateStrategy = DuplicateStrategy.Fail
                )
            }
        }
    }

    @Test
    fun testEagerBoundBindingDependsOnMapOfProvider() {
        Component {
            com.ivianuu.injekt.factory(behavior = Bound + Eager) {
                get<Map<String, Provider<String>>>()
                    .forEach { it.value() }
            }
            com.ivianuu.injekt.map<String, String>()
        }
    }

    @Test
    fun testReusesMapBuildersInsideAComponentBuilder() {
        val component = Component {
            com.ivianuu.injekt.instance(Command1)
            com.ivianuu.injekt.instance(Command2)
            com.ivianuu.injekt.map<String, Any> {
                put(
                    "a",
                    keyOf<Command1>()
                )
            }
            com.ivianuu.injekt.map<String, Any> {
                put(
                    "b",
                    keyOf<Command2>()
                )
            }
        }

        assertEquals(2, component.get<Map<String, Any>>().size)
    }
}
