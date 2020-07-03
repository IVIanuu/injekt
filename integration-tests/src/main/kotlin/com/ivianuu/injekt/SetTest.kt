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

import com.ivianuu.injekt.test.Command
import com.ivianuu.injekt.test.CommandA
import com.ivianuu.injekt.test.CommandB
import com.ivianuu.injekt.test.CommandC
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class SetTest {

    @Test
    fun testSetOfValueInstance() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Set<Command>> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
            return create()
        }
        
        fun invoke() = factory().a
         """
    ) {
        val set =
            invokeSingleFile<Set<Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0] is CommandA)
        assertTrue(set[1] is CommandB)
        assertTrue(set[2] is CommandC)
    }

    @Test
    fun testSetOfValueProvider() = codegen(
        """
        @Factory
        fun invoke(): @Provider () -> Set<Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
            return create()
        }
         """
    ) {
        val set =
            invokeSingleFile<@Provider () -> Set<Command>>()().toList()
        assertEquals(3, set.size)
        assertTrue(set[0] is CommandA)
        assertTrue(set[1] is CommandB)
        assertTrue(set[2] is CommandC)
    }

    @Test
    fun testSetOfProviderInstance() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Set<@Provider () -> Command>> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
            return create()
        }
        
        fun invoke() = factory().a
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
    fun testSetOfProviderProvider() = codegen(
        """
        @Factory
        fun invoke(): @Provider () -> Set<@Provider () -> Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
            return create()
        }
         """
    ) {
        val set =
            invokeSingleFile<@Provider () -> Set<@Provider () -> Command>>()().toList()
        assertEquals(3, set.size)
        assertTrue(set[0]() is CommandA)
        assertTrue(set[1]() is CommandB)
        assertTrue(set[2]() is CommandC)
    }

    @Test
    fun testSetOfAssistedProviderInstance() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Set<@Provider (String) -> Command>> {
            transient { arg: String -> CommandA() }
            transient { arg: String -> CommandB() }
            transient { arg: String -> CommandC() }
            set<@Provider (String) -> Command> {
                add<@Provider (String) -> CommandA>()
                add<@Provider (String) -> CommandB>()
                add<@Provider (String) -> CommandC>()
            }
            return create()
        }
        
        fun invoke() = factory().a
         """
    ) {
        val set =
            invokeSingleFile<Set<@Provider (String) -> Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0]("a") is CommandA)
        assertTrue(set[1]("b") is CommandB)
        assertTrue(set[2]("c") is CommandC)
    }

    @Test
    fun testSetOfAssistedProviderProvider() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<@Provider () -> Set<@Provider (String) -> Command>> {
            transient { arg: String -> CommandA() }
            transient { arg: String -> CommandB() }
            transient { arg: String -> CommandC() }
            set<@Provider (String) -> Command> {
                add<@Provider (String) -> CommandA>()
                add<@Provider (String) -> CommandB>()
                add<@Provider (String) -> CommandC>()
            }
            return create()
        }
        
        fun invoke() = factory().a
         """
    ) {
        val set =
            invokeSingleFile<@Provider () -> Set<@Provider (String) -> Command>>()().toList()
        assertEquals(3, set.size)
        assertTrue(set[0]("a") is CommandA)
        assertTrue(set[1]("b") is CommandB)
        assertTrue(set[2]("c") is CommandC)
    }

    @Test
    fun testEmptySet() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Set<Command>> {
            set<Command>()
            return create()
        }
        
        fun invoke() = factory().a
         """
    ) {
        val set =
            invokeSingleFile<Set<Command>>().toList()
        assertEquals(0, set.size)
    }

    @Test
    fun testUndeclaredSet() = codegen(
        """
        @Factory
        fun createInstance(): TestComponent1<Set<Command>> {
            return create()
        }
        """
    ) {
        assertInternalError("no binding found")
    }

    @Test
    fun testSingleElementSet() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Set<Command>> {
            transient { CommandA() }
            set<Command> {
                add<CommandA>()
            }
            return create()
        }
        
        fun invoke() = factory().a
         """
    ) {
        val set =
            invokeSingleFile<Set<Command>>().toList()
        assertEquals(1, set.size)
    }

    @Test
    fun testSetOverridesFails() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<Set<Command>> {
            transient { CommandA() }
            transient { CommandB() }
            set<Command> {
                add<CommandA>()
                add<CommandA>()
            }
            return create()
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
            return create()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            set<Command> {
                add<CommandB>()
            }
            return create()
        }
        
        fun invoke() = createParent().childFactory().set
    """
    ) {
        val set =
            invokeSingleFile<Set<Command>>().toList()
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
            return create()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            set<Command> {
                add<CommandA>()
            }
            return create()
        }
         """
    ) {
        assertInternalError("already bound")
    }

    @Test
    fun testGenericSetBinding() = codegen(
        """
        @Module
        fun <T : Command> intoSet() { 
            set<Command> { add<T>() }
        }
        
        @Factory
        fun factory(): TestComponent1<Set<Command>> {
            transient { CommandA() }
            intoSet<CommandA>()
            transient { CommandB() }
            intoSet<CommandB>()
            transient { CommandC() }
            intoSet<CommandC>()
            return create()
        }

        fun invoke() = factory().a
         """
    ) {
        val set =
            invokeSingleFile<Set<Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0] is CommandA)
        assertTrue(set[1] is CommandB)
        assertTrue(set[2] is CommandC)
    }

    @Test
    fun testQualifiedSetOfValue() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<@TestQualifier1 Set<Any>> {
            set<@TestQualifier1 Set<Any>, Any>()
            return create()
        }

        fun invoke() = factory().a
        """
    ) {
        assertOk()
    }

    @Test
    fun testQualifiedMapOfProvider() = codegen(
        """
        @Factory
        fun factory(): TestComponent1<@TestQualifier1 Set<@Provider () -> Any>> {
            set<@TestQualifier1 Set<Any>, Any>()
            return create()
        }

        fun invoke() = factory().a
        """
    ) {
        assertOk()
    }

}
