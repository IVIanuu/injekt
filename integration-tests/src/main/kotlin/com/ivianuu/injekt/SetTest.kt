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
        @Reader 
        fun commandA() = CommandA()
        
        @SetElements(TestComponent::class) 
        @Reader
        fun commandAIntoSet(): Set<Command> = setOf(given<CommandA>())
        
        @Given 
        @Reader
        fun commandB() = CommandB()
        
        @SetElements(TestComponent::class) 
        @Reader
        fun commandBIntoSet(): Set<Command> = setOf(given<CommandB>())
        
        @Given 
        @Reader
        fun commandC() = CommandC()
        
        @SetElements(TestComponent::class)
        @Reader
        fun commandCIntoSet(): Set<Command> = setOf(given<CommandC>())
        
        fun invoke(): Set<Command> {
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
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
    fun testUndeclaredSet() = codegen(
        """
        fun invoke(): Set<Command> {
            initializeComponents()
            val component = componentFactory<TestComponent.Factory>().create()
            return component.runReader { given<Set<Command>>() }
        }
        """
    ) {
        assertInternalError("no binding found")
    }

    @Test
    fun testNestedSet() = codegen(
        """
        @Given 
        @Reader 
        fun commandA() = CommandA()
        
        @SetElements(TestParentComponent::class) 
        @Reader
        fun commandAIntoSet(): Set<Command> = setOf(given<CommandA>())
        
        @Given 
        @Reader 
        fun commandB() = CommandB()
        
        @SetElements(TestChildComponent::class) 
        @Reader
        fun commandBIntoSet(): Set<Command> = setOf(given<CommandB>())
        
        fun invoke(): Pair<Set<Command>, Set<Command>> {
            initializeComponents()
            val parentComponent = componentFactory<TestParentComponent.Factory>().create()
            val childComponent = parentComponent.runReader { given<TestChildComponent.Factory>().create() }
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