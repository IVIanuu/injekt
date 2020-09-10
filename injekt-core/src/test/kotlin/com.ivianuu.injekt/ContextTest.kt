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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.Bar
import com.ivianuu.injekt.Command
import com.ivianuu.injekt.CommandA
import com.ivianuu.injekt.CommandB
import com.ivianuu.injekt.CommandC
import com.ivianuu.injekt.Foo
import com.ivianuu.injekt.childContext
import com.ivianuu.injekt.given
import com.ivianuu.injekt.map
import com.ivianuu.injekt.rootContext
import com.ivianuu.injekt.set
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextTest {

    @Test
    fun testSimple() {
        val context = rootContext {
            given { Foo() }
            given { Bar(given()) }
        }

        context.given<Bar>()
    }

    @Test
    fun testParentChild() {
        val parent = rootContext {
            given { Foo() }
        }
        val child = parent.childContext {
            given { Bar(given()) }
        }
        child.given<Bar>()
    }

    @Test
    fun testChildOverridesParent() {
        val parentFoo = Foo()
        val parent = rootContext { given { parentFoo } }
        val childFoo = Foo()
        val child = parent.childContext { given { childFoo } }
        assertSame(childFoo, child.given<Foo>())
    }

    @Test
    fun testMap() {
        val context = rootContext {
            map<String, Command> {
                put("a") { CommandA() }
                put("b") { CommandB() }
                put("c") { CommandC() }
            }
        }

        val map = context.given<Map<String, Command>>()
        assertEquals(3, map.size)
        assertTrue(map["a"] is CommandA)
        assertTrue(map["b"] is CommandB)
        assertTrue(map["c"] is CommandC)
    }

    @Test
    fun testMapParentChild() {
        val parent = rootContext {
            map<String, Command> {
                put("a") { CommandA() }
            }
        }

        val parentMap = parent.given<Map<String, Command>>()
        assertEquals(1, parentMap.size)
        assertTrue(parentMap["a"] is CommandA)

        val child = parent.childContext {
            map<String, Command> {
                put("b") { CommandB() }
            }
        }

        val childMap = child.given<Map<String, Command>>()
        assertEquals(2, childMap.size)
        assertTrue(childMap["a"] is CommandA)
        assertTrue(childMap["b"] is CommandB)
    }

    @Test
    fun testSet() {
        val context = rootContext {
            set<Command> {
                add { CommandA() }
                add { CommandB() }
                add { CommandC() }
            }
        }

        val set = context.given<Set<Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0] is CommandA)
        assertTrue(set[1] is CommandB)
        assertTrue(set[2] is CommandC)
    }

    @Test
    fun testSetParentChild() {
        val parent = rootContext {
            set<Command> {
                add { CommandA() }
            }
        }

        val parentSet = parent.given<Set<Command>>().toList()
        assertEquals(1, parentSet.size)
        assertTrue(parentSet[0] is CommandA)

        val child = parent.childContext {
            set<Command> {
                add { CommandB() }
            }
        }

        val childSet = child.given<Set<Command>>().toList()
        assertEquals(2, childSet.size)
        assertTrue(childSet[0] is CommandA)
        assertTrue(childSet[1] is CommandB)
    }

}
