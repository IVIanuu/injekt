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
            @Given fun commandA() = CommandA()
            @GivenSet fun commandAIntoSet(commandA: CommandA = given): Set<Command> = setOf(commandA)
            @Given fun commandB() = CommandB() 
            @GivenSet fun commandBIntoSet(commandB: CommandB = given): Set<Command> = setOf(commandB)
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
            @Given fun commandA() = CommandA()
            @GivenSet fun commandAIntoSet(commandA: CommandA = given): Set<Command> = setOf(commandA)

            class InnerObject {
                @Given fun commandB() = CommandB() 
                @GivenSet fun commandBIntoSet(commandB: CommandB = given): Set<Command> = setOf(commandB)

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

    /*
    @Test
    fun testUndeclaredSet() = codegen(
        """
            @Component abstract class SetComponent {
                abstract val set: Set<Command>
            }
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
            
            @Component abstract class SetComponent {
                abstract val stringSet: Set<String>
                abstract val intSet: Set<Int>
            }
        """
    )

 */

}