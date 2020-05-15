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

import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import org.junit.Test

class GraphTest {

    @Test
    fun testMissingBindingFails() = codegen(
        """
        @Transient class Dep(bar: Bar)
        @Factory fun create(): Dep = createInstance()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDuplicatedBindingFails() = codegen(
        """
        @Factory
        fun create(): Foo {
            transient { Foo() }
            transient { Foo() }
            return createInstance()
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
        @Factory fun create(): A = createInstance()
    """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testScopeMismatch() = codegen(
        """
        @TestScope2 class Dep

        @Factory
        fun create(): Dep {
            scope<TestScope>()
            return createInstance()
        }
        """
    ) {
        assertInternalError("scope mismatch")
    }

    @Test
    fun testQualified() = codegen(
        """
            @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
            @Qualifier
            annotation class TestQualifier1
            @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
            @Qualifier
            annotation class TestQualifier2
        interface TestComponent {
            val foo1: @TestQualifier1 Foo
            val foo2: @TestQualifier2 Foo
        }
        
        @Factory
        fun create(): TestComponent {
            @TestQualifier1 scoped { Foo() }
            @TestQualifier2 scoped { Foo() }
            return createImpl()
        }
        
        fun invoke(): Pair<Foo, Foo> { 
            val component = create()
            return component.foo1 to component.foo2
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testQualifiedWithValues() = codegen(
        """
            @Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
            @Qualifier
            annotation class QualifierWithValue(val value: String)
            
        interface TestComponent {
            val foo1: @QualifierWithValue("A") Foo
            val foo2: @QualifierWithValue("B") Foo
        }
        
        @Factory
        fun create(): TestComponent {
            @QualifierWithValue("A") scoped { Foo() }
            @QualifierWithValue("B") scoped { Foo() }
            return createImpl()
        }
        
        fun invoke(): Pair<Foo, Foo> { 
            val component = create()
            return component.foo1 to component.foo2
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testIgnoresNullability() = codegen(
        """
        @Factory
        fun create(): Foo {
            transient<Foo> { Foo() }
            transient<Foo?> { null }
            return createInstance()
        }
    """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testReturnsInstanceForNullableBinidng() = codegen(
        """
        @Factory
        fun invoke(): Foo? {
            transient<Foo?>()
            return createInstance()
        }
    """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableBinding() = codegen(
        """
        @Factory
        fun invoke(): Foo? {
            return createInstance()
        }
        """
    ) {
        assertNull(invokeSingleFile())
    }

}
