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

package com.ivianuu.injekt.scope

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.common.typeKeyOf
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class GivenScopeTest {
    @Test
    fun testReturnsExistingValue() {
        val givenScope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .element { "value" }
            .build()
        givenScope.element<String>() shouldBe "value"
    }

    @Test
    fun testReturnsNullForNotExistingValue() {
        val givenScope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        givenScope.elementOrNull(typeKeyOf<String>()) shouldBe null
    }

    @Test
    fun testReturnsFromDependency() {
        val givenScope = GivenScopeBuilder<TestGivenScope2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .dependency(
                GivenScopeBuilder<TestGivenScope1>(
                    elements = { emptySet() },
                    initializers = { emptySet() }
                )
                    .element { "value" }
                    .build()
            )
            .build()
        givenScope.element<String>() shouldBe "value"
    }

    @Test
    fun testGetDependencyReturnsDependency() {
        val dependency = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        val dependent = GivenScopeBuilder<TestGivenScope2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .dependency(dependency)
            .build()
        dependent.element<TestGivenScope1>() shouldBeSameInstanceAs dependency
    }

    @Test
    fun testGetDependencyReturnsNullIfNotExists() {
        val dependent = GivenScopeBuilder<TestGivenScope2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        dependent.elementOrNull(typeKeyOf<TestGivenScope1>()) shouldBe null
    }

    @Test
    fun testOverridesDependency() {
        val givenScope = GivenScopeBuilder<TestGivenScope2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .dependency(
                GivenScopeBuilder<TestGivenScope1>(
                    elements = { emptySet() },
                    initializers = { emptySet() }
                )
                    .element { "dependency" }
                    .build()
            )
            .element { "child" }
            .build()
        givenScope.element<String>() shouldBe "child"
    }

    @Test
    fun testInjectedElement() {
        @Given val injected: @GivenScopeElementBinding<TestGivenScope1> String = "value"
        val givenScope = GivenScopeBuilder<TestGivenScope1>(
            initializers = { emptySet() }
        ).build()
        givenScope.element<String>() shouldBe "value"
    }

    @Test
    fun testElementBinding() {
        @GivenScopeElementBinding<TestGivenScope1>
        @Given
        fun element(@Given givenScope: TestGivenScope1) = givenScope to givenScope
        @GivenScopeElementBinding<TestGivenScope2>
        @Given
        fun otherElement() = 0
        val givenScope = GivenScopeBuilder<TestGivenScope1>(
            initializers = { emptySet() }
        ).build()
        givenScope.element<Pair<TestGivenScope1, TestGivenScope1>>().first shouldBeSameInstanceAs givenScope
        givenScope.elementOrNull(typeKeyOf<Int>()).shouldBeNull()
    }

    @Test
    fun testGivenScopeInitializer() {
        var called = false
        @Given
        fun initializer(@Given givenScope: TestGivenScope1): GivenScopeInitializer<TestGivenScope1> = {
            called = true
        }
        var otherCalled = false
        @Given
        fun otherInitializer(): GivenScopeInitializer<TestGivenScope2> = {
            otherCalled = true
        }
        val builder = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() }
        )
        called shouldBe false
        val givenScope = builder.build()
        called shouldBe true
        otherCalled shouldBe false
    }

    @Test
    fun testChildGivenScopeModule() {
        @Given
        val childGivenScopeModule =
            ChildGivenScopeModule1<TestGivenScope1, String, TestGivenScope2>()
        val parentGivenScope = GivenScopeBuilder<TestGivenScope1>(
            initializers = { emptySet() }
        ).build()
        val childGivenScope = parentGivenScope.element<(String) -> TestGivenScope2>()("42")
        childGivenScope.element<String>() shouldBe "42"
    }

    @Test
    fun testGetSetScopedValue() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        scope.getScopedValueOrNull<String>(0) shouldBe null
        scope.setScopedValue(0, "value")
        scope.getScopedValueOrNull<String>(0) shouldBe "value"
    }

    @Test
    fun testRemoveScopedValueDisposesValue() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        var disposed = false
        scope.setScopedValue(0, GivenScopeDisposable { disposed = true })

        disposed.shouldBeFalse()
        scope.removeScopedValue(0)
        disposed.shouldBeTrue()
    }

    @Test
    fun testSetScopedValueDisposesOldValue() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        var disposed = false
        scope.setScopedValue(0, GivenScopeDisposable { disposed = true })

        disposed.shouldBeFalse()
        scope.setScopedValue(0, 0)
        disposed.shouldBeTrue()
    }

    @Test
    fun testDisposeDisposesValues() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        var disposed = false
        scope.setScopedValue(0, GivenScopeDisposable { disposed = true })

        disposed.shouldBeFalse()
        scope.dispose()
        disposed.shouldBeTrue()
    }

    @Test
    fun testGetOrCreateScopedValue() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        var calls = 0
        scope.getOrCreateScopedValue(0) { calls++ }
        scope.getOrCreateScopedValue(0) { calls++ }
        scope.getOrCreateScopedValue(1) { calls++ }
        calls shouldBe 2
    }

    @Test
    fun testDispose() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        scope.isDisposed.shouldBeFalse()
        scope.dispose()
        scope.isDisposed.shouldBeTrue()
    }

    @Test
    fun testInvokeOnDispose() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        var called = false
        scope.invokeOnDispose { called = true }
        called.shouldBeFalse()
        scope.dispose()
        called.shouldBeTrue()
    }

    @Test
    fun testInvokeOnDisposeOnDisposedScope() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        var called = false
        scope.dispose()
        scope.invokeOnDispose { called = true }
        called.shouldBeTrue()
    }

    @Test
    fun testDoesNotInvokeOnDisposeIfReturnDisposableWasDisposed() {
        val scope = GivenScopeBuilder<TestGivenScope1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        var called = false
        val disposable = scope.invokeOnDispose { called = true }
        disposable.dispose()
        called.shouldBeFalse()
        scope.dispose()
        called.shouldBeFalse()
    }
}
