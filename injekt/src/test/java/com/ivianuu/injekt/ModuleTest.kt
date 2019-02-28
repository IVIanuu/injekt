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
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import org.junit.Test

class ModuleTest {

    @Test
    fun testModuleConfigOverridesBinding() {
        val module = module(eager = true, override = true) {
            single(override = false, eager = false) { TestDep1() }
        }

        val component = component { modules(module) }

        val binding = component.getBindings().first()
        assertEquals(true, module.override)
        assertTrue(binding.override)

        assertEquals(true, module.eager)
        assertTrue(binding.eager)
    }

    @Test
    fun testModuleConfigNotOverridesBindings() {
        val module = module {
            single(override = true, eager = true) { TestDep1() }
        }

        val binding = module.getBindings().first()
        assertNull(module.override)
        assertTrue(binding.override)

        assertNull(module.eager)
        assertTrue(binding.eager)
    }

    @Test
    fun testAllowsValidOverride() {
        val throwed = try {
            component {
                modules(
                    module {
                        single { TestDep1() }
                        single(override = true) { TestDep1() }
                    }
                )
            }
            false
        } catch (e: OverrideException) {
            true
        }

        assertFalse(throwed)
    }

    @Test
    fun testThrowsOnInvalidOverride() {
        val throwed = try {
            component {
                modules(
                    module {
                        single { TestDep1() }
                        single { TestDep1() }
                    }
                )
            }
            false
        } catch (e: OverrideException) {
            true
        }

        assertTrue(throwed)
    }

}