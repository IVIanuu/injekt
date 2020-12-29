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

import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.common.keyOf
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test

class ComponentTest {

    @Test
    fun testReturnsExistingValue() {
        val component = ComponentBuilder<TestComponent1>()
            .element("value")
            .build()
        component.get<String>() shouldBe "value"
    }

    @Test
    fun testReturnsNullForNotExistingValue() {
        val component = ComponentBuilder<TestComponent1>().build()
        component.getOrNull(keyOf<String>()) shouldBe null
    }

    @Test
    fun testReturnsFromDependency() {
        val component = ComponentBuilder<TestComponent2>()
            .dependency(
                ComponentBuilder<TestComponent1>()
                    .element("value")
                    .build()
            )
            .build()
        component.get<String>() shouldBe "value"
    }

    @Test fun testGetDependencyReturnsDependency() {
        val dependency = ComponentBuilder<TestComponent1>().build()
        val dependent = ComponentBuilder<TestComponent2>()
            .dependency(dependency)
            .build()
        dependent.get<TestComponent1>() shouldBeSameInstanceAs dependency
    }

    @Test fun testGetDependencyReturnsNullIfNotExists() {
        val dependent = ComponentBuilder<TestComponent2>().build()
        dependent.getOrNull(keyOf<TestComponent1>()) shouldBe null
    }

    @Test
    fun testOverridesDependency() {
        val component = ComponentBuilder<TestComponent2>()
            .dependency(
                ComponentBuilder<TestComponent1>()
                    .element("dependency")
                    .build()
            )
            .element("child")
            .build()
        component.get<String>() shouldBe "child"
    }

    @Test
    fun testInjectedElement() {
        @GivenSetElement val injected = componentElement<TestComponent1, String>("value")
        val component = ComponentBuilder<TestComponent1>().build()
        component.get<String>() shouldBe "value"
    }

}
