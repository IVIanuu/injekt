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
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

class MapTest {

    @Test
    fun testSimpleMap() = codegen(
        """
        @Given 
        fun commandA() = CommandA()
        
        @MapEntries(TestComponent::class) 
        fun commandAIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandA::class to given<CommandA>())
        
        @Given 
        fun commandB() = CommandB()

        @MapEntries(TestComponent::class) 
        fun commandBIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandB::class to given<CommandB>())
        
        @Given 
        fun commandC() = CommandC()
        
        @MapEntries(TestComponent::class)
        fun commandCIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandC::class to given<CommandC>())
        
        fun invoke(): Map<KClass<out Command>, Command> {
            initializeInjekt()
            val component = rootComponent<TestComponent>()
            return component.runReader { given<Map<KClass<out Command>, Command>>() }
        }
        """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class] is CommandA)
        assertTrue(map[CommandB::class] is CommandB)
        assertTrue(map[CommandC::class] is CommandC)
    }

    @Test
    fun testEmptyMap() = codegen(
        """
        fun invoke(): Map<KClass<out Command>, Command> {
            initializeInjekt()
            val component = rootComponent<TestComponent>()
            return component.runReader { given<Map<KClass<out Command>, Command>>() }
        }
        """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(0, map.size)
    }

    @Test
    fun testNestedMap() = codegen(
        """
        @Given 
        fun commandA() = CommandA()
        
        @MapEntries(TestParentComponent::class) 
        fun commandAIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandA::class to given<CommandA>())
        
        @Given 
        fun commandB() = CommandB()
        
        @MapEntries(TestChildComponent::class) 
        fun commandBIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandB::class to given<CommandB>())
        
        fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
            initializeInjekt()
            val parentComponent = rootComponent<TestParentComponent>()
            val childComponent = parentComponent.runReader { childComponent<TestChildComponent>() }
            return parentComponent.runReader {
                given<Map<KClass<out Command>, Command>>() to childComponent.runReader {
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

}
