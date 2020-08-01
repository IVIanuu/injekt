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

import com.ivianuu.injekt.test.Bar
import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test

class RunReaderTest {

    @Test
    fun testSimple() = codegen(
        """
        @Given
        fun foo() = Foo()
        @Given
        fun bar() = Bar(given())
        
        fun invoke(): Bar {
            initializeInjekt()
            return runReader { given<Bar>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Bar)
    }

    @Test
    fun testScoping() = codegen(
        """
        @Given(ApplicationScoped::class)
        fun foo() = Foo()
        
        fun invoke(): Foo {
            initializeInjekt()
            return runReader { given<Foo>() }
        }
    """
    ) {
        assertSame(
            invokeSingleFile(),
            invokeSingleFile()
        )
    }

    @Test
    fun testGivenClass() = codegen(
        """
        @Given
        class AnnotatedBar {
            private val foo: Foo = given()
        }
        
        @Given
        fun foo(): Foo = Foo()

        fun invoke(): Any { 
            initializeInjekt()
            return runReader { given<AnnotatedBar>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    // todo @Test
    fun testGivenObject() = codegen(
        """
        @Given object AnnotatedFoo

        fun invoke() = {
            initializeInjekt()
            return runReader { given<AnnotatedFoo>() }
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testGivenProperty() = codegen(
        """
        @Given val foo = Foo()
        
        fun invoke(): Foo {
            initializeInjekt()
            return runReader { given<Foo>() }
        }
    """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testAssistedGivenFunction() = codegen(
        """ 
        @Given
        fun bar(foo: Foo) = Bar(foo)

        fun invoke(foo: Foo): Bar { 
            initializeInjekt()
            return runReader { given<Bar>(foo) }
        }
    """
    ) {
        invokeSingleFile(Foo())
    }

    @Test
    fun testAssistedGivenClass() = codegen(
        """ 
        @Given
        class AnnotatedBar(foo: Foo)

        fun invoke(foo: Foo): Any {
            initializeInjekt()
            return runReader { given<AnnotatedBar>(foo) }
        }
    """
    ) {
        invokeSingleFile(Foo())
    }

    // todo @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        @Given @Reader class Dep<T> {
            val value: T = given()
        }
        
        @Given fun foo() = Foo() 
        
        fun invoke() {
            initializeInjekt()
            runReader {
                given<Dep<Foo>>()
            }
        }
    """
    )

    @Test
    fun testRunReaderInput() = codegen(
        """
        fun invoke(): Pair<Foo, Foo> {
            initializeInjekt()
            val foo = Foo()
            return foo to runReader(foo) { given<Foo>() }
        }
    """
    ) {
        val (a, b) = invokeSingleFile<Pair<Foo, Foo>>()
        assertSame(a, b)
    }

}
