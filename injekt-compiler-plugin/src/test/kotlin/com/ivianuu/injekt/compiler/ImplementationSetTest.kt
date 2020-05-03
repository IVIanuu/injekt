package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Provider
import junit.framework.Assert.assertTrue
import org.junit.Test

class ImplementationSetTest {

    @Test
    fun testSet() = codegen(
        """
        interface TestComponent {
            val set: Set<Command>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
        }
        
        fun invoke(): Set<Command> = create().set
    """
    ) {
        val set = invokeSingleFile<Set<Command>>().toList()
        assertTrue(set[0] is CommandA)
        assertTrue(set[1] is CommandB)
        assertTrue(set[2] is CommandC)
    }

    @Test
    fun testSetOfProvider() = codegen(
        """
        interface TestComponent {
            val set: Set<Provider<Command>>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
        }
        
        fun invoke(): Set<Provider<Command>> = create().set
    """
    ) {
        val set = invokeSingleFile<Set<Provider<Command>>>().toList()
        assertTrue(set[0]() is CommandA)
        assertTrue(set[1]() is CommandB)
        assertTrue(set[2]() is CommandC)
    }

    @Test
    fun testSetOfLazy() = codegen(
        """
        interface TestComponent {
            val set: Set<Lazy<Command>>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            set<Command> {
                add<CommandA>()
                add<CommandB>()
                add<CommandC>()
            }
        }
        
        fun invoke(): Set<Lazy<Command>> = create().set
    """
    ) {
        val set = invokeSingleFile<Set<Lazy<Command>>>().toList()
        assertTrue(set[0]() is CommandA)
        assertTrue(set[1]() is CommandB)
        assertTrue(set[2]() is CommandC)
    }

}
