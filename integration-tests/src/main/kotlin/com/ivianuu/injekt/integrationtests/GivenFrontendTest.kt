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
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class GivenFrontendTest {

    @Test
    fun testGivenClassOk() = codegen(
        """ 
            @Given class Dep
        """
    )

    @Test
    fun testGivenAbstractClassFails() = codegen(
        """ 
            @Given abstract class Dep
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testGivenInterfaceFails() = codegen(
        """ 
            @Given interface Dep
    """
    ) {
        assertCompileError("abstract")
    }

    @Test
    fun testGivenClassAndGivenConstructorFails() = codegen(
        """
            @Given class Dep @Given constructor()  
        """
    ) {
        assertCompileError("either")
    }

    @Test
    fun testMultipleGivenConstructorsFails() = codegen(
        """
            class Dep {
                @Given constructor(foo: Foo)
                @Given constructor(bar: Bar)
            }
        """
    ) {
        assertCompileError("1")
    }

    @Test
    fun testMultipleConstructorsOnGivenClassFails() = codegen(
        """
            @Given
            class Dep { 
                constructor(foo: Foo)
                constructor(bar: Bar)
            }
        """
    ) {
        assertCompileError("choose")
    }
}
