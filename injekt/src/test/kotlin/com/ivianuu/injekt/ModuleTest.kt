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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleTest {

    @Test
    fun testBind() {
        val binding = definitionBinding(false) { "value" }
        val module = module {
            bind(
                key = keyOf<String>(),
                binding = binding
            )
        }
        assertTrue(binding in module.bindings.values)
    }

    @Test
    fun testAllowsExplicitOverride() {
        val originalBinding = definitionBinding(false) { "value" }
        val overrideBinding = definitionBinding(false) { "overridden_value" }

        val module = module {
            bind(key = keyOf<String>(), binding = originalBinding)
            bind(key = keyOf<String>(), binding = overrideBinding, override = true)
        }

        assertEquals(module.bindings[keyOf<String>()], overrideBinding)
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsImplicitOverride() {
        val firstBinding = definitionBinding(false) { "value" }
        val overrideBinding = definitionBinding(false) { "overridden_value" }

        module {
            bind(key = keyOf<String>(), binding = firstBinding)
            bind(key = keyOf<String>(), binding = overrideBinding, override = false)
        }
    }

    @Test
    fun testInclude() {
        val moduleA = module {
            factory { TestDep1() }
            map<String, Any> { "key" to keyOf<TestDep1>() }
            set<Any> { add<TestDep1>() }
        }

        val moduleB = module { include(moduleA) }

        assertTrue(keyOf<TestDep1>() in moduleB.bindings)
        assertTrue(moduleB.mapBindings.getAll().containsKey(keyOf<Map<String, Any>>()))
        assertTrue(moduleB.setBindings.getAll().containsKey(keyOf<Set<Any>>()))
    }

    @Test
    fun testInheresAllAttributesWhenIncluding() {
        val moduleA = module {
            single(override = true, eager = true) { TestDep1() }
        }

        val moduleB = module { include(moduleA) }

        val binding = moduleB.bindings.values.single()
        assertTrue(binding.override)
        assertTrue(binding.eager)
        assertTrue(binding.scoped)
    }
}