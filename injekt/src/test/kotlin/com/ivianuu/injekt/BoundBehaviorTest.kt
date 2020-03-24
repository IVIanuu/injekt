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

import junit.framework.Assert.assertEquals
import org.junit.Test

class BoundBehaviorTest {

    @Test
    fun testWithoutScope() {
        val usedComponents = mutableListOf<Component>()

        val componentA = Component {
            bind(
                Binding(
                    keyOf(),
                    behavior = BoundBehavior
                ) {
                    usedComponents += this
                    Unit
                }
            )
        }

        val componentB = Component {
            parents(componentA)
        }

        componentA.get<Unit>()
        componentB.get<Unit>()

        assertEquals(listOf(componentA, componentA), usedComponents)
    }

    @Test
    fun testWithScope() {
        val usedComponents = mutableListOf<Component>()

        val componentA = Component {
            scopes(TestScopeOne)
        }

        val componentB = Component {
            parents(componentA)

            bind(
                Binding(
                    keyOf(),
                    behavior = BoundBehavior(scope = TestScopeOne)
                ) {
                    usedComponents += this
                    Unit
                }
            )
        }

        val componentC = Component {
            parents(componentB)
        }

        componentB.get<Unit>()
        componentC.get<Unit>()

        assertEquals(listOf(componentA, componentA), usedComponents)
    }
}
