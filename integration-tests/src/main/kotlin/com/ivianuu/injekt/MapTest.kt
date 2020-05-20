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
import kotlin.reflect.KClass

class MapTest {

    @Test
    fun testMapOfValueInstance() = codegen(
        """
        @InstanceFactory
        fun invoke(): Map<KClass<out Command>, Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return create()
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
    fun testMapOfValueProvider() = codegen(
        """
        @InstanceFactory
        fun invoke(): @Provider () -> Map<KClass<out Command>, Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return create()
        }
        """
    ) {
        val map =
            invokeSingleFile<@Provider () -> Map<KClass<out Command>, Command>>()()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class] is CommandA)
        assertTrue(map[CommandB::class] is CommandB)
        assertTrue(map[CommandC::class] is CommandC)
    }

    @Test
    fun testMapOfProviderInstance() = codegen(
        """
        @InstanceFactory
        fun invoke(): Map<KClass<out Command>, @Provider () -> Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return create()
        }
    """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, @Provider () -> Command>>()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class]!!() is CommandA)
        assertTrue(map[CommandB::class]!!() is CommandB)
        assertTrue(map[CommandC::class]!!() is CommandC)
    }

    @Test
    fun testMapOfProviderProvider() = codegen(
        """
        @InstanceFactory
        fun invoke(): @Provider () -> Map<KClass<out Command>, @Provider () -> Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return create()
        }
    """
    ) {
        val map =
            invokeSingleFile<@Provider () -> Map<KClass<out Command>, @Provider () -> Command>>()()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class]!!() is CommandA)
        assertTrue(map[CommandB::class]!!() is CommandB)
        assertTrue(map[CommandC::class]!!() is CommandC)
    }

    @Test
    fun testMapOfLazyInstance() = codegen(
        """
        @InstanceFactory
        fun invoke(): Map<KClass<out Command>, @Lazy () -> Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return create()
        }
         """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, @Lazy () -> Command>>()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class]!!() is CommandA)
        assertTrue(map[CommandB::class]!!() is CommandB)
        assertTrue(map[CommandC::class]!!() is CommandC)
    }

    @Test
    fun testMapOfLazyProvider() = codegen(
        """
        @InstanceFactory
        fun invoke(): @Provider () -> Map<KClass<out Command>, @Lazy () -> Command> {
            transient { CommandA() }
            transient { CommandB() }
            transient { CommandC() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
                put<CommandB>(CommandB::class)
                put<CommandC>(CommandC::class)
            }
            return create()
        }
         """
    ) {
        val map =
            invokeSingleFile<@Provider () -> Map<KClass<out Command>, @Lazy () -> Command>>()()
        assertEquals(3, map.size)
        assertTrue(map[CommandA::class]!!() is CommandA)
        assertTrue(map[CommandB::class]!!() is CommandB)
        assertTrue(map[CommandC::class]!!() is CommandC)
    }

    @Test
    fun testEmptyMap() = codegen(
        """
        @InstanceFactory
        fun invoke(): Map<KClass<out Command>, Command> {
            map<KClass<out Command>, Command>()
            return create()
        }
         """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(0, map.size)
    }

    @Test
    fun testUndeclaredMap() = codegen(
        """
        @InstanceFactory
        fun createComponent(): Map<String, Command> {
            return create()
        }
        """
    ) {
        assertInternalError("no binding found")
    }

    @Test
    fun testSingleEntryMap() = codegen(
        """
        @InstanceFactory
        fun invoke(): Map<KClass<out Command>, Command> {
            transient { CommandA() }
            map<KClass<out Command>, Command> {
                put<CommandA>(CommandA::class)
            }
            return create()
        }
         """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(1, map.size)
        assertTrue(map[CommandA::class] is CommandA)
    }

    @Test
    fun testMapOverrideFails() = codegen(
        """
        @InstanceFactory
        fun createComponent(): Map<String, Command> {
            transient { CommandA() }
            transient { CommandB() }
            map<String, Command> {
                put<CommandA>("a")
                put<CommandB>("a")
            }
            return create()
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
            return create()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            map<KClass<out Command>, Command> {
                put<CommandB>(CommandB::class)
            }
            return create()
        }
        
        fun invoke() = createParent().childFactory().map
    """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, Command>>()
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
            return create()
        }
        
        @ChildFactory
        fun createChild(): ChildComponent {
            transient { CommandB() }
            map<KClass<out Command>, Command> {
                put<CommandB>(CommandA::class)
            }
            return create()
        }
         """
    ) {
        assertInternalError("already bound")
    }

}
