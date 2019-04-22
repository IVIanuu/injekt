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

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ModuleBuilderTest {

    @Test
    fun testOverride() {
        val firstBinding = binding(
            type = String::class,
            kind = FactoryKind,
            definition = { "my_value" }
        )

        val overrideBinding = binding(
            type = String::class,
            kind = SingleKind,
            override = true,
            definition = { "my_overridden_value" }
        )

        val module = module {
            addBinding(firstBinding)
            addBinding(overrideBinding)
        }

        assertTrue(module.bindings[Key(String::class)] === overrideBinding)
    }

    @Test
    fun testAllowsValidOverride() {
        val firstBinding = binding(
            type = String::class,
            kind = FactoryKind,
            definition = { "my_value" }
        )

        val overrideBinding = binding(
            type = String::class,
            kind = SingleKind,
            override = true,
            definition = { "my_overridden_value" }
        )

        var throwed = false

        try {
            module {
                addBinding(firstBinding)
                addBinding(overrideBinding)
            }
        } catch (e: Exception) {
            throwed = true
        }

        assertFalse(throwed)
    }

    @Test
    fun testDisallowsInvalidOverride() {
        val firstBinding = binding(
            type = String::class,
            kind = FactoryKind,
            definition = { "my_value" }
        )

        val overrideBinding = binding(
            type = String::class,
            kind = SingleKind,
            definition = { "my_overridden_value" }
        )

        var throwed = false

        try {
            module {
                addBinding(firstBinding)
                addBinding(overrideBinding)
            }
        } catch (e: Exception) {
            throwed = true
        }

        assertTrue(throwed)
    }

}