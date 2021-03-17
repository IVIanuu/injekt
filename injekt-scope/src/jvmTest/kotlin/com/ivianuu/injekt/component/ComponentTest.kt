// injekt-incremental-fix 1615647377329 injekt-end
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

package com.ivianuu.injekt.component

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.common.typeKeyOf
import com.ivianuu.injekt.scope.GivenScopeElementBinding
import com.ivianuu.injekt.scope.GivenScopeInitializer
import io.kotest.matchers.shouldBe
import org.junit.Test

class ComponentTest {

    @Test
    fun testReturnsExistingValue() {
        val component = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .element { "value" }
            .build()
        component.element<String>() shouldBe "value"
    }

    @Test
    fun testReturnsNullForNotExistingValue() {
        val component = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        component.elementOrNull(typeKeyOf<String>()) shouldBe null
    }

    @Test
    fun testReturnsFromDependency() {
        val component = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .dependency(
                com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
                    elements = { emptySet() },
                    initializers = { emptySet() }
                )
                    .element { "value" }
                    .build()
            )
            .build()
        component.element<String>() shouldBe "value"
    }

    @Test
fun testGetDependencyReturnsDependency() {
        val dependency = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        val dependent = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .dependency(dependency)
            .build()
        dependent.element<TestComponent1>() shouldBeSameInstanceAs dependency
    }

    @Test
fun testGetDependencyReturnsNullIfNotExists() {
        val dependent = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        ).build()
        dependent.elementOrNull(typeKeyOf<TestComponent1>()) shouldBe null
    }

    @Test
    fun testOverridesDependency() {
        val component = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent2>(
            elements = { emptySet() },
            initializers = { emptySet() }
        )
            .dependency(
                com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
                    elements = { emptySet() },
                    initializers = { emptySet() }
                )
                    .element { "dependency" }
                    .build()
            )
            .element { "child" }
            .build()
        component.element<String>() shouldBe "child"
    }

    @Test
    fun testInjectedElement() {
        @Given val injected: @GivenScopeElementBinding<TestComponent1> String = "value"
        val component = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
            initializers = { emptySet() }
        ).build()
        component.element<String>() shouldBe "value"
    }

    @Test
    fun testElementBinding() {
        @GivenScopeElementBinding<TestComponent1>
        @Given
        fun element(@Given component: TestComponent1) = component to component
        @GivenScopeElementBinding<TestComponent2>
        @Given
        fun otherElement() = 0
        val component = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
            initializers = { emptySet() }
        ).build()
        component.element<Pair<TestComponent1, TestComponent1>>().first shouldBeSameInstanceAs component
        component.elementOrNull(typeKeyOf<Int>()).shouldBeNull()
    }

    @Test
    fun testComponentInitializer() {
        var called = false
        @Given
        fun initializer(@Given component: TestComponent1): GivenScopeInitializer<TestComponent1> = {
            called = true
        }
        var otherCalled = false
        @Given
        fun otherInitializer(): GivenScopeInitializer<TestComponent2> = {
            otherCalled = true
        }
        val builder = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
            elements = { emptySet() }
        )
        called shouldBe false
        val component = builder.build()
        called shouldBe true
        otherCalled shouldBe false
    }

    @Test
    fun testChildComponentModule() {
        @Given
        val childComponentModule =
            com.ivianuu.injekt.scope.ChildComponentModule1<TestComponent1, String, TestComponent2>()

        val parentComponent = com.ivianuu.injekt.scope.ComponentBuilder<TestComponent1>(
            initializers = { emptySet() }
        ).build()
        val childComponent = parentComponent.element<(String) -> TestComponent2>()("42")
        childComponent.element<String>() shouldBe "42"
    }

}
