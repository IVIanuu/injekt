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
import io.kotest.matchers.shouldBe
import org.junit.Test
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.AppGivenScope
import com.ivianuu.injekt.scope.GivenScopeElementBinding
import com.ivianuu.injekt.scope.Eager

class EagerTest {

    @Test
    fun testEager() {
        var callCount = 0
        class Foo
        @Eager<AppGivenScope>
        @Given
        fun eagerFoo(): Foo {
            callCount++
            return Foo()
        }
        @GivenScopeElementBinding<AppGivenScope>
        @Given
        fun fooElement(@Given foo: Foo) = foo
        val builder = com.ivianuu.injekt.scope.ComponentBuilder<AppGivenScope>()
        callCount shouldBe 0
        val component = builder.build()
        callCount shouldBe 1
        component.element<Foo>()
        callCount shouldBe 1
    }

}
