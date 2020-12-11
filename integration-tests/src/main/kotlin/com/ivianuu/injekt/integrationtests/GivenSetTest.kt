package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.Command
import com.ivianuu.injekt.test.CommandA
import com.ivianuu.injekt.test.CommandB
import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert
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
            @Component abstract class ParentSetComponent {
                abstract val set: Set<Command>
                abstract val childSetComponentFactory: () -> ChildSetComponent
            
                @Binding protected fun commandA() = CommandA()
                
                @SetElements
                protected fun commandAIntoSet(commandA: CommandA): Set<Command> = 
                    setOf(commandA)
            }
            
            @ChildComponent
            abstract class ChildSetComponent {
                abstract val set: Set<Command>
            
                @Binding protected fun commandB() = CommandB()
                
                @SetElements
                protected fun commandBIntoSet(commandB: CommandB): Set<Command> = 
                    setOf(commandB)
            }
         
            fun invoke(): Pair<Set<Command>, Set<Command>> {
                val parent = component<ParentSetComponent>()
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
            @Component abstract class SetComponent {
                abstract val set: Set<(String) -> Command>
                
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
            }
            fun invoke(): Set<(String) -> Command> {
                return component<SetComponent>().set
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
            @Component abstract class TestComponent {
                abstract val map: Set<Command>
            }
        """
    )

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

    @Test
    fun testScopedSet() = codegen(
        """
            @Binding fun commandA() = CommandA()
            
            @Scoped(SetComponent::class)
            @SetElements fun commandAIntoSet(commandA: CommandA): Set<Command> = setOf(commandA)
            
            @Component abstract class SetComponent {
                abstract val set: Set<Command>
                
                @Binding protected fun commandB() = CommandB()
                
                @SetElements protected fun commandBIntoSet(commandB: CommandB): Set<Command> = setOf(commandB)
            }
         
            fun invoke(): Pair<Set<Command>, Set<Command>> {
                val component = component<SetComponent>()
                return component.set to component.set
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Set<Command>, Set<Command>>>()
            .let { it.first.toList() to it.second.toList() }
        Assert.assertSame(a[0], b[0])
        Assert.assertNotSame(a[1], b[1])
    }

}