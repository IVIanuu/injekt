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

import com.ivianuu.injekt.test.assertCompileError
import com.ivianuu.injekt.test.assertOk
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ReaderFrontendTest {

    @Test
    fun test() = codegen(
        """
            interface Context {
                fun <T> getProviderOrNull(key: Key<T>): @Reader (() -> T)?
            }
            
            inline fun <reified T> Context.getOrNull(): T? =
                getOrNull(keyOf())
            
            fun <T> Context.getOrNull(key: Key<T>): T? =
                getProviderOrNull(key)?.invoke()
            
            inline fun <reified T> Context.get(): T = get(keyOf())
            
            fun <T> Context.get(key: Key<T>): T = getProviderOrNull(key)?.invoke()
                ?: error("No given found for ''")
            
            private class ContextImpl(
                private val parent: Context?,
                private val providers: Map<Key<*>, @Reader () -> Any?>
            ) : Context {
                @Suppress("UNCHECKED_CAST")
                override fun <T> getProviderOrNull(key: Key<T>): @Reader (() -> T)? =
                    providers[key] as? @Reader (() -> T)? ?: parent?.getProviderOrNull(key)
            }
            
            class ContextBuilder(private val parent: Context? = null) {
                private val providers = mutableMapOf<Key<*>, @Reader () -> Any?>()
            
                fun <T> unscoped(key: Key<T>, provider: @Reader () -> T) {
                    providers[key] = provider
                }
            
                fun build(): Context = ContextImpl(parent, providers)
            }
            
            
            inline fun <reified T> ContextBuilder.unscoped(noinline provider: @Reader () -> T) {
                unscoped(keyOf(), provider)
            }
            
            inline fun rootContext(init: ContextBuilder.() -> Unit): Context =
                ContextBuilder().apply(init).build()
            
            inline fun Context.childContext(init: ContextBuilder.() -> Unit): Context =
                ContextBuilder(this).apply(init).build()
            
            @Reader
            val readerContext: Context
                get() = error("Intrinsic") 
                
            inline fun <reified T> ContextBuilder.scoped(noinline provider: @Reader () -> T) {
                scoped(keyOf(), provider)
            }
            
            fun <T> ContextBuilder.scoped(
                key: Key<T>,
                provider: @Reader () -> T
            ) {
                unscoped(key, ScopedProvider(provider))
            }
            
            private class ScopedProvider<T>(
                provider: @Reader () -> T
            ) : @Reader () -> T {
                private var _provider: @Reader (() -> T)? = provider
                private var _value: Any? = this
            
                @Reader
                override fun invoke(): T {
                    var value: Any? = _value
                    if (value === this) {
                        synchronized(this) {
                            value = _value
                            if (value === this) {
                                value = _provider!!()
                                _value = value
                                _provider = null
                            }
                        }
                    }
                    return value as T
                }
            }
            
            fun lol() {
                val context = rootContext {
                    unscoped { Foo() }
                    unscoped { Bar(given()) }
                }
            }
        """
    )

    @Test
    fun testReaderCallInReaderAllowed() =
        codegen(
            """
            @Reader fun a() {}
            @Reader fun b() { a() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReaderCallInNonReaderNotAllowed() =
        codegen(
            """
            @Reader fun a() {}
            fun b() { a() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testReaderCallInNonReaderLambdaIsNotAllowed() =
        codegen(
            """
            val lambda: () -> Unit = {
                func()
            }
            @Reader fun func() {}
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testNestedReaderCallInReaderAllowed() =
        codegen(
            """
            @Reader fun a() {}
            fun b(block: () -> Unit) = block()
            @Reader
            fun c() {
                b {
                    a()
                }
            }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReaderClassConstructionInReaderAllowed() =
        codegen(
            """
            @Reader class ReaderClass
            @Reader fun b() { ReaderClass() }
        """
        ) {
            assertOk()
        }

    @Test
    fun testReaderClassConstructionInNonReaderNotAllowed() =
        codegen(
            """
            @Reader class ReaderClass
            fun b() { ReaderClass() }
        """
        ) {
            assertCompileError()
        }

    @Test
    fun testReaderInterfaceFails() = codegen(
        """
        @Reader
        interface Interface
    """
    ) {
        assertCompileError("interface")
    }

    @Test
    fun testReaderObjectFails() = codegen(
        """
        @Reader
        object Object
    """
    ) {
        assertCompileError("object")
    }

    @Test
    fun testReaderPropertyOk() = codegen(
        """
        @Reader
        val property: Boolean get() = given()
    """
    )

    @Test
    fun testReaderContextOk() = codegen(
        """
        @Reader
        fun func() {
            readerContext
        }
    """
    )

    @Test
    fun testReaderPropertyWithBackingFieldFails() = codegen(
        """
            @Reader
            val property = ""
    """
    ) {
        assertCompileError("backing field")
    }

    @Test
    fun testReaderVarFails() = codegen(
        """
            @Reader
            var property = ""
    """
    ) {
        assertCompileError("var")
    }

}
