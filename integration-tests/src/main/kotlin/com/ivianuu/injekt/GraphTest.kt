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
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertNull
import org.junit.Test

class GraphTest {

    @Test
    fun testMissingBindingFails() = codegen(
        """
        @Given class Dep(bar: Bar)
        fun invoke() {
            initializeComponents()
            component.runReader { get<Dep>() }
        }
        """
    ) {
        assertInternalError("no binding")
    }

    // todo name
    @Test
    fun testCannotResolveDirectBindingWithAssistedParameters() = codegen(
        """
        @Given class Dep(bar: @Assisted Bar)
        @Factory fun createDep(): TestComponent1<Dep> = create()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testDuplicatedBindingFails() = codegen(
        """
        @Given fun foo1() = Foo()
        @Given fun foo2() = Foo()
        
        fun invoke() {
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            component.runReader { given<Foo>() }
        }
        """
    ) {
        assertInternalError("multiple")
    }

    @Test
    fun testComponentMismatch() = codegen(
        """
        @Given(Any::class) class Dep

        fun invoke() {
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            component.runReader { given<Dep>() }
        }
        """
    ) {
        assertInternalError("component mismatch")
    }

    @Test
    fun testDistinctTypeDistinction() = codegen(
        """
        @DistinctType typealias Foo1 = Foo
        @DistinctType typealias Foo2 = Foo
        
        @Given @Reader fun foo1(): Foo1 = Foo()
        @Given @Reader fun foo2(): Foo2 = Foo()
        
        fun invoke(): Pair<Foo, Foo> {
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            return component.runReader { given<Foo1>() to given<Foo2>() }
        }
    """
    ) {
        val (foo1, foo2) = invokeSingleFile<Pair<Foo, Foo>>()
        assertNotSame(foo1, foo2)
    }

    @Test
    fun testIgnoresNullability() = codegen(
        """
        @Given fun foo(): Foo = Foo()
        @Given fun nullableFoo(): Foo? = null

        fun invoke() { 
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            component.runReader { given<Foo1>() to given<Foo2>() }
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
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            return component.runReader { given<Foo?>() }
        }
        """
    ) {
        assertNotNull(invokeSingleFile())
    }

    @Test
    fun testReturnsNullOnMissingNullableBinding() = codegen(
        """
        fun invoke(): Foo? { 
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            return component.runReader { given<Foo?>() }
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
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            component.runReader { given<List<*>>() }
        }
    """
    )

}
