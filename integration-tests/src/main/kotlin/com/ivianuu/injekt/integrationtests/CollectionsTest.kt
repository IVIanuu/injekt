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

import com.ivianuu.injekt.test.Command
import com.ivianuu.injekt.test.CommandA
import com.ivianuu.injekt.test.CommandB
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

class CollectionsTest {

    @Test
    fun testSimpleMap() = codegen(
        """
            @Given 
            fun commandA() = CommandA()
            
            @GivenMapEntries
            fun commandAIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandA::class to given<CommandA>())
            
            @Given 
            fun commandB() = CommandB()
    
            @GivenMapEntries 
            fun commandBIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandB::class to given<CommandB>())
         
            fun invoke(): Map<KClass<out Command>, Command> {
                return rootFactory<TestContext>().runReader { given<Map<KClass<out Command>, Command>>() }
            }
        """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(2, map.size)
        assertTrue(map[CommandA::class] is CommandA)
        assertTrue(map[CommandB::class] is CommandB)
    }

    @Test
    fun testNestedMap() = codegen(
        """
            @Given 
            fun commandA() = CommandA()
            
            @GivenMapEntries(TestParentContext::class)
            fun commandAIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandA::class to given<CommandA>())
            
            @Given 
            fun commandB() = CommandB()
    
            @GivenMapEntries(TestChildContext::class)
            fun commandBIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandB::class to given<CommandB>())
         
            fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
                return rootFactory<TestParentContext>().runReader { 
                    given<Map<KClass<out Command>, Command>>() to childContext<TestChildContext>().runReader {
                        given<Map<KClass<out Command>, Command>>()
                    }
                }
            }
        """
    ) {
        val (parentMap, childMap) =
            invokeSingleFile<Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>>>()
        assertEquals(1, parentMap.size)
        assertTrue(parentMap[CommandA::class] is CommandA)
        assertEquals(2, childMap.size)
        assertTrue(childMap[CommandA::class] is CommandA)
        assertTrue(childMap[CommandB::class] is CommandB)
    }

    @Test
    fun testAssistedMap() = codegen(
        """
            @Given 
            fun commandA(arg: String) = CommandA()
            
            @GivenMapEntries
            fun commandAIntoMap(): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandA::class to given<(String) -> CommandA>())
            
            @Given 
            fun commandB(arg: String) = CommandB()
    
            @GivenMapEntries 
            fun commandBIntoMap(): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandB::class to given<(String) -> CommandB>())
    
            fun invoke(): Map<KClass<out Command>, (String) -> Command> {
                return rootFactory<TestContext>().runReader { given<Map<KClass<out Command>, (String) -> Command>>() }
            }
        """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, (String) -> Command>>()
        assertEquals(2, map.size)
        assertTrue(map[CommandA::class]!!("a") is CommandA)
        assertTrue(map[CommandB::class]!!("b") is CommandB)
    }

    @Test
    fun testUndeclaredMap() = codegen(
        """
            @RootFactory
            typealias MyFactory = () -> TestComponent1<Map<KClass<out Command>, Command>>
        """
    ) {
        assertInternalError("no given")
    }

    @Test
    fun testSimpleSet() = codegen(
        """
            @Given 
            fun commandA() = CommandA()
            
            @GivenSetElements
            fun commandAIntoSet(): Set<Command> = setOf(given<CommandA>())
            
            @Given 
            fun commandB() = CommandB()
            
            @GivenSetElements
            fun commandBIntoSet(): Set<Command> = setOf(given<CommandB>())
    
            fun invoke(): Set<Command> {
                return rootFactory<TestContext>().runReader { given<Set<Command>>() }
            }
        """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        assertEquals(2, set.size)
        assertTrue(set.any { it is CommandA })
        assertTrue(set.any { it is CommandB })
    }

    @Test
    fun testNestedSet() = codegen(
        """
            @Given 
            fun commandA() = CommandA()
            
            @GivenSetElements(TestParentContext::class)
            fun commandAIntoSet(): Set<Command> = setOf(given<CommandA>())
            
            @Given 
            fun commandB() = CommandB()
            
            @GivenSetElements(TestChildContext::class)
            fun commandBIntoSet(): Set<Command> = setOf(given<CommandB>())
    
            fun invoke(): Pair<Set<Command>, Set<Command>> {
                return rootFactory<TestParentContext>().runReader { 
                    given<Set<Command>>() to childContext<TestChildContext>().runReader {
                        given<Set<Command>>()
                    }
                }
            }
        """
    ) {
        val (parentSet, childSet) = invokeSingleFile<Pair<Set<Command>, Set<Command>>>().toList()
        assertEquals(1, parentSet.size)
        assertTrue(parentSet.any { it is CommandA })
        assertEquals(2, childSet.size)
        assertTrue(childSet.any { it is CommandA })
        assertTrue(childSet.any { it is CommandB })
    }

    @Test
    fun testAssistedSet() = codegen(
        """
            @Given 
            fun commandA(arg: String) = CommandA()
            
            @GivenSetElements
            fun commandAIntoSet(): Set<(String) -> Command> = setOf(given<(String) -> CommandA>())
            
            @Given 
            fun commandB(arg: String) = CommandB()
            
            @GivenSetElements
            fun commandBIntoSet(): Set<(String) -> Command> = setOf(given<(String) -> CommandB>())
    
            fun invoke(): Set<(String) -> Command> {
                return rootFactory<TestContext>().runReader { given<Set<(String) -> Command>>() }
            }
        """
    ) {
        val set = invokeSingleFile<Set<(String) -> Command>>().toList()
        assertEquals(2, set.size)
        assertTrue(set.any { it("a") is CommandA })
        assertTrue(set.any { it("b") is CommandB })
    }


    @Test
    fun testUndeclaredSet() = codegen(
        """
            @RootFactory
            typealias MyFactory = () -> TestComponent1<Set<Command>>
        """
    ) {
        assertInternalError("no given")
    }

    // todo test child overrides parent
    // todo test input overrides implicit

}
