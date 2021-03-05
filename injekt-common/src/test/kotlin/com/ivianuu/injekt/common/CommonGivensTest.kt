// injekt-incremental-fix 1614946737776 injekt-end
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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.given
import org.junit.Test

class CommonGivensTest {

    @Test
    fun testCanInjectMapForSetOfPairs() {
        @Given val set = setOf("key" to "value")
        val map = given<Map<String, String>>()
    }

    @Test
    fun testCanInjectLazy() {
        @Given fun foo() = Foo()
        val lazyFoo = given<Lazy<Foo>>()
    }

    class Foo

}
