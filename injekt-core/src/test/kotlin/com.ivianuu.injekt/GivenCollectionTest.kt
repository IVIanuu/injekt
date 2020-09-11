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

import org.junit.Assert
import org.junit.Test

class GivenCollectionTest {

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
        Assert.assertEquals(3, map.size)
        Assert.assertTrue(map["a"] is CommandA)
        Assert.assertTrue(map["b"] is CommandB)
        Assert.assertTrue(map["c"] is CommandC)
    }

    @Test
    fun testMapParentChild() {
        val parent = rootContext {
            map<String, Command> {
                put("a") { CommandA() }
            }
        }

        val parentMap = parent.given<Map<String, Command>>()
        Assert.assertEquals(1, parentMap.size)
        Assert.assertTrue(parentMap["a"] is CommandA)

        val child = parent.childContext {
            map<String, Command> {
                put("b") { CommandB() }
            }
        }

        val childMap = child.given<Map<String, Command>>()
        Assert.assertEquals(2, childMap.size)
        Assert.assertTrue(childMap["a"] is CommandA)
        Assert.assertTrue(childMap["b"] is CommandB)
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
        Assert.assertEquals(3, set.size)
        Assert.assertTrue(set[0] is CommandA)
        Assert.assertTrue(set[1] is CommandB)
        Assert.assertTrue(set[2] is CommandC)
    }

    @Test
    fun testSetParentChild() {
        val parent = rootContext {
            set<Command> {
                add { CommandA() }
            }
        }

        val parentSet = parent.given<Set<Command>>().toList()
        Assert.assertEquals(1, parentSet.size)
        Assert.assertTrue(parentSet[0] is CommandA)

        val child = parent.childContext {
            set<Command> {
                add { CommandB() }
            }
        }

        val childSet = child.given<Set<Command>>().toList()
        Assert.assertEquals(2, childSet.size)
        Assert.assertTrue(childSet[0] is CommandA)
        Assert.assertTrue(childSet[1] is CommandB)
    }

}
