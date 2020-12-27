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
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class GivenSetTest {

    @Test
    fun testSimpleSet() = codegen(
        """
            @GivenSetElement fun commandA(): Command = CommandA()
            @GivenSetElement fun commandB(): Command = CommandB() 
            fun invoke() = given<Set<Command>>()
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
            @GivenSetElement fun commandA(): Command = CommandA()

            class InnerObject {
                @GivenSetElement fun commandB(): Command = CommandB()
                val set = given<Set<Command>>()
            }

            fun invoke() = given<Set<Command>>() to InnerObject().set
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
    fun testEmptyDefault() = codegen(
        """
            fun invoke() = given<Set<Command>>()
        """
    ) {
        assertEquals(emptySet<Command>(), invokeSingleFile())
    }

}