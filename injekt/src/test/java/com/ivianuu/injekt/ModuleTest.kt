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

class ModuleTest {

    @Test
    fun testBind() {
val binding = object : Binding<String> {
override fun link(context: DefinitionContext) {
}
override fun get(parameters: ParametersDefinition?): String = "hello"
}
val module = module { bindAlias(keyOf<String>(), binding) }
        assertTrue(module.bindings.values.contains(binding))
    }

    @Test
    fun testAllowsExplicitOverride() {
        val firstBinding = binding(
            kind = FactoryKind,
            definition = { "my_value" }
        )

        val overrideBinding = binding(
            kind = SingleKind,
            override = true,
            definition = { "my_overridden_value" }
        )

        val module = module {
            bindAlias(firstBinding)
            bindAlias(overrideBinding)
        }

        assertEquals(module.bindings[Key(typeOf<String>())], overrideBinding)
    }

    @Test(expected = IllegalStateException::class)
    fun testDisallowsImplicitOverride() {
        val firstBinding = binding(
            kind = FactoryKind,
            definition = { "my_value" }
        )

        val overrideBinding = binding(
            kind = SingleKind,
            definition = { "my_overridden_value" }
        )

        module {
            bindAlias(firstBinding)
            bindAlias(overrideBinding)
        }
    }

}*/