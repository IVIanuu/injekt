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
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import org.junit.Test

class GraphTest {

    @Test
    fun testMissingBindingFails() = codegen(
        """
        @Transient class Dep(bar: Bar)
        @Factory fun createDep(): TestComponent1<Dep> = create()
        """
    ) {
        assertInternalError("no binding")
    }

    // todo name
    @Test
    fun testCannotResolveDirectBindingWithAssistedParameters() = codegen(
        """
        @Transient class Dep(bar: @Assisted Bar)
        @Factory fun createDep(): TestComponent1<Dep> = create()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDuplicatedBindingFails() = codegen(
        """
        @Factory
        fun createFoo(): TestComponent1<Foo> {
            transient { Foo() }
            transient { Foo() }
            return create()
        }
        """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testCircularDependency() = codegen(
        """
        @Transient class A(b: B)
        @Transient class B(a: A)
        @Factory fun createA(): TestComponent1<A> = create()
    """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testCircularDependencyWithProvider() = codegen(
        """
        @Scoped<TestComponent1<A>> class A(b: B)
        @Transient class B(a: @Provider () -> A)
        @Factory fun invoke(): TestComponent1<A> {
            return create()
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCircularDependencyWithProvider2() = codegen(
        """
        @Scoped<TestComponent1<B>> class A(b: B)
        @Transient class B(a: @Provider () -> A)
        @Factory fun invoke(): TestComponent1<B> {
            return create()
        }
    """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testCircularDependencyWithIrrelevantProvider() = codegen(
        """
        @Scoped<TestComponent1<B>> class A(b: B)
        @Transient class B(a: A)
        @Transient class C(b: @Provider () -> B)
        @Factory fun invoke(): TestComponent1<B> {
            return create()
        }
    """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testScopeMismatch() = codegen(
        """
        @Scoped<Any> class Dep

        @Factory
        fun createDep(): TestComponent1<Dep> {
            return create()
        }
        """
    ) {
        assertInternalError("scope mismatch")
    }

    @Test
    fun testQualified() = codegen(
        """
        @Factory
        fun factory(): TestComponent2<@TestQualifier1 Foo, @TestQualifier2 Foo> { 
            scoped<@TestQualifier1 Foo> { Foo() }
            scoped<@TestQualifier2 Foo> { Foo() }
            return create()
        }
        
        fun invoke(): Pair<Foo, Foo> { 
            val component = factory()
            return component.a to component.b
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testQualifiedWithValues() = codegen(
        """
        @Target(AnnotationTarget.TYPE) 
        @Qualifier 
        annotation class QualifierWithValue(val value: String)

        @Factory
        fun factory(): TestComponent2<@QualifierWithValue("A") Foo, @QualifierWithValue("B") Foo> { 
            scoped<@QualifierWithValue("A") Foo> { Foo() }
            scoped<@QualifierWithValue("B") Foo> { Foo() }
            return create()
        }
        
        fun invoke(): Pair<Foo, Foo> { 
            val component = factory()
            return component.a to component.b
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testQualifiedWithTypeParameters() = codegen(
        """
        @Target(AnnotationTarget.TYPE) 
        @Qualifier 
        annotation class QualifierWithType<T>
        
        @Factory
        fun factory(): TestComponent2<@QualifierWithType<String> Foo, @QualifierWithType<Int> Foo> { 
            scoped<@QualifierWithType<String> Foo> { Foo() }
            scoped<@QualifierWithType<Int> Foo> { Foo() }
            return create()
        }
        
        fun invoke(): Pair<Foo, Foo> { 
            val component = factory()
            return component.a to component.b
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testQualifiedWithTypeParametersMultiCompile() = codegen(
        """
        @Target(AnnotationTarget.TYPE) 
        @Qualifier 
        annotation class QualifierWithType<T>
        
        @Factory
        fun factory(): TestComponent2<@QualifierWithType<String> Foo, @QualifierWithType<Int> Foo> { 
            scoped<@QualifierWithType<String> Foo> { Foo() }
            scoped<@QualifierWithType<Int> Foo> { Foo() }
            return create()
        }
        
        fun invoke(): Pair<Foo, Foo> { 
            val component = factory()
            return component.a to component.b
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testQualifiedGet() = codegen(
        """
        @Factory
        fun factory(): TestComponent3<@TestQualifier1 String, @TestQualifier2 String, Pair<String, String>> {
            transient<@TestQualifier1 String> { "a" }
            transient<@TestQualifier2 String> { "b" }
            transient { a: @TestQualifier1 String, b: @TestQualifier2 String ->
                Pair<String, String>(a, b)
            }
            return create()
        }

        fun invoke(): Pair<Pair<String, String>, Pair<String, String>> { 
            val component = factory()
            return component.a to component.b to component.c
        }
    """
    ) {
        val pairs = invokeSingleFile<Pair<Pair<String, String>, Pair<String, String>>>()
        assertEquals(pairs.first, pairs.second)
    }

    @Test
    fun testIgnoresNullability() = codegen(
        """
        @Factory
        fun createFoo(): TestComponent1<Foo> {
            transient<Foo> { Foo() }
            transient<Foo?> { null }
            return create()
        }
    """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableBinding() =
        codegen(
            """
        @Factory
        fun invoke(): TestComponent1<Foo?> {
            transient<Foo?>()
            return create()
        }
    """
        ) {
            assertNotNull(invokeSingleFile())
        }

    @Test
    fun testReturnsNullOnMissingNullableBinding() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Foo?> {
            return create()
        }
        
        fun invoke() = factory().a
        """
    ) {
        assertNull(invokeSingleFile())
    }

    @Test
    fun testAliasWithTypeParameters() = codegen(
        """
        @Module
        inline fun <A : B, B> fakeAlias() {
            alias<A, B>()
        }
        
        @Factory
        fun createFooAsAny(): TestComponent1<Any> {
            transient<Foo>()
            fakeAlias<Foo, Any>()
            return create()
        }
    """
    )

    @Test
    fun testTypeWithStarProjectedArg() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<List<*>> {
            instance<List<*>>(listOf<Any?>())
            return create()
        }
    """
    )

    @Test
    fun testGenericAnnotatedClass() = codegen(
        """
        @Transient
        class Wrapper<T>(val value: T)
        
        interface WrappedComponent {
            val fooWrapper: Wrapper<Foo>
            val barWrapper: Wrapper<Bar>
        }
        
        @Factory
        fun createWrapperComponent(): WrappedComponent {
            transient<Foo>()
            transient<Bar>()
            return create()
        }
    """
    )

    /*@Test
    fun testGenericDslProvider() = codegen("""
        class Wrapper<T>(val value: T)
        
        interface WrappedComponent {
            val fooWrapper: Wrapper<Foo>
            val barWrapper: Wrapper<Bar>
        }
        
        @Factory
        fun createWrapperComponent(): WrappedComponent {
            transient<Foo>()
            transient<Bar>()
            transient(::createWrapper)
            return create()
        }
        
        private fun <T> createWrapper(value: T): Wrapper<T> = Wrapper(value)
    """)*/

}
