package com.ivianuu.injekt.compiler

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

}
