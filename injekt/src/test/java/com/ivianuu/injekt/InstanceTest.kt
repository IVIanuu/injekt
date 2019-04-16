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
class InstanceTest {

    @Test
    fun testSingleCreatesOnce() {
        val component = component()
        component.addBinding(
            Binding(
                kind = SingleKind,
                definition = { TestDep1() }
            )
        )

val instance = component.instances.first()

        val value1 = instance.get(component.context, null)
        val value2 = instance.get(component.context, null)

        assertEquals(value1, value2)
    }

    @Test
    fun testFactoryCreatesNew() {
        val component = component()
        component.addBinding(
            Binding(
                kind = FactoryKind,
                definition = { TestDep1() }
            )
        )

        val instance = component.getInstances().last()

        assertTrue(instance is FactoryInstance)

        val value1 = instance.get(component.context, null)
        val value2 = instance.get(component.context, null)

        assertNotEquals(value1, value2)
    }

    @Test
    fun testInstanceCreationFailed() {
        val component = component()
        component.addBinding(
            Binding(
                type = TestDep1::class,
                kind = SingleKind,
                definition = { error("error") }
            )
        )

        val instance = component.getInstances().last()

        val throwed = try {
            instance.get(component.context, null)
            false
        } catch (e: InstanceCreationException) {
            true
        }

        assertTrue(throwed)
    }
}*/