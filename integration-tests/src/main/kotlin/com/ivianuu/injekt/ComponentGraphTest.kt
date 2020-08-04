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

import com.ivianuu.injekt.test.Foo
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertSame
import org.junit.Test

class ComponentGraphTest {

    @Test
    fun testMissingBindingFails() = codegen(
        """
        @Given class Dep(bar: Bar)
        fun invoke() {
            runReader { given<Dep>() }
        }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDistinctTypeParameter() = codegen(
        """
        @SetElements fun setA() = setOf("a")
        @SetElements fun setB() = setOf(0)
        
        fun invoke(): Pair<Set<String>, Set<Int>> {
            return runReader { given<Set<String>>() to given<Set<Int>>() }
        }
    """
    ) {
        val (setA, setB) = invokeSingleFile<Pair<Set<String>, Set<Int>>>()
        assertNotSame(setA, setB)
    }

    @Test
    fun testDistinctTypeAlias() = codegen(
        """
        @Distinct typealias Foo1 = Foo
        @Distinct typealias Foo2 = Foo
        
        @Given fun foo1(): Foo1 = Foo()
        @Given fun foo2(): Foo2 = Foo()
        
        fun invoke(): Pair<Foo, Foo> {
            return runReader { given<Foo1>() to given<Foo2>() }
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testDistinctTypeAliasMulti() = multiCodegen(
        listOf(
            source(
                """
                @Distinct typealias Foo1 = Foo
                @Given fun foo1(): Foo1 = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                @Distinct typealias Foo2 = Foo
                @Given fun foo2(): Foo2 = Foo() 
            """
            )
        ),
        listOf(
            source(
                """
                fun invoke(): Pair<Foo, Foo> {
                    return runReader { given<Foo1>() to given<Foo2>() }
                } 
            """, name = "File.kt"
            )
        )
    ) {
        val (foo1, foo2) = it.last().invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testIgnoresNullability() = codegen(
        """
        @Given fun foo(): Foo = Foo()
        @Given fun nullableFoo(): Foo? = null

        fun invoke() { 
            runReader { given<Foo>() to given<Foo?>() }
        }
    """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableBinding() = codegen(
        """
        @Given fun foo(): Foo = Foo()

        fun invoke(): Foo? { 
            return runReader { given<Foo?>() }
        }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableBinding() = codegen(
        """
        fun invoke(): Foo? { 
            return runReader { given<Foo?>() }
        }
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """
        @Given fun list(): List<*> = emptyList<Any?>()
        
        fun invoke() { 
            runReader { given<List<*>>() }
        }
    """
    )

    @Test
    fun testPrefersInputsOverGiven() = codegen(
        """
            @Given
            fun provideFoo() = Foo()
            
            fun invoke(foo: Foo): Foo {
                return runReader(foo) { given() }
            }
        """
    ) {
        val foo = Foo()
        assertSame(foo, invokeSingleFile(foo))
    }

    @Test
    fun testPrefersInternalOverExternal() = multiCodegen(
        listOf(
            source(
                """
                    // todo remove once reader vars are supported
                    var externalFooField: Foo? = null
                    @Given
                    val externalFoo: Foo get() = externalFooField!!
                """
            )
        ),
        listOf(
            source(
                """
                    // todo remove once reader vars are supported
                    var internalFooField: Foo? = null
                    @Given
                    val internalFoo: Foo get() = internalFooField!!
                    
                    fun invoke(
                        internalFoo: Foo,
                        externalFoo: Foo
                    ): Foo {
                        externalFooField = externalFoo
                        internalFooField = internalFoo
                        return runReader { given<Foo>() }
                    }
                """,
                name = "File.kt"
            )
        )
    ) {
        val externalFoo = Foo()
        val internalFoo = Foo()
        assertSame(internalFoo, it.last().invokeSingleFile(internalFoo, externalFoo))
    }

    @Test
    fun testDuplicatedInputsFails() = codegen(
        """
        fun invoke() {
            runReader(Foo(), Foo()) { given<Foo>() }
        }
        """
    ) {
        assertInternalError("multiple inputs")
    }

    @Test
    fun testDuplicatedInternalBindingsFails() = codegen(
        """
        @Given fun foo1() = Foo()
        @Given fun foo2() = Foo()
        
        fun invoke() {
            runReader { given<Foo>() }
        }
        """
    ) {
        assertInternalError("multiple internal bindings")
    }

    @Test
    fun testDuplicatedExternalBindingsFails() = multiCodegen(
        listOf(
            source(
                """
                    @Given fun foo1() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    @Given fun foo2() = Foo()
            """
            )
        ),
        listOf(
            source(
                """
                    fun invoke() { 
                        runReader { given<Foo>() }
                    }
                """
            )
        )
    ) {
        it.last().assertInternalError("multiple external bindings")
    }

}
