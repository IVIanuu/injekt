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
                @Binding 
                fun commandA() = CommandA()
                
                @MapEntries
                fun commandAIntoMap(
                    commandA: CommandA
                ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)
                
                @Binding 
                fun commandB() = CommandB()
        
                @MapEntries 
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
                @Binding 
                fun commandA() = CommandA()
                
                @MapEntries
                fun commandAIntoMap(commandA: CommandA): Map<KClass<out Command>, Command> = 
                    mapOf(CommandA::class to commandA)
            }
            
            @Module
            object ChildModule {
                @Binding 
                fun commandB() = CommandB()
        
                @MapEntries
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
                @Binding 
                fun commandA(@Assisted arg: String) = CommandA()
                
                @MapEntries
                fun commandAIntoMap(
                    commandAFactory: (String) -> CommandA
                ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandA::class to commandAFactory)
                
                @Binding 
                fun commandB(@Assisted arg: String) = CommandB()
        
                @MapEntries 
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
            @Component
            abstract class TestComponent {
                abstract val map: Map<KClass<out Command>, Command>
            }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testSimpleSet() = codegen(
        """
            @Component
            abstract class SetComponent {
                abstract val set: Set<Command>
            
                @Binding 
                protected fun commandA() = CommandA()
                
                @SetElements
                protected fun commandAIntoSet(commandA: CommandA): Set<Command> = setOf(commandA)
                
                @Binding 
                protected fun commandB() = CommandB()
                
                @SetElements
                protected fun commandBIntoSet(commandB: CommandB): Set<Command> = setOf(commandB)
            }
         
            fun invoke(): Set<Command> {
                return SetComponentImpl().set
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
                @Binding 
                fun commandA() = CommandA()
                
                @SetElements
                fun commandAIntoSet(commandA: CommandA): Set<Command> = 
                    setOf(commandA)
            }
            
            @Module
            object ChildModule {
                @Binding 
                fun commandB() = CommandB()
        
                @SetElements
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
                @Binding 
                fun commandA(@Assisted arg: String) = CommandA()
                
                @SetElements
                fun commandAIntoSet(
                    commandAFactory: (String) -> CommandA
                ): Set<(String) -> Command> = setOf(commandAFactory)
                
                @Binding 
                fun commandB(@Assisted arg: String) = CommandB()
        
                @SetElements
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
            @Component
            abstract class SetComponent {
                abstract val set: Set<Command>
            }
        """
    ) {
        assertInternalError("no binding")
    }

    // todo test child overrides parent
    // todo test input overrides implicit
}
