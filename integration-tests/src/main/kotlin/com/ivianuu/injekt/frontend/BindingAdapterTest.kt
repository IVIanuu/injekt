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

package com.ivianuu.injekt.frontend

import com.ivianuu.injekt.assertCompileError
import com.ivianuu.injekt.codegen
import org.junit.Test

class BindingAdapterTest {

    @Test
    fun testBindingAdapterWithInvalidComponent() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter
        
        @BindingAdapterFunction(MyBindingAdapter::class)
        @Module
        fun <T> func() {
        }
    """
    ) {
        assertCompileError("@CompositionComponent")
    }

    /*@Test
    fun testCorrectBindingAdapter() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                inline fun <T> bind() {
                }
            }
        }
    """
    )

    @Test
    fun testBindingAdapterWithoutCompanion() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter
    """
    ) {
        assertCompileError("companion")
    }

    @Test
    fun testBindingAdapterWithoutModule() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object
        }
    """
    ) {
        assertCompileError("module")
    }

    @Test
    fun testBindingAdapterWithoutTypeParameters() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                fun bind() {
                }
            }
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testBindingAdapterWithMultipleTypeParameters() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                fun <A, B> bind() {
                }
            }
        }
    """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testBindingAdapterWithTransient() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                fun <T> bind() {
                }
            }
        }
        
        @MyBindingAdapter
        @Transient
        class MyClass
    """
    ) {
        assertCompileError("transient")
    }

    @Test
    fun testBindingAdapterWithScoped() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                fun <T> bind() {
                }
            }
        }
        
        @TestScope
        @MyBindingAdapter
        class MyClass
    """
    ) {
        assertCompileError("scope")
    }

    @Test
    fun testBindingAdapterNotInBounds() = codegen(
        """
        @BindingAdapter(TestComponent::class)
        annotation class MyBindingAdapter {
            companion object {
                @Module
                fun <T : UpperBound> bind() {
                }
            }
        }
        
        interface UpperBound
        
        @MyBindingAdapter
        class MyClass
    """
    ) {
        assertCompileError("bound")
    }*/

}