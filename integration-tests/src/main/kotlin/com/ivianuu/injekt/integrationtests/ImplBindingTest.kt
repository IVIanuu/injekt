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

}
