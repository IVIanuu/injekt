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

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class EagerTest {

    @Test
    fun testEagerBehavior() {
        var called = false
        Component { bind { called = true } }
        assertFalse(called)
        Component {
            bind(behavior = Eager) { called = true }
        }
        assertTrue(called)
    }

    @Test
    fun testEagerDslFunction() {
        var called = false
        Component {
            factory {
                called = true
                "non eager binding"
            }
            eager<String>()
        }
        assertTrue(called)
    }

    @Test
    fun testMultipleBoundEagerBindings() {
        Component {
            factory(qualifier = Qualifier(UUID.randomUUID())) { get<TestDep3>() }
            factory(behavior = Bound + Eager) { TestDep2(get()) }
            factory(behavior = Bound + Eager) { TestDep1() }
        }
    }

}
