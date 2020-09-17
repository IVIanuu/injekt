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
        fun commandA() = CommandA()
        
        @GivenSetElements
        fun commandAIntoSet(): Set<Command> = setOf(given<CommandA>())
        
        @Given 
        fun commandB() = CommandB()
        
        @GivenSetElements
        fun commandBIntoSet(): Set<Command> = setOf(given<CommandB>())
        
        @Given 
        fun commandC() = CommandC()
        
        @GivenSetElements
        fun commandCIntoSet(): Set<Command> = setOf(given<CommandC>())
        
        fun invoke(): Set<Command> {
            return rootContext<TestContext>().runReader { given<Set<Command>>() }
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
        
        @Given 
        fun commandC(arg: String) = CommandC()
        
        @GivenSetElements
        fun commandCIntoSet(): Set<(String) -> Command> = setOf(given<(String) -> CommandC>())
        
        fun invoke(): Set<(String) -> Command> {
            return rootContext<TestContext>().runReader { given<Set<(String) -> Command>>() }
        }
        """
    ) {
        val set = invokeSingleFile<Set<(String) -> Command>>().toList()
        assertEquals(3, set.size)
        assertTrue(set[0]("a") is CommandA)
        assertTrue(set[1]("b") is CommandB)
        assertTrue(set[2]("c") is CommandC)
    }


    @Test
    fun testUndeclaredSet() = codegen(
        """
        fun invoke(): Set<Command> {
            return rootContext<TestContext>().runReader { given<Set<Command>>() }
        }
        """
    ) {
        assertInternalError("no given")
    }

}
