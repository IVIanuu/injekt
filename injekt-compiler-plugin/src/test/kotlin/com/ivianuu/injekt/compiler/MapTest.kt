package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Lazy
import com.ivianuu.injekt.Provider
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

class MapTest {

    @Test
    fun testMap() = codegen(
        """
        interface TestComponent {
            val map: Map<KClass<out Command>, Command>
        }
        
        @Factory
        fun create(): TestComponent {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return createImpl()
        }
        
        fun invoke() = create().map
    """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class] is CommandA)
        assertTrue(map[CommandB::class] is CommandB)
        assertTrue(map[CommandC::class] is CommandC)
    }

    @Test
    fun testMapOfProvider() = codegen(
        """
        interface TestComponent {
            val map: Map<KClass<out Command>, @Provider () -> Command>
        }
        
        @Factory
        fun create(): TestComponent {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return createImpl()
        }
        
        fun invoke() = create().map
    """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, @Provider () -> Command>>()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class]!!() is CommandA)
        assertTrue(map[CommandB::class]!!() is CommandB)
        assertTrue(map[CommandC::class]!!() is CommandC)
    }

    @Test
    fun testMapOfLazy() = codegen(
        """
        interface TestComponent2 {
            val map: Map<KClass<out Command>, @Lazy () -> Command>
        }
        
        @Factory
        fun create(): TestComponent2 {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return createImpl()
        }
        
        fun invoke() = create().map
    """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, @Lazy () -> Command>>()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class]!!() is CommandA)
        assertTrue(map[CommandB::class]!!() is CommandB)
        assertTrue(map[CommandC::class]!!() is CommandC)
    }

    @Test
    fun testEmptyMap() = codegen(
        """
        interface TestComponent {
            val map: Map<KClass<out Command>, Command>
        }
        
        @Factory
        fun create(): TestComponent {
            map<KClass<out Command>, Command>()
            return createImpl()
        }
        
        fun invoke() = create().map
    """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(0, map.size)
    }

    @Test
    fun testSingleEntryMap() = codegen(
        """
        interface TestComponent {
            val map: Map<KClass<out Command>, Command>
        }
        
        @Factory
        fun create(): TestComponent {
            transient { CommandA() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
            }
            return createImpl()
        }
        
        fun invoke() = create().map
    """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(1, map.size)
        assertTrue(map[CommandA::class] is CommandA)
    }

    @Test
    fun testMapOverrideFails() = codegen(
        """
        interface TestComponent {
            val map: Map<String, Command>
        }
        
        @Factory
        fun create(): TestComponent {
            transient { CommandA() }
            transient { CommandB() }
            map<String, Command> {
                put<CommandA>("a")
                put<CommandB>("a")
            }
            return createImpl()
        }
    """
    ) {
        assertInternalError("already bound")
    }

    @Test
    fun testNestedMap() = codegen(
        """
        interface ParentComponent {
            val map: Map<KClass<out Command>, Command>
            val childFactory: @ChildFactory () -> ChildComponent
        }
        
        interface ChildComponent {
            val map: Map<KClass<out Command>, Command>
        }
        
        @Factory
        fun createParent(): ParentComponent {
            transient { CommandA() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
            }
            childFactory(::createChild)
            return createImpl()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            map<KClass<out Command>, Command> {
                put<CommandB>(CommandB::class)
            }
            return createImpl()
        }
        
        fun invoke() = createParent().childFactory().map
    """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(2, map.size)
        assertTrue(map[CommandA::class] is CommandA)
        assertTrue(map[CommandB::class] is CommandB)
    }

    @Test
    fun testNestedOverrideFails() = codegen(
        """
        interface ParentComponent {
            val map: Map<KClass<out Command>, Command>
            val childFactory: @ChildFactory () -> ChildComponent
        }
        
        interface ChildComponent {
            val map: Map<KClass<out Command>, Command>
        }
        
        @Factory
        fun createParent(): ParentComponent {
            transient { CommandA() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
            }
            childFactory(::createChild)
            return createImpl()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            map<KClass<out Command>, Command> {
                put<CommandB>(CommandA::class)
            }
            return createImpl()
        }
         """
    ) {
        assertInternalError("already bound")
    }

}