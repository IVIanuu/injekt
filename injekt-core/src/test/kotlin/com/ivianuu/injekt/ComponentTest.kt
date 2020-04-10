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

        val component = Component {
            bind { instance }
        }

        assertEquals(instance, component.get<TestDep1>())
    }

    @Test
    fun testGetQualified() {
        val instance = TestDep1()

        val component = Component {
            bind(qualifier = TestQualifier1) { instance }
        }

        assertEquals(instance, component.get<TestDep1>(qualifier = TestQualifier1))
    }

    @Test
    fun testGetMultiQualified() {
        val instance = TestDep1()

        val component = Component {
            bind(qualifier = TestQualifier1 + TestQualifier2) { instance }
        }

        assertEquals(instance, component.get<TestDep1>(qualifier = TestQualifier1 + TestQualifier2))
    }

    @Test
    fun testGetNested() {
        val componentA = Component {
            bind { TestDep1() }
        }
        val componentB = Component {
            parents(componentA)
            bind { TestDep2(get()) }
        }

        val componentC = Component {
            parents(componentB)
            bind { TestDep3(get(), get()) }
        }

        componentC.get<TestDep3>()
    }

    @Test(expected = IllegalStateException::class)
    fun testGetUnknownInstanceThrows() {
        val component = Component()
        component.get<Int>()
    }

    @Test
    fun testGetNullableInstanceReturnsNonNullable() {
        val component = Component {
            bind { "string" }
        }
        assertEquals("string", component.get<String?>())
    }

    @Test(expected = IllegalStateException::class)
    fun testGetNonNullableNotReturnsNullable() {
        val component = Component {
            bind<String?> { null }
        }

        component.get<String>()
    }

    @Test
    fun testGetUnknownNullableInstanceReturnsNull() {
        val component = Component()
        assertNull(component.get<String?>())
    }

    @Test
    fun testTypeDistinction() {
        val component = Component {
            bind { listOf(1, 2, 3) }
            bind { listOf("one", "two", "three") }
        }

        val ints = component.get<List<Int>>()
        val strings = component.get<List<String>>()

        assertEquals(listOf(1, 2, 3), ints)
        assertEquals(listOf("one", "two", "three"), strings)
        assertNotSame(ints, strings)
    }

    @Test
    fun testInstantiatesBindingWithRequestingComponent() {
        val componentA = Component { bind { this } }
        val componentB = Component { parents(componentA) }
        val componentC = Component { parents(componentB) }

        val componentAResult = componentA.get<Component>()
        val componentBResult = componentB.get<Component>()
        val componentCResult = componentC.get<Component>()

        assertEquals(componentAResult, componentAResult)
        assertEquals(componentB, componentBResult)
        assertEquals(componentC, componentCResult)
    }

}
