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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.tschuchort.compiletesting.PluginOption
import org.junit.Test

class ConfigurationTest {

    @Test
    fun testCannotPerformGivenCallsWithoutFlag() = codegen(
        """
            fun invoke() = given<Foo>()
        """,
        config = {
            pluginOptions = pluginOptions
                .filter { it.optionName != "allowGivenCalls" } + PluginOption(
                "com.ivianuu.injekt",
                "allowGivenCalls",
                "false"
            )
        }
    ) {
        compilationShouldHaveFailed("given calls are not allowed in this compilation. Please check your build configuration or provide all arguments to the call")
    }

    @Test
    fun testCanPerformGivenCallsWithoutFlagButWithAllArguments() = codegen(
        """
            fun invoke() = given<Foo>(Foo())
        """,
        config = {
            pluginOptions = pluginOptions
                .filter { it.optionName != "allowGivenCalls" } + PluginOption(
                "com.ivianuu.injekt",
                "allowGivenCalls",
                "false"
            )
        }
    )

}