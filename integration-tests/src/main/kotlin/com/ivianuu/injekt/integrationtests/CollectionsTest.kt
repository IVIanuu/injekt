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
import junit.framework.Assert.assertNotSame
import junit.framework.Assert.assertSame
import junit.framework.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

class CollectionsTest {

    @Test
    fun testSimpleMap() = codegen(
        """
            @Binding 
            fun commandA() = CommandA()
            
            @MapEntries fun commandAIntoMap(
                commandA: CommandA
            ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)

            @Binding 
            fun commandB() = CommandB()
    
            @MapEntries 
            fun commandBIntoMap(
                commandB: CommandB
            ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)
            
            fun invoke(): Map<KClass<out Command>, Command> {
                return create<Map<KClass<out Command>, Command>>()
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
            @Component interface ParentMapComponent {
                val map: Map<KClass<out Command>, Command>
                val childMapComponentFactory: (ChildMapComponent.Module) -> ChildMapComponent

                class Module {
                
                    @Binding fun commandA() = CommandA()
                    
                    @MapEntries
                    fun commandAIntoMap(commandA: CommandA): Map<KClass<out Command>, Command> = 
                        mapOf(CommandA::class to commandA)   
                    }
            }
            
            @Component interface ChildMapComponent {
                val map: Map<KClass<out Command>, Command>

                class Module {
            
                    @Binding fun commandB() = CommandB()
                
                    @MapEntries
                    fun commandBIntoMap(commandB: CommandB): Map<KClass<out Command>, Command> = 
                        mapOf(CommandB::class to commandB)
                    }
            }
         
            fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
                val parent = create<ParentMapComponent>(ParentMapComponent.Module())
                return parent.map to parent.childMapComponentFactory(ChildMapComponent.Module()).map
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
    fun testChildMapOverridesParent() = codegen(
        """
            @Component interface ParentMapComponent {
                val map: Map<String, String>

                val childMapComponentFactory: (ChildMapComponent.Module) -> ChildMapComponent
            
                class Module {
                    @Binding fun value() = "parent"
                
                    @MapEntries fun valueIntoMap(value: String): Map<String, String> = 
                        mapOf("key" to value)
                }
            }
            
            @Component interface ChildMapComponent {
                val map: Map<String, String>

                class Module {
                    @Binding fun value() = "child"
                
                    @MapEntries fun valueIntoMap(value: String): Map<String, String> = 
                        mapOf("key" to value)
                }
            }
         
            fun invoke(): Map<String, String> {
                val parent = create<ParentMapComponent>(ParentMapComponent.Module())
                return parent.childMapComponentFactory(ChildMapComponent.Module()).map
            }
        """
    ) {
        val map = invokeSingleFile<Map<String, String>>()
        assertEquals("child", map["key"])
    }

    @Test
    fun testAssistedMap() = codegen(
        """
            @Binding 
            fun commandA(arg: String) = CommandA()
            
            @MapEntries
            fun commandAIntoMap(
                commandAFactory: (String) -> CommandA
            ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandA::class to commandAFactory)
            
            @Binding 
            fun commandB(arg: String) = CommandB()
    
            @MapEntries 
            fun commandBIntoMap(
                commandBFactory: (String) -> CommandB
            ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandB::class to commandBFactory)
         
            fun invoke(): Map<KClass<out Command>, (String) -> Command> {
                return create<Map<KClass<out Command>, (String) -> Command>>()
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
    fun testDefaultMap() = codegen(
        """
            @Default @MapEntries fun defaultMap() = mapOf<KClass<out Command>, Command>()

            @Component interface TestComponent {
                val map: Map<KClass<out Command>, Command>
            }
        """
    )

    @Test
    fun testUndeclaredMap() = codegen(
        """
            fun invoke() = create<Map<KClass<out Command>, Command>>()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testGenericMap() = codegen(
        """ 
            @Binding fun string() = ""
            
            @Binding fun int() = 0
            
            @MapEntries fun <V> genericMap(instance: V): Map<Int, V> = mapOf(instance.hashCode() to instance)
            
            @Component interface MapComponent {
                val stringMap: Map<Int, String>
                val intMap: Map<Int, Int>
            }
        """
    )

    @Test
    fun testScopedMap() = codegen(
        """
            @Binding 
            fun commandA() = CommandA()
            
            @Scoped(TestScope1::class)
            @MapEntries fun commandAIntoMap(
                commandA: CommandA
            ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)
             
            @Binding fun commandB() = CommandB()
    
            @MapEntries fun commandBIntoMap(
                commandB: CommandB
            ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)
            
            fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
                val mapFactory = create<@Scoped(TestScope1::class) () -> Map<KClass<out Command>, Command>>()
                return mapFactory() to mapFactory()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>>>()
        assertSame(a[CommandA::class], b[CommandA::class])
        assertNotSame(a[CommandB::class], b[CommandB::class])
    }

    @Test
    fun testSimpleSet() = codegen(
        """
            @Binding 
            fun commandA() = CommandA()
            
            @SetElements fun commandAIntoSet(commandA: CommandA): Set<Command> = setOf(commandA)

            @Binding 
            fun commandB() = CommandB()
                
            @SetElements
            fun commandBIntoSet(commandB: CommandB): Set<Command> = setOf(commandB)
         
            fun invoke(): Set<Command> {
                return create<Set<Command>>()
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
            @Component interface ParentSetComponent {
                val set: Set<Command>

                val childSetComponentFactory: (ChildSetComponent.Module) -> ChildSetComponent
            
                class Module {
                    @Binding fun commandA() = CommandA()
                    
                    @SetElements
                    fun commandAIntoSet(commandA: CommandA): Set<Command> = 
                        setOf(commandA)
                }
            }
            
            @Component interface ChildSetComponent {
                val set: Set<Command>
            
                class Module {
                    @Binding fun commandB() = CommandB()
                    
                    @SetElements
                    fun commandBIntoSet(commandB: CommandB): Set<Command> = 
                        setOf(commandB)
                }
            }
         
            fun invoke(): Pair<Set<Command>, Set<Command>> {
                val parent = create<ParentSetComponent>(ParentSetComponent.Module())
                return parent.set to parent.childSetComponentFactory(ChildSetComponent.Module()).set
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
            @Binding 
            fun commandA(arg: String) = CommandA()
            
            @SetElements fun commandAIntoSet(
                commandAFactory: (String) -> CommandA
            ): Set<(String) -> Command> = setOf(commandAFactory)
            
            @Binding 
            fun commandB(arg: String) = CommandB()
    
            @SetElements fun commandBIntoSet(
                commandBFactory: (String) -> CommandB
            ): Set<(String) -> Command> = setOf(commandBFactory)

            fun invoke(): Set<(String) -> Command> {
                return create<Set<(String) -> Command>>()
            }
        """
    ) {
        val set = invokeSingleFile<Set<(String) -> Command>>().toList()
        assertEquals(2, set.size)
        assertTrue(set.any { it("a") is CommandA })
        assertTrue(set.any { it("b") is CommandB })
    }

    @Test
    fun testDefaultSet() = codegen(
        """
            @Default @SetElements fun defaultSet() = setOf<Command>()

            @Component interface TestComponent {
                val map: Set<Command>
            }
        """
    )

    @Test
    fun testUndeclaredSet() = codegen(
        """
            fun invoke() = create<Set<Command>>()
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testGenericSet() = codegen(
        """ 
            @Binding fun string() = ""
            
            @Binding fun int() = 0
            
            @SetElements fun <T> genericSet(instance: T) = setOf(instance)
            
            @Component interface SetComponent {
                val stringSet: Set<String>
                val intSet: Set<Int>
            }
        """
    )

    @Test
    fun testScopedSet() = codegen(
        """
            @Binding fun commandA() = CommandA()
            
            @Scoped(TestScope1::class)
            @SetElements fun commandAIntoSet(commandA: CommandA): Set<Command> = setOf(commandA)
            
            @Binding fun commandB() = CommandB()
                
            @SetElements fun commandBIntoSet(commandB: CommandB): Set<Command> = setOf(commandB)

            fun invoke(): Pair<Set<Command>, Set<Command>> {
                val setFactory = create<@Scoped(TestScope1::class) () -> Set<Command>>()
                return setFactory() to setFactory()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Set<Command>, Set<Command>>>()
            .let { it.first.toList() to it.second.toList() }
        assertSame(a[0], b[0])
        assertNotSame(a[1], b[1])
    }

}
