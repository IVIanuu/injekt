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
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import org.junit.Test

class ComponentTest {

    @Test
    fun testGet() {
        val instance = TestDep1()
        val component = Component(Module {
            factory { instance }
        })
        assertEquals(instance, component.get<TestDep1>())
    }

    @Test
    fun testGetQualified() {
        val instance = TestDep1()
        val component = Component(Module {
            factory { TestDep1() }
            factory(TestQualifier1::class) { instance }
        })
        assertEquals(instance, component.get<TestDep1>(qualifier = TestQualifier1::class))
    }

    @Test
    fun testGetNested() {
        val componentA = Component(
            Module {
                factory { TestDep1() }
            }
        )
        val componentB = componentA.plus<TestScope1>(Module {
            factory { TestDep2(get()) }
        })

        val componentC = componentB.plus<TestScope2>(Module {
            factory { TestDep3(get(), get()) }
        })

        componentC.get<TestDep3>()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetUnknownInstanceThrows() {
        val component = Component()
        component.get<Int>()
    }

    @Test
    fun testGetNullableInstanceReturnsNonNullable() {
        val component = Component(Module {
            factory { "string" }
        })
        assertEquals("string", component.get<String?>())
    }

    @Test(expected = IllegalStateException::class)
    fun testGetNonNullableNotReturnsNullable() {
        val component = Component(Module {
            factory<String?> { null }
        })
        component.get<String>()
    }

    @Test
    fun testGetUnknownNullableInstanceReturnsNull() {
        val component = Component()
        assertNull(component.get<String?>())
    }

    @Test
    fun testTypeDistinction() {
        val component = Component(Module {
            factory { listOf(1, 2, 3) }
            factory { listOf("one", "two", "three") }
        })

        val ints = component.get<List<Int>>()
        val strings = component.get<List<String>>()

        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf("one", "two", "three"), strings)
        assertNotSame(ints, strings)
    }

    /*@Test
    fun testInstantiatesBindingWithRequestingComponent() {
        val componentA = Component { factory { this } }
        val componentB = Component { parents(componentA) }
        val componentC = Component { parents(componentB) }

        val componentAResult = componentA.get<Component>()
        val componentBResult = componentB.get<Component>()
        val componentCResult = componentC.get<Component>()

        assertEquals(componentAResult, componentAResult)
        assertEquals(componentB, componentBResult)
        assertEquals(componentC, componentCResult)
    }*/

    @Test(expected = IllegalStateException::class)
    fun testThrowsWhenOverridingScope() {
        val parent = Component<TestScope1>()
        parent.plus<TestScope1>()
    }

    @Test(expected = IllegalStateException::class)
    fun testOverride() {
        Component(Module {
            factory { "my_value" }
            factory { "my_overridden_value" }
        })
    }

    @Test(expected = IllegalStateException::class)
    fun testNestedOverride() {
        val parentComponent = Component(Module {
            factory { "my_value" }
        })

        parentComponent.plus<TestScope1>(Module {
            factory { "my_overridden_value" }
        })
    }

    @Test
    fun testImplicitComponentBindings() {
        val componentA = Component<TestScope1>()
        val componentB = componentA.plus<TestScope2>()

        assertEquals(componentA, componentA.get<Component>())
        assertEquals(
            componentA,
            componentA.get<Component>(qualifier = TestScope1::class)
        )

        assertEquals(componentB, componentB.get<Component>())
        assertEquals(
            componentB,
            componentB.get<Component>(qualifier = TestScope2::class)
        )
        assertEquals(
            componentA,
            componentB.get<Component>(qualifier = TestScope1::class)
        )
    }

    @Test
    fun testScoped() {
        val componentA = Component(Module {
            scoped { TestDep1() }
        })

        val componentB = componentA.plus<TestScope1>()
        val componentC = componentB.plus<TestScope2>()

        val depA = componentA.get<TestDep1>()
        val depA2 = componentA.get<TestDep1>()
        val depB = componentB.get<TestDep1>()
        val depC = componentC.get<TestDep1>()

        assertEquals(depA, depA2)
        assertEquals(depA, depB)
        assertEquals(depA, depC)
    }

}
