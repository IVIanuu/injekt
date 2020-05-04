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
            val map: Map<kotlin.reflect.KClass<out Command>, Command>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<kotlin.reflect.KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
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
            val map: Map<kotlin.reflect.KClass<out Command>, @Provider () -> Command>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<kotlin.reflect.KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
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
        interface TestComponent {
            val map: Map<kotlin.reflect.KClass<out Command>, @Lazy () -> Command>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<kotlin.reflect.KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
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
    fun testMapOverrideFails() = codegen(
        """
        interface TestComponent {
            val map: Map<String, Command>
        }
        
        @Factory
        fun create(): TestComponent = createImplementation {
            transient { CommandA() }
            transient { CommandB() }
            map<String, Command> {
                put<CommandA>("a")
                put<CommandB>("a")
            }
        }
    """
    ) {
        assertInternalError()
    }

}