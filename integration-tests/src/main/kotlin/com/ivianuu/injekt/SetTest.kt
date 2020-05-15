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

class SetTest {

    @Test
    fun testSet() = codegen(
        """
        @Factory
        fun invoke(): Set<Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
            return createInstance()
        }
         """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0] is CommandA)
        assertTrue(set[1] is CommandB)
        assertTrue(set[2] is CommandC)
    }

    @Test
    fun testSetOfProvider() = codegen(
        """
        @Factory
        fun invoke(): Set<@Provider () -> Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
            return createInstance()
        }
         """
    ) {
        val set =
            invokeSingleFile<Set<@Provider () -> Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0]() is CommandA)
        assertTrue(set[1]() is CommandB)
        assertTrue(set[2]() is CommandC)
    }

    @Test
    fun testSetOfLazy() = codegen(
        """
        @Factory
        fun invoke(): Set<@Lazy () -> Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
            return createInstance()
        }
         """
    ) {
        val set =
            invokeSingleFile<Set<@Lazy () -> Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0]() is CommandA)
        assertTrue(set[1]() is CommandB)
        assertTrue(set[2]() is CommandC)
    }

    @Test
    fun testEmptySet() = codegen(
        """
        @Factory
        fun invoke(): Set<Command> {
            set<Command>()
            return createInstance()
        }
         """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        assertEquals(0, set.size)
    }

    @Test
    fun testUndeclaredSet() = codegen(
        """
        @Factory
        fun create(): Set<Command> {
            return createInstance()
        }
        """
    ) {
        assertInternalError("no binding found")
    }

    @Test
    fun testSingleElementSet() = codegen(
        """
        @Factory
        fun invoke(): Set<Command> {
            transient { CommandA() }
            set<Command> {
                add<CommandA>()
            }
            return createInstance()
        }
         """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        assertEquals(1, set.size)
    }

    @Test
    fun testSetOverridesFails() = codegen(
        """
        @Factory
        fun create(): Set<Command> {
            transient { CommandA() }
            transient { CommandB() }
            set<Command> {
                add<CommandA>()
                add<CommandA>()
            }
            return createInstance()
        }
    """
    ) {
        assertInternalError("already bound")
    }

    @Test
    fun testNestedSet() = codegen(
        """
        interface ParentComponent {
            val set: Set<Command>
            val childFactory: @ChildFactory () -> ChildComponent
        }
        
        interface ChildComponent {
            val set: Set<Command>
        }
        
        @Factory
        fun createParent(): ParentComponent {
            transient { CommandA() }
            set<Command> {
                add<CommandA>()
            }
            childFactory(::createChild)
            return createImpl()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            set<Command> {
                add<CommandB>()
            }
            return createImpl()
        }
        
        fun invoke() = createParent().childFactory().set
    """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        assertEquals(2, set.size)
        assertTrue(set[0] is CommandA)
        assertTrue(set[1] is CommandB)
    }

    @Test
    fun testNestedOverrideFails() = codegen(
        """
        interface ParentComponent {
            val set: Set<Command>
            val childFactory: @ChildFactory () -> ChildComponent
        }
        
        interface ChildComponent {
            val set: Set<Command>
        }
        
        @Factory
        fun createParent(): ParentComponent {
            transient { CommandA() }
            set<Command> {
                add<CommandA>()
            }
            childFactory(::createChild)
            return createImpl()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            set<Command> {
                add<CommandA>()
            }
            return createImpl()
        }
         """
    ) {
        assertInternalError("already bound")
    }

}
