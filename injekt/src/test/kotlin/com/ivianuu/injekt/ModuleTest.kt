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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleTest {

    @Test
    fun testBind() {
        val binding = DefinitionBinding(key = keyOf<String>()) { "value" }
        val module = Module { bind(binding) }
        assertTrue(binding in module.bindings.values)
    }

    @Test
    fun testOverride() {
        val originalBinding = DefinitionBinding(key = keyOf<String>()) { "value" }
        val overrideBinding =
            DefinitionBinding(
                key = keyOf<String>(),
                overrideStrategy = OverrideStrategy.Override
            ) { "overridden_value" }

        val module = Module {
            bind(originalBinding)
            bind(overrideBinding)
        }

        assertEquals(module.bindings[keyOf<String>()], overrideBinding)
    }

    @Test
    fun testOverrideDrop() {
        val originalBinding = DefinitionBinding(key = keyOf<String>()) { "value" }
        val overrideBinding =
            DefinitionBinding(
                key = keyOf<String>(),
                overrideStrategy = OverrideStrategy.Drop
            ) { "overridden_value" }

        val module = Module {
            bind(originalBinding)
            bind(binding = overrideBinding)
        }

        assertEquals(module.bindings[keyOf<String>()], originalBinding)
    }

    @Test(expected = IllegalStateException::class)
    fun testOverrideFail() {
        val originalBinding = DefinitionBinding(key = keyOf<String>()) { "value" }
        val overrideBinding =
            DefinitionBinding(
                key = keyOf<String>(),
                overrideStrategy = OverrideStrategy.Fail
            ) { "overridden_value" }

        Module {
            bind(originalBinding)
            bind(overrideBinding)
        }
    }

    @Test
    fun testInclude() {
        val moduleA = Module {
            factory { TestDep1() }
            map<String, Any> { "key" to keyOf<TestDep1>() }
            set<Any> { add<TestDep1>() }
        }

        val moduleB = Module { include(moduleA) }

        assertTrue(keyOf<TestDep1>() in moduleB.bindings)
        assertTrue(keyOf<Map<String, Any>>() in moduleB.multiBindingMaps)
        assertTrue(keyOf<Set<Any>>() in moduleB.multiBindingSets)
    }

}
