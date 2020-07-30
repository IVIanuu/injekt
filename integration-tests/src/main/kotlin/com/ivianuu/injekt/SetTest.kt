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

class SetTest {

    @Test
    fun testSimpleSet() = codegen(
        """
        @Given 
        fun commandA() = CommandA()
        
        @SetElements(TestComponent::class) 
        fun commandAIntoSet(): Set<Command> = setOf(given<CommandA>())
        
        @Given 
        fun commandB() = CommandB()
        
        @SetElements(TestComponent::class) 
        fun commandBIntoSet(): Set<Command> = setOf(given<CommandB>())
        
        @Given 
        fun commandC() = CommandC()
        
        @SetElements(TestComponent::class)
        fun commandCIntoSet(): Set<Command> = setOf(given<CommandC>())
        
        fun invoke(): Set<Command> {
            initializeInjekt()
            val component = rootComponent<TestComponent>()
            return component.runReader { given<Set<Command>>() }
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
    fun testEmptySet() = codegen(
        """
        fun invoke(): Set<Command> {
            initializeInjekt()
            val component = rootComponent<TestComponent>()
            return component.runReader { given<Set<Command>>() }
        }
        """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        assertEquals(0, set.size)
    }

    @Test
    fun testNestedSet() = codegen(
        """
        @Given 
        fun commandA() = CommandA()
        
        @SetElements(TestParentComponent::class) 
        fun commandAIntoSet(): Set<Command> = setOf(given<CommandA>())
        
        @Given 
        fun commandB() = CommandB()
        
        @SetElements(TestChildComponent::class) 
        fun commandBIntoSet(): Set<Command> = setOf(given<CommandB>())
        
        fun invoke(): Pair<Set<Command>, Set<Command>> {
            initializeInjekt()
            val parentComponent = rootComponent<TestParentComponent>()
            val childComponent = parentComponent.runReader { childComponent<TestChildComponent>() }
            return parentComponent.runReader {
                given<Set<Command>>() to childComponent.runReader {
                    given<Set<Command>>()
                }
            }
        }
    """
    ) {
        val pair = invokeSingleFile<Pair<Set<Command>, Set<Command>>>()
        val parentSet = pair.first.toList()
        val childSet = pair.second.toList()
        assertEquals(1, parentSet.size)
        assertTrue(parentSet[0] is CommandA)
        assertEquals(2, childSet.size)
        assertTrue(childSet[0] is CommandA)
        assertTrue(childSet[1] is CommandB)
    }

}