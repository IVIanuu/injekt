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
import com.ivianuu.injekt.util.getBinding
import com.ivianuu.injekt.util.getInstance
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class EagerTest {

    @Test
    fun testEagerSingleIsCreatedAtStart() {
        val component = component {
            modules(
                module {
                    single(eager = true) { TestDep1() }
                }
            )
        }

        val binding = component.getBinding<TestDep1>()
        val instance = component.getInstance<TestDep1>(
            binding.type, binding.qualifier
        )

        assertTrue(binding.eager)
        assertTrue(instance.isCreated)
    }

    @Test
    fun testNonEagerSingleIsNotCreatedAtStart() {
        val component = component {
            modules(
                module {
                    single(eager = false) { TestDep1() }
                }
            )
        }

        val binding = component.getBinding<TestDep1>()
        val instance = component.getInstance<TestDep1>(
            binding.type, binding.qualifier
        )

        assertFalse(binding.eager)
        assertFalse(instance.isCreated)
    }

    @Test
    fun testFactoryIsNotCreatedAtStart() {
        val component = component {
            modules(
                module {
                    factory { TestDep1() }
                }
            )
        }

        val binding = component.getBinding<TestDep1>()
        val instance = component.getInstance<TestDep1>(
            binding.type, binding.qualifier
        )

        assertFalse(binding.eager)
        assertFalse(instance.isCreated)
    }

    @Test
    fun testDeferEagerInstances() {
        val component = component(createEagerInstances = false) {
            modules(
                module {
                    single(eager = true) { TestDep1() }
                }
            )
        }

        val binding = component.getBinding<TestDep1>()
        val instance = component.getInstance<TestDep1>(
            binding.type, binding.qualifier
        )

        assertFalse(instance.isCreated)

        component.createEagerInstances()

        assertTrue(instance.isCreated)
    }

}