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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Bound
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Eager
import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import junit.framework.Assert.assertEquals
import org.junit.Test

class MapMultiBindingTest {

    @Test
    fun testMapBinding() {
        val component = Component {
            factory { Command1 }
            factory { Command2 }
            factory { Command3 }
            map<String, Command>(mapQualifier = TestQualifier1) {
                put<Command1>("one")
                put<Command2>("two")
                put<Command3>("three")
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
        assertEquals(providerMap.getValue("three")(), Command3)

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
            map<String, Int>()
        }

        assertEquals(0, component.get<Map<String, Int>>().size)
    }

    @Test
    fun testNestedMapBindings() {
        val componentA = Component {
            factory { Command1 }
            map<String, Command> { put<Command1>("one") }
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
            factory { Command2 }
            map<String, Command> { put<Command2>("two") }
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
            factory { Command3 }
            map<String, Command> { put<Command3>("three") }
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
            factory { Command1 }
            factory { Command2 }
            map<String, Command> {
                put<Command1>("key")
                put<Command2>("key", duplicateStrategy = DuplicateStrategy.Override)
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
            factory { Command1 }
            factory { Command2 }
            map<String, Command> {
                put<Command1>("key")
                put<Command2>("key", duplicateStrategy = DuplicateStrategy.Drop)
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
            factory { Command1 }
            factory { Command2 }
            map<String, Command> {
                put<Command1>("key")
                put<Command2>("key", duplicateStrategy = DuplicateStrategy.Fail)
            }
        }
    }

    @Test
    fun testNestedOverride() {
        val componentA = Component {
            factory { Command1 }
            map<String, Command> { put<Command1>("key") }
        }
        val componentB = Component {
            parents(Component {
                parents(componentA)
            })
            factory { Command2 }
            map<String, Command> {
                put<Command2>(
                    "key",
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
            factory { Command1 }
            map<String, Command> { put<Command1>("key") }
        }
        val componentB = Component {
            parents(Component {
                parents(componentA)
            })
            factory { Command2 }
            map<String, Command> {
                put<Command2>(
                    "key",
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
            factory { Command1 }
            map<String, Command> { put<Command1>("key") }
        }
        val componentB = Component {
            parents(Component {
                parents(componentA)
            })
            factory { Command2 }
            map<String, Command> {
                put<Command2>(
                    "key",
                    duplicateStrategy = DuplicateStrategy.Fail
                )
            }
        }
    }

    @Test
    fun testEagerBoundBindingDependsOnMapOfProvider() {
        Component {
            factory(tag = Bound + Eager) {
                get<Map<String, Provider<String>>>()
                    .forEach { it.value() }
            }
            map<String, String>()
        }
    }

    @Test
    fun testReusesMapBuildersInsideAComponentBuilder() {
        val component = Component {
            map<String, Any> { put<TestDep1>("a") }
            map<String, Any> { put<TestDep2>("b") }
        }

        assertEquals(2, component.get<Map<String, Any>>().size)
    }
}
