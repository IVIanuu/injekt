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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleTest {

    @Test
    fun testBind() {
        val binding = definitionBinding { "value" }
        val module = module { bind(binding) }
        assertTrue(module.bindings.values.contains(binding))
    }

    @Test
    fun testAllowsExplicitOverride() {
        val firstBinding = definitionBinding { "value" }
        val overrideBinding = definitionBinding { "overridden_value" }

        val module = module {
            bind(firstBinding)
            bind(overrideBinding, override = true)
        }

        assertEquals(module.bindings[keyOf<String>()], overrideBinding)
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsImplicitOverride() {
        val firstBinding = definitionBinding { "value" }
        val overrideBinding = definitionBinding { "overridden_value" }

        module {
            bind(firstBinding)
            bind(overrideBinding, override = false)
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

        assertTrue(moduleB.bindings.containsKey(keyOf<TestDep1>()))
        assertTrue(moduleB.mapBindings?.getAll()?.containsKey(keyOf<Map<String, Any>>()) ?: false)
        assertTrue(moduleB.setBindings?.getAll()?.containsKey(keyOf<Set<Any>>()) ?: false)
    }

    @Test
    fun testInheresAllAttributesWhenIncluding() {
        val moduleA = module {
            single(override = true) { TestDep1() }
        }

        val moduleB = module { include(moduleA) }

        val binding = moduleB.bindings.values.first()
        assertTrue(binding.override)
        assertFalse(binding.unscoped)
    }
}