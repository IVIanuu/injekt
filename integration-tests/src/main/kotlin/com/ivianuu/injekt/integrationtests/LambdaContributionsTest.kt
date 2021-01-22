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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class LambdaContributionsTest {

    @Test
    fun testModuleLambdaParameter() = codegen(
        """
            class Ctx(@Given val foo: Foo)
            val factory: (@Module Ctx) -> Foo = { given() }
            fun invoke(foo: Foo) = factory(Ctx(foo))
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testLambdaModule() = codegen(
        """
            @Given val foo = Foo()
            @Module val barGiven: @Given (@Given Foo) -> Bar = { Bar(it) }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testNestedLambdaModule() = codegen(
        """
            @Given val foo = Foo()
            @Module val barGiven: @Module (@Given Foo) -> @Given () -> Bar = { foo ->
                {
                    Bar(foo)
                }
            }
            fun invoke() = given<Bar>()
        """
    )

    @Test
    fun testLambdaGivenSetElement() = codegen(
        """
            @Given val foo = Foo()
            @Module val fooSet: @GivenSetElement (@Given Foo) -> Foo = { it }
            fun invoke() = given<Set<Foo>>()
        """
    )

    @Test
    fun testInterceptorLambda() = codegen(
        """
            var called = false
            fun <T> interceptorFactory(): @Interceptor (() -> T) -> T = { called = true; it() }
           
            @Module val fooInterceptorModule = interceptorFactory<Foo>()
            @Given fun foo() = Foo()
            
            fun invoke(): Boolean {
                given<Foo>()
                return called
            }
        """
    ) {
        assertTrue(invokeSingleFile<Boolean>())
    }

    @Test
    fun testScopedLambdaGiven() = codegen(
        """
            @Module val givens: @Given (@Given Foo) -> @Scoped<AppComponent> Bar = { Bar(it) } 
            @Given val foo = Foo()
            @Given val appComponent = ComponentBuilder<AppComponent>().build()
            fun invoke() = given<Bar>()
        """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

}
