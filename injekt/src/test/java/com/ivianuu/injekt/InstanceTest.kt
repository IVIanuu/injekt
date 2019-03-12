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

import com.ivianuu.injekt.util.TestDep1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstanceTest {

    @Test
    fun testSingleCreatesOnce() {
        val component = component()
        component.addBinding(
            Binding.create(
                type = TestDep1::class,
                kind = SingleKind,
                definition = { TestDep1() }
            )
        )

        val instance = component.getInstances().first()

        assertFalse(instance.isCreated)

        val value1 = instance.get(component, null)
        assertTrue(instance.isCreated)
        val value2 = instance.get(component, null)
        assertTrue(instance.isCreated)

        assertEquals(value1, value2)
    }

    @Test
    fun testFactoryCreatesNew() {
        val component = component()
        component.addBinding(
            Binding.create(
                type = TestDep1::class,
                kind = FactoryKind,
                definition = { TestDep1() }
            )
        )

        val instance = component.getInstances().last()

        assertTrue(instance is FactoryInstance)
        assertFalse(instance.isCreated)

        val value1 = instance.get(component, null)
        assertFalse(instance.isCreated)
        val value2 = instance.get(component, null)
        assertFalse(instance.isCreated)

        assertNotEquals(value1, value2)
    }

    @Test
    fun testInstanceCreationFailed() {
        val component = component()
        component.addBinding(
            Binding.create(
                type = TestDep1::class,
                kind = SingleKind,
                definition = { error("error") }
            )
        )

        val instance = component.getInstances().last()

        val throwed = try {
            instance.get(component, null)
            false
        } catch (e: InstanceCreationException) {
            true
        }

        assertTrue(throwed)
    }
}