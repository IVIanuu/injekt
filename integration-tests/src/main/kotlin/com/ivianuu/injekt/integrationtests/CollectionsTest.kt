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
            @Module
            object MapModule {
                @Given 
                fun commandA() = CommandA()
                
                @GivenMapEntries
                fun commandAIntoMap(
                    commandA: CommandA
                ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)
                
                @Given 
                fun commandB() = CommandB()
        
                @GivenMapEntries 
                fun commandBIntoMap(
                    commandB: CommandB
                ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)
            }
            
            @RootFactory
            typealias MapFactory = (MapModule) -> TestComponent1<Map<KClass<out Command>, Command>>
         
            fun invoke(): Map<KClass<out Command>, Command> {
                return rootFactory<MapFactory>()(MapModule).a
            }
        """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(2, map.size)
        assertTrue(map[CommandA::class] is CommandA)
        assertTrue(map[CommandB::class] is CommandB)
    }

    @Test
    fun testNestedMap() = codegen(
        """
            @Module
            object ParentModule {
                @Given 
                fun commandA() = CommandA()
                
                @GivenMapEntries
                fun commandAIntoMap(commandA: CommandA): Map<KClass<out Command>, Command> = 
                    mapOf(CommandA::class to commandA)
            }
            
            @Module
            object ChildModule {
                @Given 
                fun commandB() = CommandB()
        
                @GivenMapEntries
                fun commandBIntoMap(commandB: CommandB): Map<KClass<out Command>, Command> = 
                    mapOf(CommandB::class to commandB)
            }
            
            @RootFactory
            typealias MyParentFactory = (ParentModule) -> TestComponent2<Map<KClass<out Command>, Command>, MyChildFactory>
            
            @ChildFactory
            typealias MyChildFactory = (ChildModule) -> TestComponent1<Map<KClass<out Command>, Command>>
         
            fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
                val parent = rootFactory<MyParentFactory>()(ParentModule)
                return parent.a to parent.b(ChildModule).a
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
            @Module
            object MapModule {
                @Given 
                fun commandA(@Assisted arg: String) = CommandA()
                
                @GivenMapEntries
                fun commandAIntoMap(
                    commandAFactory: (String) -> CommandA
                ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandA::class to commandAFactory)
                
                @Given 
                fun commandB(@Assisted arg: String) = CommandB()
        
                @GivenMapEntries 
                fun commandBIntoMap(
                    commandBFactory: (String) -> CommandB
                ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandB::class to commandBFactory)
            }

            @RootFactory 
            typealias MapFactory = (MapModule) -> TestComponent1<Map<KClass<out Command>, (String) -> Command>>
         
            fun invoke(): Map<KClass<out Command>, (String) -> Command> {
                return rootFactory<MapFactory>()(MapModule).a
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
            @Module
            object SetModule {
                @Given 
                fun commandA() = CommandA()
                
                @GivenSetElements
                fun commandAIntoSet(commandA: CommandA): Set<Command> = setOf(commandA)
                
                @Given 
                fun commandB() = CommandB()
                
                @GivenSetElements
                fun commandBIntoSet(commandB: CommandB): Set<Command> = setOf(commandB)
            }
    
            @RootFactory
            typealias SetFactory = (SetModule) -> TestComponent1<Set<Command>>
         
            fun invoke(): Set<Command> {
                return rootFactory<SetFactory>()(SetModule).a
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
            @Module
            object ParentModule {
                @Given 
                fun commandA() = CommandA()
                
                @GivenSetElements
                fun commandAIntoSet(commandA: CommandA): Set<Command> = 
                    setOf(commandA)
            }
            
            @Module
            object ChildModule {
                @Given 
                fun commandB() = CommandB()
        
                @GivenSetElements
                fun commandBIntoSet(commandB: CommandB): Set<Command> = 
                    setOf(commandB)
            }
            
            @RootFactory
            typealias MyParentFactory = (ParentModule) -> TestComponent2<Set<Command>, MyChildFactory>
            
            @ChildFactory
            typealias MyChildFactory = (ChildModule) -> TestComponent1<Set<Command>>
         
            fun invoke(): Pair<Set<Command>, Set<Command>> {
                val parent = rootFactory<MyParentFactory>()(ParentModule)
                return parent.a to parent.b(ChildModule).a
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
            @Module
            object SetModule {
                @Given 
                fun commandA(@Assisted arg: String) = CommandA()
                
                @GivenSetElements
                fun commandAIntoSet(
                    commandAFactory: (String) -> CommandA
                ): Set<(String) -> Command> = setOf(commandAFactory)
                
                @Given 
                fun commandB(@Assisted arg: String) = CommandB()
        
                @GivenSetElements
                fun commandBIntoSet(
                    commandBFactory: (String) -> CommandB
                ): Set<(String) -> Command> = setOf(commandBFactory)
            }

            @RootFactory 
            typealias SetFactory = (SetModule) -> TestComponent1<Set<(String) -> Command>>
         
            fun invoke(): Set<(String) -> Command> {
                return rootFactory<SetFactory>()(SetModule).a
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
