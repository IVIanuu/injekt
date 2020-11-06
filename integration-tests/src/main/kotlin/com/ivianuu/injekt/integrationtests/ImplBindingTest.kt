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

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ImplBindingTest {

    @Test
    fun testSimpleImplBinding() = codegen(
        """
            interface Repository
            
            @ImplBinding
            class RepositoryImpl : Repository
            
            @Component
            abstract class MyComponent {
                abstract val repository: Repository
            }
        """
    )

    @Test
    fun testObjectImplBinding() = codegen(
        """
            interface Repository
            
            @ImplBinding
            object RepositoryImpl : Repository
            
            @Component
            abstract class MyComponent {
                abstract val repository: Repository
            }
        """
    )

    @Test
    fun testImplBindingWithTypeParameters() = codegen(
        """
            interface Repository<T>
            
            @ImplBinding
            class RepositoryImpl<T> : Repository<T>
            
            @Component
            abstract class MyComponent {
                abstract val repository: Repository<String>
            }
        """
    )

    @Test
    fun testImplBindingWithTypeParametersWithMultipleUpperBounds() = codegen(
        """
            interface Repository<T, S> where T : String, T : CharSequence
            
            @ImplBinding
            class RepositoryImpl<T, S> : Repository<T, S> where T : String, T : CharSequence
            
            @Component
            abstract class MyComponent {
                abstract val repository: Repository<String, String>
            }
        """
    )

}
