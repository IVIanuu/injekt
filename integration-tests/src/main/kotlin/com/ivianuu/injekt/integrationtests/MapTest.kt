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
import kotlin.reflect.KClass

class MapTest {

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
        
        @Given 
        fun commandC() = CommandC()
        
        @GivenMapEntries
        fun commandCIntoMap(): Map<KClass<out Command>, Command> = mapOf(CommandC::class to given<CommandC>())
        
        fun invoke(): Map<KClass<out Command>, Command> {
            return context<TestComponent>().runReader { given<Map<KClass<out Command>, Command>>() }
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
    fun testUndeclaredMap() = codegen(
        """
        fun invoke(): Map<KClass<out Command>, Command> {
            return context<TestComponent>().runReader { given<Map<KClass<out Command>, Command>>() }
        }
        """
    ) {
        assertInternalError("no binding")
    }

}
