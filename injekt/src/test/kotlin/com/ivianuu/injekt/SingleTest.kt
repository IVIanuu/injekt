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
import junit.framework.Assert.assertTrue
import org.junit.Test

class SingleTest {

    @Test
    fun testInstantiatesOnlyOnce() {
        val component = Component {
            modules(
                Module {
                    single { TestDep1() }
                }
            )
        }

        val value1 = component.get<TestDep1>()
        val value2 = component.get<TestDep1>()

        assertTrue(System.identityHashCode(value1) == System.identityHashCode(value2))
    }

    @Test
    fun testReusesSingleJustInTimeBindings() {
        val componentA = Component { scopes(TestScopeOne) }

        val componentB = Component {
            scopes(TestScopeTwo)
            dependencies(componentA)
        }
        val componentC = Component {
            scopes(TestScopeThree)
            dependencies(componentB)
        }

        val depA = componentA.get<SingleJustInTimeDep>()
        val depA2 = componentA.get<SingleJustInTimeDep>()
        val depB = componentB.get<SingleJustInTimeDep>()
        val depC = componentC.get<SingleJustInTimeDep>()

        assertEquals(depA, depA2)
        assertEquals(depA, depB)
        assertEquals(depA, depC)
    }

}

@TestScopeOne
@Single
class SingleJustInTimeDep {
    object BindingFactory : com.ivianuu.injekt.BindingFactory<SingleJustInTimeDep> {
        override fun create(): Binding<SingleJustInTimeDep> {
            return DefinitionBinding(
                key = keyOf<SingleJustInTimeDep>(),
                kind = SingleKind,
                scoping = Scoping.Scoped(TestScopeOne)
            ) { SingleJustInTimeDep() }
        }
    }
}