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
            Binding.createSingle(
                type = TestDep1::class,
                definition = { TestDep1() }
            )
        )

        val instance = component.instances.values.first()

        assertFalse(instance.isCreated)

        val value1 = instance.get()
        assertTrue(instance.isCreated)
        val value2 = instance.get()
        assertTrue(instance.isCreated)

        assertEquals(value1, value2)
    }

    @Test
    fun testFactoryCreatesNew() {
        val component = component()
        component.addBinding(
            Binding.createFactory(
                type = TestDep1::class,
                definition = { TestDep1() }
            )
        )

        val instance = component.instances.values.first()

        assertTrue(instance is FactoryInstance)
        assertFalse(instance.isCreated)

        val value1 = instance.get()
        assertFalse(instance.isCreated)
        val value2 = instance.get()
        assertFalse(instance.isCreated)

        assertNotEquals(value1, value2)
    }

    @Test
    fun testInstanceCreationFailed() {
        val component = component()
        component.addBinding(
            Binding.createSingle(
                type = TestDep1::class,
                definition = { error("error") }
            )
        )

        val instance = component.instances.values.first()

        val throwed = try {
            instance.get()
            false
        } catch (e: InstanceCreationException) {
            true
        }

        assertTrue(throwed)
    }
}