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
            @Component
            abstract class MapComponent {
                abstract val map: Map<KClass<out Command>, Command>
            
                @Binding 
                protected fun commandA() = CommandA()
                
                @MapEntries
                protected fun commandAIntoMap(
                    commandA: CommandA
                ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)
                
                @Binding 
                protected fun commandB() = CommandB()
        
                @MapEntries 
                protected fun commandBIntoMap(
                    commandB: CommandB
                ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)
            }
            
            fun invoke(): Map<KClass<out Command>, Command> {
                return MapComponentImpl().map
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
            @Component
            abstract class ParentMapComponent {
                abstract val map: Map<KClass<out Command>, Command>

                abstract val childMapComponentFactory: () -> ChildMapComponent
            
                @Binding
                protected fun commandA() = CommandA()
                
                @MapEntries
                protected fun commandAIntoMap(commandA: CommandA): Map<KClass<out Command>, Command> = 
                    mapOf(CommandA::class to commandA)
            }
            
            @ChildComponent
            abstract class ChildMapComponent {
                abstract val map: Map<KClass<out Command>, Command>
            
                @Binding
                protected fun commandB() = CommandB()
                
                @MapEntries
                protected fun commandBIntoMap(commandB: CommandB): Map<KClass<out Command>, Command> = 
                    mapOf(CommandB::class to commandB)
            }
         
            fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
                val parent = ParentMapComponentImpl()
                return parent.map to parent.childMapComponentFactory().map
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
            @Component
            abstract class MapComponent {
                abstract val map: Map<KClass<out Command>, (String) -> Command>
            
                @Binding 
                protected fun commandA(arg: @Assisted String) = CommandA()
                
                @MapEntries
                protected fun commandAIntoMap(
                    commandAFactory: (String) -> CommandA
                ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandA::class to commandAFactory)
                
                @Binding 
                protected fun commandB(arg: @Assisted String) = CommandB()
        
                @MapEntries 
                protected fun commandBIntoMap(
                    commandBFactory: (String) -> CommandB
                ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandB::class to commandBFactory)
            }
         
            fun invoke(): Map<KClass<out Command>, (String) -> Command> {
                return MapComponentImpl().map
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
            @Component
            abstract class ParentSetComponent {
                abstract val set: Set<Command>

                abstract val childSetComponentFactory: () -> ChildSetComponent
            
                @Binding
                protected fun commandA() = CommandA()
                
                @SetElements
                protected fun commandAIntoSet(commandA: CommandA): Set<Command> = 
                    setOf(commandA)
            }
            
            @ChildComponent
            abstract class ChildSetComponent {
                abstract val set: Set<Command>
            
                @Binding
                protected fun commandB() = CommandB()
                
                @SetElements
                protected fun commandBIntoSet(commandB: CommandB): Set<Command> = 
                    setOf(commandB)
            }
         
            fun invoke(): Pair<Set<Command>, Set<Command>> {
                val parent = ParentSetComponentImpl()
                return parent.set to parent.childSetComponentFactory().set
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
            @Component
            abstract class SetComponent {
                abstract val set: Set<(String) -> Command>
                
                @Binding 
                fun commandA(arg: @Assisted String) = CommandA()
                
                @SetElements
                fun commandAIntoSet(
                    commandAFactory: (String) -> CommandA
                ): Set<(String) -> Command> = setOf(commandAFactory)
                
                @Binding 
                fun commandB(arg: @Assisted String) = CommandB()
        
                @SetElements
                fun commandBIntoSet(
                    commandBFactory: (String) -> CommandB
                ): Set<(String) -> Command> = setOf(commandBFactory)
            }

            fun invoke(): Set<(String) -> Command> {
                return SetComponentImpl().set
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
