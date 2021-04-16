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

import com.ivianuu.injekt.test.*
import io.kotest.matchers.types.*
import org.junit.*

class GivenLambdaTest {
    @Test
    fun testGivenLambda() = codegen(
        """
            fun invoke(foo: Foo) = given<@Given (@Given () -> Foo) -> Foo>()({ foo })
        """
    ) {
        val foo = Foo()
        invokeSingleFile(foo) shouldBeSameInstanceAs foo
    }

    @Test
    fun testGivenLambdaChain() = codegen(
        """
            @Given val fooModule: @Given () -> @Given () -> Foo = { { Foo() } }
            fun invoke() = given<Foo>()
        """
    ) {
        invokeSingleFile()
            .shouldBeTypeOf<Foo>()
    }

    @Test
    fun testCanRequestGivenLambda() = codegen(
        """
            typealias MyAlias = @Composable () -> Unit
            @Given fun myAlias(): MyAlias = {}
            @Given class MyComposeView(@Given val content: @Composable () -> Unit)
            fun invoke() = given<(@Given @Composable () -> Unit) -> MyComposeView>()
        """
    )
}
